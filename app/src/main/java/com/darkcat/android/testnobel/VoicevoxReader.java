package com.darkcat.android.testnobel;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class VoicevoxReader implements AutoCloseable {
    private static final String TAG = "VoicevoxCheck";
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicLong generation = new AtomicLong();

    // workerスレッドからのみ読み書きします。
    private long handle;
    // 以下はUIスレッドからのみ読み書きします。
    private boolean closed;
    private MediaPlayer player;
    private File playingFile;

    public VoicevoxReader(Context context) {
        requireMainThread();
        this.context = context.getApplicationContext();
    }

    public void speak(String text) {
        speak(text, 3); // ずんだもん・ノーマル
    }

    public void speak(String text, int styleId) {
        requireMainThread();
        if (closed) throw new IllegalStateException("Reader is closed");
        if (text == null || styleId < 0 || text.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid text or style ID");
        }
        final long id = generation.incrementAndGet();
        releasePlayer();
        if (text.trim().isEmpty()) return;

        worker.execute(() -> {
            File output = null;
            try {
                if (id != generation.get()) return;
                ensureInitialized();
                if (id != generation.get()) return;

                long started = android.os.SystemClock.elapsedRealtime();
                byte[] wav = VoicevoxBridge.synthesizeReader(handle,
                        text.getBytes(StandardCharsets.UTF_8), styleId);
                Log.i(TAG, "合成時間: "
                        + (android.os.SystemClock.elapsedRealtime() - started) + " ms");
                if (id != generation.get()) return;
                if (wav == null || wav.length == 0) {
                    throw new IOException("生成されたWAVが空です");
                }
                output = File.createTempFile("voicevox-", ".wav", context.getCacheDir());
                try (FileOutputStream out = new FileOutputStream(output)) {
                    out.write(wav);
                }
                final File ready = output;
                if (!main.post(() -> {
                    if (closed || id != generation.get()) {
                        ready.delete();
                        return;
                    }
                    play(ready, id);
                })) {
                    ready.delete();
                }
                output = null; // 以後はUI側がファイルを管理
            } catch (Exception | LinkageError e) {
                Log.e(TAG, "読み上げ失敗", e);
                final String message = e.toString();
                main.post(() -> {
                    if (!closed && id == generation.get()) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    }
                });
            } finally {
                if (output != null) output.delete();
            }
        });
    }

    // 初回のみ実行。失敗した場合は次のspeakで再試行します。
    private void ensureInitialized() throws IOException {
        if (handle != 0) return;
        VoicevoxAssets.prepare(context);
        File root = new File(context.getFilesDir(), "voicevox");
        handle = VoicevoxBridge.createReader(
                new File(root, "open_jtalk_dic_utf_8-1.11").getAbsolutePath(),
                new File(root, "models/0.vvm").getAbsolutePath());
        if (handle == 0) throw new IOException("初期化ハンドルが空です");
        Log.i(TAG, "辞書・モデルの初期化完了（このインスタンスでは一度だけ）");
    }

    private void play(File file, long id) {
        releasePlayer();
        playingFile = file;
        MediaPlayer p = new MediaPlayer();
        player = p;
        try {
            p.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            try (FileInputStream in = new FileInputStream(file)) {
                p.setDataSource(in.getFD());
            }
            p.setOnPreparedListener(mp -> {
                if (player == mp && !closed && generation.get() == id) {
                    mp.start();
                    Log.i(TAG, "再生開始");
                }
            });
            p.setOnCompletionListener(mp -> {
                if (player == mp) {
                    Log.i(TAG, "再生完了");
                    releasePlayer();
                }
            });
            p.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "再生エラー: " + what + ", " + extra);
                if (player == mp) releasePlayer();
                return true;
            });
            p.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            Log.e(TAG, "再生準備失敗", e);
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void stop() {
        requireMainThread();
        generation.incrementAndGet();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (playingFile != null) {
            playingFile.delete();
            playingFile = null;
        }
    }

    @Override
    public void close() {
        requireMainThread();
        if (closed) return;
        closed = true;
        stop();
        // 合成と同じスレッドに解放処理を並べ、使用中の解放を防ぎます。
        worker.execute(() -> {
            if (handle != 0) {
                VoicevoxBridge.destroyReader(handle);
                handle = 0;
            }
        });
        worker.shutdown();
    }

    private static void requireMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Call from the UI thread");
        }
    }
}
