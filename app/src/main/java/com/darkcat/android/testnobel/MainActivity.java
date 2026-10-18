package com.darkcat.android.testnobel;

import static android.content.ContentValues.TAG;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.darkcat.android.testnobel.VoicevoxBridge;

 public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private Util util = new Util(); // Utilクラスのインスタンス
    private SoundPool soundPool;
    private SoundPool mSoundPoolVoice;
    private int mSoundId;
    private SoundPool mSoundPoolVoice1;
    private int mSoundId1;
    private SoundPool mSoundPoolVoice2;
    private int mSoundId2;
    private SoundPool mSoundPoolVoice3;
    private int mSoundId3;
    private MediaPlayer mediaPlayer;
    private MediaPlayer mediaPlayerVoice1;
    private MediaPlayer mediaPlayerVoice2;
    private MediaPlayer mediaPlayerVoice3;
    private Button chooseButton_1;
    private Button chooseButton_2;
    private TextView textViewUp;
    private TextView textViewDown;
    private ImageView iconImage;
    private ImageView animeImage;
    // 繰り返し間隔（1000ミリ秒 = 1秒）
    private final long INTERVAL_MS = 1000L;
    private int choose_sound_1; // ボタン選択効果音＿１
    private int TEXT_SPEED_INTERVAL = 100; // 文字列のsetText()インターバル
    private Handler handler; // 別スレッドから実行するHandlerインスタンス
    private Animation anim; // アニメーション用インスタンス
    private CountDownLatch countDownLatch;
    private Boolean touchFlag = false; // タッチイベント用のフラグ
    private CyclicBarrier barrier; // サイクリックバリア
    private int text = 0;// 出力テキスト数
    private int number = 0; // 対象のsetText
    private StoryManager story; // ストーリーマネージャー関数
    private boolean firstFlag = true; // ストーリーを最初は読める
    private boolean firstSpeakFlg = true; // 最初の発声フラグ
    private boolean firstTouchFlg = true; // 初回タッチフラグ
    private MagicManager magic = new MagicManager();// マジックマネージャのインスタンス
    private AudioManager audio;
    private int ringVolume = 0;
    private int ringMaxVolume = 0;
    private TextToSpeech tts; // Text-to-speechのinスタンス
    File filesDir;
    private File darkFile; // dark-nekoの出力先外部ファイル
    private File VoiceBoxFile; // VoiceBoxの出力先外部ファイル
    private String LOCAL_IP = "";
    private VoiceBoxClient voicevox; // VoiceBoxインスタンス
    private String dictPath; // 辞書ファイルのパス
    private MediaProjectionManager projectionManager; // MediaProjectionManager
    private VoicevoxReader voicevoxReader;

     private static final int REQUEST_CODE_CAPTURE_PERM = 999; // 値は0以上の任意の整数
//     // VoiceBoxCore読み込み（後で対応する！！）
//     static {
//         System.loadLibrary("c++_shared");
//         System.loadLibrary("voicevox_onnxruntime");
//         System.loadLibrary("voicevox_core");
//         System.loadLibrary("native-lib");
//     }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /// /
        voicevoxReader = new VoicevoxReader(this);
        voicevoxReader.speak("こんにちは、ずんだもんなのだ。");

        // スキップ・停止ボタンから呼ぶ場合
        voicevoxReader.stop();
        /// /



        // 初期設定
        setInit();

        // ↓↓↓ストーリー仮置き１章（消さずに保存しておくこと！！）ーーーーーーー

        changeView(R.drawable.black);

        playFromMediaPlayer(R.raw.identity);

//        startBattle();
//        startWalk();
        // 仮トークン取得
//        getFCMToken();

        // トークン送信
        // （テスト用）
        onNewToken("eS4SY2PzRLiWT6ryjzyx5o:APA91bHShIcBVqQqVDErJCpdbj-qbMRXa9vxPA67GyW62hzv5wvSNYxkHh3P2WLwt7N4_tSFzfoqtjsk43n0Ku4JcpydGQwYTm413rA5NMvyLtenUKl_Cho");

        // MediaProjectionManagerを取得
        projectionManager =
                (MediaProjectionManager) getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE
                );

        // ストーリー読みだし
//        popStoryText();
        startRepeatingTask();

        // ↑↑↑ストーリー仮置き１章（消さずに保存しておくこと！！）ーーーーーーー
    }

    // VoiceBoxにしゃべらせるメソッド
    // talkContent : 再生するメッセージ
    // speaker : 再生するVoiceBoxのキャラクター    // 例）3 : ずんだもん　8 : 春日部つむぎ
    // ※仮置き、後で修正する！！！！
    public void speakVoiceBox(String talkContent, String talkContent2) {
        speakVoice(talkContent, talkContent2);
    }

     // しゃべっていないときに自動的に進む
     private final Runnable runnable = new Runnable() {
         @Override
         public void run() {
             // ここに定期実行したい関数を書く
             execPopStoryText();

             // 次回も自身を呼び出してループさせる
             handler.postDelayed(this, INTERVAL_MS);
         }
     };

     // 定期実行を開始する処理
     private void startRepeatingTask() {
         if (!firstTouchFlg) {
             runnable.run();
         }
     }

     public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
        // サーバーのエンドポイント
        String serverUrl = "https://console.firebase.google.com/u/0/project/test-reo/overview";

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        StringRequest postRequest = new StringRequest(Request.Method.POST, serverUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("FCM", "Token送信成功: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("FCM", "Token送信失敗: " + error.getMessage());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("token", token);
                return params;
            }
        };

        requestQueue.add(postRequest);
    }

    // 音楽ファイルの再生
    // rValueMusic : 再生する音楽のR値
    public void playFromMediaPlayer(int rValueMusic) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopFromMediaPlayer();
        }
        mediaPlayer = null;

        mediaPlayer = MediaPlayer.create(this, rValueMusic);

        // 音量半減化
        mediaPlayer.setVolume(0.1f, 0.1f);

        // ループ設定
        mediaPlayer.setLooping(true);

        // メディアプレイヤー再生
        mediaPlayer.start();
    }

    // ボイスボックスファイルの再生
    // rValueMusic : 再生する音楽のR値
    public void playFromVoiceBox(int rValueMusic) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopFromMediaPlayer();
        }
        mediaPlayer = null;

        mediaPlayer = MediaPlayer.create(this, rValueMusic);

        // VoiceBoxの出力先外部ファイル
        try {
            mediaPlayer.setDataSource(VoiceBoxFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 音量半減化
        mediaPlayer.setVolume(0.5f, 0.5f);

        // ループ設定
        mediaPlayer.setLooping(true);

        // メディアプレイヤー再生
        mediaPlayer.start();
    }


    // FCMトークンの取得
    //
    public void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Voice1ファイルの再生
    // rValueMusic : 再生する音楽のR値
    public void playFromMediaPlayerVoice1(int rValueMusic) {
        mediaPlayerVoice1 = MediaPlayer.create(this, rValueMusic);

        // 音量最大化
        mediaPlayerVoice1.setVolume(1.0f, 1.0f);

        // ループ設定
        mediaPlayerVoice1.setLooping(false);

        // メディアプレイヤー再生
        mediaPlayerVoice1.start();

        // 音楽をリリースする
        mediaPlayerVoice1.setOnCompletionListener(mediaPlayer -> mediaPlayer.release());
    }

    // Voice1ファイルの再生
    // rValueMusic : 再生する音楽のR値
    public void playFromMediaPlayerVoice2(int rValueMusic) {
        mediaPlayerVoice2 = MediaPlayer.create(this, rValueMusic);

        // 音量最大化
        mediaPlayerVoice2.setVolume(1.0f, 1.0f);

        // ループ設定
        mediaPlayerVoice2.setLooping(false);

        // メディアプレイヤー再生
        mediaPlayerVoice2.start();

        // 音楽をリリースする
        mediaPlayerVoice2.setOnCompletionListener(mediaPlayer -> mediaPlayer.release());
    }

    // Voice1ファイルの再生
    // rValueMusic : 再生する音楽のR値
    public void playFromMediaPlayerVoice3(int rValueMusic) {
        mediaPlayerVoice3 = MediaPlayer.create(this, rValueMusic);

        // 音量最大化
        mediaPlayerVoice3.setVolume(1.0f, 1.0f);

        // ループ設定
        mediaPlayerVoice3.setLooping(false);

        // メディアプレイヤー再生
        mediaPlayerVoice3.start();

        // 音楽をリリースする
        mediaPlayerVoice3.setOnCompletionListener(mediaPlayer -> mediaPlayer.release());
    }

    // 音楽ファイルの停止
    public void stopFromMediaPlayer() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    //
    private void setClickListeners() {
        // ボタンに効果音を割り当てる
        chooseButton_1.setOnClickListener(v -> {
            // ボタン選択効果音を鳴らすように設定
            playSound(choose_sound_1);

            // 選択肢を選ぶまで凍らせるフラグをfalseにして解除（共通処理）
            story.setStoryFreezeFlg(false);

            // 選択肢を消す（共通処理）
            viewDisappearButton();

            // ストーリーをうまく遷移させる
            storyChooser(chooseButton_1);
        });

        // ボタンに効果音を割り当てる
        chooseButton_2.setOnClickListener(v -> {
            // ボタン選択効果音を鳴らすように設定
            playSound(choose_sound_1);

            // 選択肢を選ぶまで凍らせるフラグをfalseにして解除（共通処理）
            story.setStoryFreezeFlg(false);

            // 選択肢を消す（共通処理）
            viewDisappearButton();

            // ストーリーをうまく遷移させる
            storyChooser(chooseButton_2);
        });

        // 仮タッチ時にイベントが進むようにする
        // ポップしたものが文字列の場合ー＞テキストストーリー
        // ポップしたものがR値の場合ー＞エフェクト？ストーリー
        animeImage.setOnClickListener(v -> {
            execPopStoryText();
        });

        // 仮タッチ時にイベントが進むようにする
        textViewUp.setOnClickListener(v -> {
            execPopStoryText();
        });

        // 仮タッチ時にイベントが進むようにする
        textViewDown.setOnClickListener(v -> {
            execPopStoryText();
        });
    }

     // ネイティブメソッド
     public native byte[] synthesize(String text, String dictPath);

    // 初期設定
    private void setInit() {
        // 辞書ファイルのパスを取得
        dictPath = getFilesDir().getAbsolutePath();

        // ストーリーマネージャーインスタンス生成
        story = new StoryManager();

        // レイアウトファイル読み込み
        setContentView(R.layout.activity_main);

        // 背景画像差し替え
        changeView(R.drawable.karafuto_shishamo);

        // Handlerインスタンス生成
        handler = new Handler(Looper.getMainLooper());

        // サイクリックバリアインスタンスを生成
        barrier = new CyclicBarrier(1, null);

        // (！警告消す)
        // オーディオアトリビュートインスタンス生成
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        // （！警告消す）
        // サウンドプールインスタンス生成
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                // ストリーム数に応じて
                .setMaxStreams(2)
                .build();

        // サウンド１をロード
        choose_sound_1 = soundPool.load(this, R.raw.__koukaonn__tin, 1);

        // ボタン初期設定
        chooseButton_1 = (Button) findViewById(R.id.button1);
        chooseButton_2 = (Button) findViewById(R.id.button2);

        // アニメ画面設定
        animeImage = (ImageView) findViewById(R.id.imageView);
        textViewUp = (TextView) findViewById(R.id.textView_upper);
        textViewDown = (TextView) findViewById(R.id.textView_downer);

        // メッセージバーを可視化する
        VisibleMessageBar();

        // 画面クリック時イベント設定
        setClickListeners();

        // Text-to-speechインスタンス作成
        tts = new TextToSpeech(this, this);

        filesDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        // 外部ストレージがない場合
        if (filesDir == null) {
            filesDir = getDataDir();
        }
        // ディレクトリがある場合は作らない
        makeDirectory();
        darkFile = new File(filesDir, "dark-neko.wav");
        VoiceBoxFile = new File(filesDir, "voicebox.wav");
        try {
            darkFile.createNewFile();
            VoiceBoxFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer = new MediaPlayer();

//        // LOCALIP
//        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int ipAddress = wifiInfo.getIpAddress();
//        LOCAL_IP = Formatter.formatIpAddress(ipAddress);
//        voicevox = new VoiceBoxClient("http://" + LOCAL_IP + ":50021");
//        // アイコン読み込み
//        iconImage = (ImageView) findViewById(R.id.view_icon);

        // 仮置き
        SoundManager soundManager = new SoundManager();
        soundManager.initSound();
    }

    // ディレクトリを作る関数
    // ディレクトリがある場合は作らない
    public void makeDirectory() {
        if (filesDir != null) {
            if (!filesDir.exists()) {
                boolean created = filesDir.mkdirs();
                if (created) {
                    Log.d("Debug", "外部ファイルディレクトリを作成しました: " + filesDir.getAbsolutePath());
                } else {
                    Log.e("Debug", "外部ファイルディレクトリの作成に失敗しました");
                }
            } else {
                Log.d("Debug", "外部ファイルディレクトリは既に存在します: " + filesDir.getAbsolutePath());
            }
        } else {
            Log.e("Debug", "外部ストレージが利用できません");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.JAPANESE);

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "この言語はサポートされていません。");
            } else {
                // OK!!!!
            }
        } else {
            Log.e("TTS", "初期化に失敗しました。");
        }
    }

     @Override
     protected void onStop() {
         if (voicevoxReader != null) voicevoxReader.stop();
         super.onStop();
         // ホーム画面に戻った際などにサービスを停止
         stopService(new Intent(this, StoryMakerService.class));
     }

    @Override
    protected void onDestroy() {
        if (voicevoxReader != null) {
            voicevoxReader.close();
            voicevoxReader = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

        // voicevox解放！
        releaseVoicevoxTestPlayer();

        // サービスのクラス名を指定して停止
        Intent intent = new Intent(this, StoryMakerService.class);
        stopService(intent);
    }

    // TTSにしゃべらせるメソッド
    private void speakVoice(String text, String text2) {

        String txt2 = "";
        if (text2.startsWith("qqq")) {
            txt2 = text2.substring(text2.indexOf("qqq") + 3);
        }

        String txt = "";
        if (text2.isEmpty()) {
            txt = text;
        } else if (!text2.isEmpty()) {
            txt = text + txt2;
        }

        if (firstSpeakFlg) {
            tts.speak(txt, TextToSpeech.QUEUE_FLUSH, null, null);
            this.firstSpeakFlg = false;
        } else {
            // 3秒（3000ミリ秒）後に処理を実行
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    String txt = "";
                    String txt2 = "";
                    if (text2.startsWith("qqq")) {
                        txt2 = text2.substring(text2.indexOf("qqq") + 3);
                    }

                    if (text2.isEmpty()) {
                        txt = text;
                    } else if (!text2.isEmpty()) {
                        txt = text + txt2;
                    }
                    tts.speak(txt, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }, 3000);
        }
        sleeping();
    }

    // 戦闘処理
    private void startBattle() {
        Intent intent = new Intent(this, BattleActivity.class);
        startActivity(intent);
        // 音楽も多重起動しないようにする
        stopFromMediaPlayer();

        // このメインアクティビティを終了（多重起動しないようにする）
        finish();
    }

    // 戦闘処理
    private void startWalk() {
        Intent intent = new Intent(this, WalkActivity.class);
        startActivity(intent);
        // 音楽も多重起動しないようにする
        stopFromMediaPlayer();

        // このメインアクティビティを終了（多重起動しないようにする）
        finish();
    }

    // 間を持たせるためにシーンの合間合間に１秒待機する関数
    // （置き場所に注意！）（未設定）
    private void waitSecond() {
        try {
            story.setStoryFreezeFlg(true);
            removeClickListeners();
            Thread.sleep(1000);
            setClickListeners();
            story.setStoryFreezeFlg(false);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // クリックイベント一時削除処理
    private void removeClickListeners() {
        animeImage.setOnClickListener(null);
        textViewUp.setOnClickListener(null);
        textViewDown.setOnClickListener(null);
    }

    // 2秒間待機させる関数
    private void sleeping() {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     // 最後に待機させる番兵
     private void ending() {
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
     }


     // popStory関数を実行する中身の関数
    // popStory関数を直すときはここを直す
    private void execPopStoryText() {
        // 初めの一回だけ手でタッチして進める
        if (firstTouchFlg) {
            removeClickListeners();
            handler.postDelayed(runnable, INTERVAL_MS);
            firstTouchFlg = false;
        }

        // 選択肢を選ぶまで凍らせるフラグがfalseの時にしか実行できない
        if ((firstFlag && !story.getStoryFreezeFlg()) && !tts.isSpeaking()) {
            // 次回のために声をstopする
            stopVoices();
            if (((String) story.getStory()).contains("showText")) {
                String tmpStrValue = ((String) story.getStory()).replace("showText", "");
                String voiceColor = "";
                if (tmpStrValue.contains("zzz")) {
                    voiceColor = tmpStrValue.substring(tmpStrValue.indexOf("zzz") + "zzz".length(), tmpStrValue.indexOf("zzz") + "zzz".length() + 1);
                    tmpStrValue = tmpStrValue.replace("zzz" + voiceColor, "");
                }
                String colorValue = "";
                String tmpRValue = "";
                // 追加ボイスある場合の処理
                if (tmpStrValue.contains("vvv")) {
                    colorValue = tmpStrValue.substring(tmpStrValue.indexOf("setTextColor") + "setTextColor".length());
                    tmpRValue = tmpStrValue.replace(tmpStrValue.substring(tmpStrValue.indexOf("setTextColor")), "");
                    int voiceNum = Integer.parseInt(tmpRValue.substring(tmpRValue.indexOf("vvv") + "vvv".length()));
                    // setMusicVoice1(voiceNum); // オリジナルボイス
                    if (tmpStrValue.contains("rrr")) {
                        speakVoiceBox(tmpStrValue.substring(0, tmpStrValue.indexOf("qqq")), tmpStrValue.substring(tmpStrValue.indexOf("qqq"), tmpStrValue.indexOf("rrr"))); // VOICEBOX
                        tmpRValue = tmpRValue.replace("rrr", "");
                    } else {
                        speakVoiceBox(tmpStrValue.substring(0, tmpStrValue.indexOf("qqq")), ""); // VOICEBOX
                    }
                    tmpRValue = tmpRValue.replace(("vvv" + voiceNum), "");
                }

                // 色セットする場合の処理
                if (tmpStrValue.contains("setTextColor")) {
                    setTextColor(colorValue);
                } else if (tmpStrValue.contains("setTextColorUp")) {
                    setTextColorUp(colorValue);
                } else if (tmpStrValue.contains("setTextColorDown")) {
                    setTextColorDown(colorValue);
                }
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
                // 過去のメッセージを入れておく
                story.setPastMessage(tmpRValue);
                showText(tmpRValue);
                // 自分で自分を呼ぶことで自動的に次に進む
//                execPopStoryText();
                // ストーリーを最後まで見ないと読めないようにする
            }
        } else if (story.getOldStory().toString().equals(textViewUp.getText().toString() + textViewDown.getText().toString())) {
            // showTextを連続で実行しているときの連打制御用if文
            if (((String) story.getStory()).contains("changeView")) {
                // 追加でメッセージバーを表示・非表示する場合
                String tmpStrValue = ((String) story.getStory());
                String strValue = "";
                if (tmpStrValue.contains("hide")) {
                    hideMessageBar();
                    // 後処理
                    tmpStrValue = tmpStrValue.replace("hide", "");
                } else if (tmpStrValue.contains("view")) {
                    VisibleMessageBar();
                    // 後処理
                    tmpStrValue = tmpStrValue.replace("view", "");
                }

                // 末尾のコマンド文字列をトリム
                tmpStrValue = tmpStrValue.replace("changeView", "");
                int tmpRValue = Integer.parseInt(tmpStrValue);

                changeView(tmpRValue);
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
                // 自分で自分を呼ぶことで自動的に次に進む
                execPopStoryText();
                // ViewChooseButtonがストーリーの末尾に存在するとき
            } else if (((String) story.getStory()).contains("viewChooseButton")) {
                // 末尾のコマンド文字列をトリム
                String tmpStrValue = ((String) story.getStory()).replace("setChooseText", "");

                // それぞれの引数を「qqq」またぎで分割して関数に代入
                String firstStr1 = tmpStrValue.substring(0, tmpStrValue.indexOf("qqq"));
                String firstStr2 = tmpStrValue.substring(tmpStrValue.indexOf("qqq") + 3);
                firstStr2 = firstStr2.replace(firstStr2.substring(firstStr2.indexOf("viewChooseButton")), "");

                // setChooseText()呼び出し
                setChooseText(firstStr1, firstStr2);
                // 引数によって分岐
                if (((String) story.getStory()).contains("viewChooseButton(1)")) {
                    viewChooseButton(1);
                } else if (((String) story.getStory()).contains("viewChooseButton(2)")) {
                    viewChooseButton(2);
                }
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
            } else if (((String) story.getStory()).contains("ClearText")) {
                // 末尾のコマンド文字列をトリム
                String tmpStrValue = ((String) story.getStory()).replace("ClearText", "");

                // clearText()呼び出し
                ClearText();
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
            } else if ((story.getStory().toString().contains("showText")) &&
                    story.getOldStory().toString().equals(textViewUp.getText().toString() + textViewDown.getText().toString())) {
                String tmpStrValue = ((String) story.getStory()).replace("showText", "");
                String colorValue = "";
                // テキスト色変更ある場合の処理
                if (tmpStrValue.contains("setTextColor")) {
                    colorValue = tmpStrValue.substring(tmpStrValue.indexOf("setTextColor") + "setTextColor".length());
                    // 色セットする場合の処理
                    if (tmpStrValue.contains("setTextColor")) {
                        setTextColor(colorValue);
                    } else if (tmpStrValue.contains("setTextColorUp")) {
                        setTextColorUp(colorValue);
                    } else if (tmpStrValue.contains("setTextColorDown")) {
                        setTextColorDown(colorValue);
                    }
                    tmpStrValue = tmpStrValue.replace(tmpStrValue.substring(tmpStrValue.indexOf("setTextColor")), "");
                }
                String voiceColor = "";
                // 追加ボイスある場合の処理
                if (tmpStrValue.contains("zzz")) {
                    voiceColor = tmpStrValue.substring(tmpStrValue.indexOf("zzz") + "zzz".length(), tmpStrValue.indexOf("zzz") + "zzz".length() + 1);
                    tmpStrValue = tmpStrValue.replace("zzz" + voiceColor, "");
                }
                if (tmpStrValue.contains("vvv")) {
                    int voiceNum = Integer.parseInt(tmpStrValue.substring(tmpStrValue.indexOf("vvv") + "vvv".length()));
                    // setMusicVoice1(voiceNum); // オリジナルボイス
                    if (tmpStrValue.contains("rrr")) {
                        speakVoiceBox(tmpStrValue.substring(0, tmpStrValue.indexOf("qqq")), tmpStrValue.substring(tmpStrValue.indexOf("qqq"), tmpStrValue.indexOf("rrr"))); // VOICEBOX
                        tmpStrValue = tmpStrValue.replace("rrr", "");
                    } else {
                        speakVoiceBox(tmpStrValue.substring(0, tmpStrValue.indexOf("qqq")), ""); // VOICEBOX
                    }
                    tmpStrValue = tmpStrValue.replace(("vvv" + voiceNum), "");
                }
//////
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
                // 過去のメッセージを入れておく
                story.setPastMessage(tmpStrValue);
                showText(tmpStrValue);
                // 自分で自分を呼ぶことで自動的に次に進む
//                execPopStoryText();
                // ストーリーを最後まで見ないと読めないようにする
            } else if (((String) story.getStory()).contains("playFromMediaPlayer")) {
                // 末尾のコマンド文字列をトリム
                String tmpStrValue = ((String) story.getStory()).replace("playFromMediaPlayer", "");
                // R値に変換
                int playTarget_R = Integer.parseInt(tmpStrValue);

                // playFromMediaPlayer()呼び出し
                playFromMediaPlayer(playTarget_R);
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
                // 自分で自分を呼ぶことで自動的に次に進む
                execPopStoryText();
                // テキストをクリアする
            } else if (((String) story.getStory()).contains("ClearText")) {
                // 末尾のコマンド文字列をトリム
                String tmpStrValue = ((String) story.getStory()).replace("ClearText", "");

                // clearText()呼び出し
                ClearText();
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
            } else if (((String) story.getStory()).contains("sleeping")) {
                // 末尾のコマンド文字列をトリム
                String tmpStrValue = ((String) story.getStory()).replace("sleeping", "");

                // sleeping()呼び出し
                sleeping();
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
                // 自分で自分を呼ぶことで自動的に次に進む
//                execPopStoryText();
                // １命令待機させる「STOP」（飛ばさないようにしたいときに使う）
            } else if (((String) story.getStory()).contains("STOP")) {
                firstFlag = false;
                story.setNumber(story.getNumber() + 1);
            } else if (((String) story.getStory()).contains("END")) {
                // 終了の番兵を配置
                firstFlag = false;
                ending();
                story.setNumber(story.getNumber() + 1);
            }
        }
    }
//        Log.e("tag", story.getStory().toString().replace("qqq", "").replace("changeView", "")
//                .replace("showText", ""));
//        Log.e("tag", textViewUp.getText().toString() + textViewDown.getText().toString());


    // 画面タッチしたらストーリーをポップする
    private void popStoryText() {
        // 現在のnumberをストーリーマネージャーに聞きに行く
        number = story.getNumber();

        // 繰り返しストーリーを進める
        if (!tts.isSpeaking()) {
            try {
                Thread.sleep(1000);
                startRepeatingTask();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        // 仮タッチ時にイベントが進むようにする
//        // ポップしたものが文字列の場合ー＞テキストストーリー
//        // ポップしたものがR値の場合ー＞エフェクト？ストーリー
//        animeImage.setOnClickListener(v -> {
//            execPopStoryText();
//        });
//
//        // 仮タッチ時にイベントが進むようにする
//        textViewUp.setOnClickListener(v -> {
//            execPopStoryText();
//        });
//
//        // 仮タッチ時にイベントが進むようにする
//        textViewDown.setOnClickListener(v -> {
//            execPopStoryText();
//        });
    }

    // StoryManager用にFirstFlagの取得関数
    public boolean getFirstFlag() {
        return this.firstFlag;
    }

    // StoryManager用にFirstFlagの設定関数
    public void setFirstFlag(boolean flag) {
        this.firstFlag = flag;
    }

    // Resume時の動作
    public void onResume() {
        // 音楽再開
        super.onResume();
        if (mediaPlayer == null) {
            playFromMediaPlayer(R.raw.yespartytime);
        }
    }

    // voice再生
    public void playVoice(SoundPool soundPool, int mSoundId) {
        soundPool.play(mSoundId, 1.0F, 1.0F, 0, 0, 1.0F);
    }

    // 背景画像の差し替え
    // rValueView : 差し替える画像のR値
    private void changeView(int rValueView) {
        ImageView myImage = findViewById(R.id.imageView);
        Drawable myDrawable = getResources().getDrawable(rValueView, getTheme());
        myImage.setImageDrawable(myDrawable);
    }

    // アイコン画像の差し替え
    // rValueView : 差し替えるアイコン画像のR値
    private void changeIconView(int rValueView) {
//        ImageView myImage = findViewById(R.id.view_icon);
//        Drawable myDrawable = getResources().getDrawable(rValueView);
//        myImage.setImageDrawable(myDrawable);
    }

    // 画像全体を見せるためにメッセージバーを隠す
    private void hideMessageBar() {
        TextView textViewUpper = findViewById(R.id.textView_upper);
        TextView textViewDowner = findViewById(R.id.textView_downer);
        textViewUpper.setVisibility(View.INVISIBLE);
        textViewDowner.setVisibility(View.INVISIBLE);
    }

    // 画像全体を見せるためにメッセージバーを再表示する
    private void VisibleMessageBar() {
        TextView textViewUpper = findViewById(R.id.textView_upper);
        TextView textViewDowner = findViewById(R.id.textView_downer);
        textViewUpper.setVisibility(View.VISIBLE);
        textViewDowner.setVisibility(View.VISIBLE);
    }

    // 選択ボタンを表示させる
    // count：1~2
    private void viewChooseButton(int count) {
        if (count == 1) {
            chooseButton_1.setVisibility(View.VISIBLE);
            // 選択肢を選ぶまで凍らせる
            story.setStoryFreezeFlg(true);
        } else if (count == 2) {
            chooseButton_1.setVisibility(View.VISIBLE);
            chooseButton_2.setVisibility(View.VISIBLE);
            // 選択肢を選ぶまで凍らせる
            story.setStoryFreezeFlg(true);
        } else {
            // 値を間違えている場合は処理をしない
        }
    }

    // 選択ボタンを全て消滅させる
    private void viewDisappearButton() {
        chooseButton_1.setVisibility(View.INVISIBLE);
        chooseButton_2.setVisibility(View.INVISIBLE);
    }

    // 選択テキストをセットする
    // str1：上の選択テキスト
    // str2：下の選択テキスト
    private void setChooseText(String str1, String str2) {
        // セットテキスト
        chooseButton_1.setText(str1);
        chooseButton_2.setText(str2);
    }

    // テキストをクリアする
    private void ClearText() {
        // テキストビュー取得
        textViewUp = findViewById(R.id.textView_upper);
        textViewDown = findViewById(R.id.textView_downer);

        // クリア
        textViewUp.setText("");
        textViewDown.setText("");
    }

    // ゆっくりとテキストを表示させる
    // targetString_1：表示させる対象文言_1行目
    // targetString_2：表示させる対象文言_2行目
    // number: 対象のshowText（仮）
    public void showText(String targetString) {

        // テキストをクリアする
        ClearText();

        // Runnable初期化
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

            }
        };

        // テキストビュー取得
        textViewUp = findViewById(R.id.textView_upper);
        textViewDown = findViewById(R.id.textView_downer);

        // 別スレッドでの処理を登録
        ExecutorService service = Executors.newFixedThreadPool(1);

        // targetString1_2生成
        String targetString_1 = "";
        String targetString_2 = "";

        targetString_1 = targetString.substring(0, targetString.indexOf("qqq"));
        targetString_2 = targetString.substring(targetString.indexOf("qqq") + 3);

        // タスク登録
        runnable = taskWaiting(targetString_1, targetString_2, barrier, runnable);

        // タスク登録
        service.submit(runnable);

    }

    // add・removeEventListener用の関数
    private void clickEventListener() {
        // ボタン選択効果音を鳴らすように設定
        playSound(choose_sound_1);
        touchFlag = true;
    }

    // ゴウトのアイコン用アニメーション
    private Animation goutIcon() {
        // Scale 拡大縮小
        // from幅倍率,to幅倍率,from高さ倍率,to高さ倍率,中心軸X,中心軸Y
        Animation scale = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f);

        // true：補間をsetに適用
        AnimationSet set = new AnimationSet(true);
        scale.setDuration(500); // 1000msかけて動く
        set.addAnimation(scale);

        // true：終了時に元の状態に戻さない
        set.setFillAfter(true);

        // 0でリピートなし
        set.setRepeatCount(0);

        return set;
    }

    // 効果音再生関数
    // targetSound：鳴らす対象のロード済み効果音
    private void playSound(int targetSound) {
        // 効果音再生
        // AudioManagerを取得する
        audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        // 現在の音量を取得する
        ringVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);

        // ストリームごとの最大音量を取得する
        ringMaxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);

        soundPool.play(targetSound, ringVolume, ringVolume, 1, 0, 1);
    }

    // （showText内で使用）
    private Runnable taskWaiting(String targetString_1, String targetString_2, CyclicBarrier barrier, Runnable runnable) {

        // 結合テキスト
        String mergeText = targetString_1 + targetString_2;

        // テキスト出力用の変数
        String outputText = "";

        // 2行目用のカウント変数
        int kk = 0;

        // 全ての文字を表示させるまで繰り返す
        for (int k = 0; k <= mergeText.length(); k++) {

            // 別スレッドからPOSTを実行
            // 行番号に応じてテキストを表示させる
            outputText = mergeText.substring(0, k);

            // 2行目の場合はtargetString_2の引数で出力用変数を上書き
            if (k > targetString_1.length()) {
                kk++;
                outputText = targetString_2.substring(0, kk);
            }

            if (k <= targetString_1.length()) {
                String finalOutputText = outputText;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        textViewUp.setText(finalOutputText);
                        // 再描画して文字を即表示させる
                        textViewUp.invalidate();
                        while (textViewUp.getText().toString().length() == targetString_1.length()) {
                            CyclicbarrierAwait(barrier);
                            break;
                        }
                    }
                }, k * TEXT_SPEED_INTERVAL);
            } else if (k > targetString_1.length()) {
                String finalOutputText1 = outputText;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        textViewDown.setText(finalOutputText1);
                        // 再描画して文字を即表示させる
                        textViewDown.invalidate();
                        while (true) {
                            if (textViewDown.getText().toString().length() == targetString_2.length())
                            CyclicbarrierAwait(barrier);
                            break;
                        }
                    }
                }, k * TEXT_SPEED_INTERVAL);
            }
        }
        // 仮待機
        CyclicbarrierAwait(barrier);

        return runnable;
    }

    // CyclicBarrierのawait処理
    private void CyclicbarrierAwait(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Voice1の分岐（１：野獣先輩）
    private void setMusicVoice1(int value) {
        if (value == 0) {
            playFromMediaPlayerVoice1(R.raw.h0);
        } else if (value == 1) {
            playFromMediaPlayerVoice1(R.raw.h1);
        } else if (value == 2) {
            playFromMediaPlayerVoice1(R.raw.h2);
        } else if (value == 3) {
            playFromMediaPlayerVoice1(R.raw.h3);
        } else if (value == 4) {
            playFromMediaPlayerVoice1(R.raw.h4);
        } else if (value == 5) {
            playFromMediaPlayerVoice1(R.raw.h5);
        } else if (value == 6) {
            playFromMediaPlayerVoice1(R.raw.h6_1);
        } else if (value == 7) {
           playFromMediaPlayerVoice1(R.raw.h7_1);
        }
    }

//    // Voice2の分岐（２；ノック）
//    private void setMusicVoice2(int value) {
//        if (value == 10) {
//            playFromMediaPlayerVoice1(R.raw.h10);
//        } else if (value == 11) {
//            playFromMediaPlayerVoice1(R.raw.h11);
//        } else if (value == 12) {
//            playFromMediaPlayerVoice1(R.raw.h12);
//        } else if (value == 13) {
//            playFromMediaPlayerVoice1(R.raw.h13);
//        } else if (value == 14) {
//            playFromMediaPlayerVoice1(R.raw.h14);
//        } else if (value == 15) {
//            playFromMediaPlayerVoice1(R.raw.h15);
//        } else if (value == 16) {
//            playFromMediaPlayerVoice1(R.raw.h16);
//        } else if (value == 17) {
//            playFromMediaPlayerVoice1(R.raw.h17);
//        } else if (value == 18) {
//            playFromMediaPlayerVoice1(R.raw.h18);
//        } else if (value == 19) {
//            playFromMediaPlayerVoice1(R.raw.h19);
//        } else if (value == 20) {
//            playFromMediaPlayerVoice1(R.raw.h20);
//        } else if (value == 21) {
//            playFromMediaPlayerVoice1(R.raw.h21);
//        } else if (value == 22) {
//            playFromMediaPlayerVoice1(R.raw.h22);
//        } else if (value == 23) {
//            playFromMediaPlayerVoice1(R.raw.h23);
//        } else if (value == 24) {
//            playFromMediaPlayerVoice1(R.raw.h24);
//        } else if (value == 25) {
//            playFromMediaPlayerVoice1(R.raw.h25);
//        } else if (value == 26) {
//            playFromMediaPlayerVoice1(R.raw.h26);
//        } else if (value == 27) {
//            playFromMediaPlayerVoice1(R.raw.h27);
//        } else if (value == 28) {
//            playFromMediaPlayerVoice1(R.raw.h28);
//        } else if (value == 29) {
//            playFromMediaPlayerVoice1(R.raw.h29);
//        } else if (value == 30) {
//            playFromMediaPlayerVoice1(R.raw.h30);
//        } else if (value == 31) {
//            playFromMediaPlayerVoice1(R.raw.h31);
//        } else if (value == 32) {
//            playFromMediaPlayerVoice1(R.raw.h32);
//        } else if (value == 33) {
//            playFromMediaPlayerVoice1(R.raw.h33);
//        } else if (value == 34) {
//            playFromMediaPlayerVoice1(R.raw.h34);
//        } else if (value == 35) {
//            playFromMediaPlayerVoice1(R.raw.h35);
//        } else if (value == 36) {
//            playFromMediaPlayerVoice1(R.raw.h36);
//        } else if (value == 37) {
//            playFromMediaPlayerVoice1(R.raw.h37);
//        } else if (value == 38) {
//            playFromMediaPlayerVoice1(R.raw.h38);
//        } else if (value == 39) {
//            playFromMediaPlayerVoice1(R.raw.h39);
//        } else if (value == 40) {
//            playFromMediaPlayerVoice1(R.raw.h40);
//        } else if (value == 41) {
//            playFromMediaPlayerVoice1(R.raw.h41);
//        } else if (value == 42) {
//            playFromMediaPlayerVoice1(R.raw.h42);
//        } else if (value == 43) {
//            playFromMediaPlayerVoice1(R.raw.h43);
//        } else if (value == 44) {
//            playFromMediaPlayerVoice1(R.raw.h44);
//        } else if (value == 45) {
//            playFromMediaPlayerVoice1(R.raw.h45);
//        } else if (value == 46) {
//            playFromMediaPlayerVoice1(R.raw.h46);
//        } else if (value == 47) {
//            playFromMediaPlayerVoice1(R.raw.h47);
//        } else if (value == 48) {
//            playFromMediaPlayerVoice1(R.raw.h48);
//        } else if (value == 49) {
//            playFromMediaPlayerVoice1(R.raw.h49);
//        } else if (value == 50) {
//            playFromMediaPlayerVoice1(R.raw.h50);
//        } else if (value == 51) {
//            playFromMediaPlayerVoice1(R.raw.h51);
//        } else if (value == 52) {
//            playFromMediaPlayerVoice1(R.raw.h52);
//        } else if (value == 53) {
//            playFromMediaPlayerVoice1(R.raw.h53);
//        } else if (value == 54) {
//            playFromMediaPlayerVoice1(R.raw.h54);
//        } else if (value == 55) {
//            playFromMediaPlayerVoice1(R.raw.h55);
//        } else if (value == 56) {
//            playFromMediaPlayerVoice1(R.raw.h56);
//        } else if (value == 57) {
//            playFromMediaPlayerVoice1(R.raw.h57);
//        } else if (value == 58) {
//            playFromMediaPlayerVoice1(R.raw.h58);
//        } else if (value == 59) {
//            playFromMediaPlayerVoice1(R.raw.h59);
//        } else if (value == 60) {
//            playFromMediaPlayerVoice1(R.raw.h60);
//        } else if (value == 61) {
//            playFromMediaPlayerVoice1(R.raw.h61);
//        } else if (value == 62) {
//            playFromMediaPlayerVoice1(R.raw.h62);
//        } else if (value == 63) {
//            playFromMediaPlayerVoice1(R.raw.h63);
//        } else if (value == 64) {
//            playFromMediaPlayerVoice1(R.raw.h64);
//        } else if (value == 65) {
//            playFromMediaPlayerVoice1(R.raw.h65);
//        } else if (value == 66) {
//            playFromMediaPlayerVoice1(R.raw.h66);
//        } else if (value == 67) {
//            playFromMediaPlayerVoice1(R.raw.h67);
//        } else if (value == 68) {
//            playFromMediaPlayerVoice1(R.raw.h68);
//        } else if (value == 69) {
//            playFromMediaPlayerVoice1(R.raw.h69);
//        }
//    }

//    // Voice3の分岐（３：悪夢ゲーム）
//    private void setMusicVoice3(int value) {
//        if (value == 1) {
//            playFromMediaPlayerVoice1(R.raw.a3_1);
//        } else if (value == 2) {
//            playFromMediaPlayerVoice1(R.raw.a3_2);
//        } else if (value == 3) {
//            playFromMediaPlayerVoice1(R.raw.a3_3);
//        } else if (value == 4) {
//            playFromMediaPlayerVoice1(R.raw.a3_4);
//        } else if (value == 5) {
//            playFromMediaPlayerVoice1(R.raw.a3_5);
//        } else if (value == 6) {
//            playFromMediaPlayerVoice1(R.raw.a3_6);
//        } else if (value == 7) {
//            playFromMediaPlayerVoice1(R.raw.a3_7);
//        } else if (value == 8) {
//            playFromMediaPlayerVoice1(R.raw.a3_8);
//        } else if (value == 9) {
//            playFromMediaPlayerVoice1(R.raw.a3_9);
//        } else if (value == 10) {
//            playFromMediaPlayerVoice1(R.raw.a3_10);
//        } else if (value == 11) {
//            playFromMediaPlayerVoice1(R.raw.a3_11);
//        } else if (value == 12) {
//            playFromMediaPlayerVoice1(R.raw.a3_12);
//        } else if (value == 13) {
//            playFromMediaPlayerVoice1(R.raw.a3_13);
//        } else if (value == 14) {
//            playFromMediaPlayerVoice1(R.raw.a3_14);
//        } else if (value == 15) {
//            playFromMediaPlayerVoice1(R.raw.a3_15);
//        } else if (value == 16) {
//            playFromMediaPlayerVoice1(R.raw.a3_16);
//        }
//    }

    // Voice3の分岐
    private void setMusicVoice4(int value) {
        if (value == 1) {
            playFromMediaPlayerVoice1(R.raw.hahaha_1);
        } else if (value == 2) {
            playFromMediaPlayerVoice1(R.raw.maroni_sama);
        } else if (value == 3) {
            playFromMediaPlayerVoice1(R.raw.pleasestop_maroni_sama);
        }
    }

    private void stopVoices() {
        if (mediaPlayerVoice1 != null) {
            mediaPlayerVoice1 = null;
        }
        if (mediaPlayerVoice2 != null) {
            mediaPlayerVoice2 = null;
        }
        if (mediaPlayerVoice3 != null) {
            mediaPlayerVoice3 = null;
        }
    }

    // ストーリーをうまく遷移させる
    private void storyChooser(Button clickedButton) {

        // ボタン１（上）の場合
        if (clickedButton == findViewById(R.id.button1)) {
            // ストーリーを変更する前にストーリーの現在位置を初期化
            story.setNumber(0);
//            firstFlag = true;

            // さらにストーリフラグによって分岐
            if (story.getCurrentStory() == 1) {
                story.setStoryFlagNumber(2); // ゲームオーバー
            } else if (story.getCurrentStory() == 3) {
                story.setStoryFlagNumber(2); // ゲームオーバー
            } else if (story.getCurrentStory() == 4) {
                story.setStoryFlagNumber(5); // あの世まで行く王様
            }
            // ボタン２（下）の場合
        } else if (clickedButton == findViewById(R.id.button2)) {
            // ストーリーを変更する前にストーリーの現在位置を初期化
            story.setNumber(0);
//            firstFlag = true;

            // さらにストーリフラグによって分岐
            if (story.getCurrentStory() == 1) {
                story.setStoryFlagNumber(3); // 3章突入
            } else if (story.getCurrentStory() == 3) {
                story.setStoryFlagNumber(4); // 4章突入
            } else if (story.getCurrentStory() == 4) {

            }
        }
    }

    // テキストが背景色と混ざっても詠めるようにテキスト色を変更する：上部・下部
    // color 色情報
    private void setTextColor(String colorString) {
        textViewUp.setTextColor(Color.parseColor(colorString));
        textViewDown.setTextColor(Color.parseColor(colorString));
    }

    // テキストが背景色と混ざっても詠めるようにテキスト色を変更する：上部のみ
    // color 色情報
    private void setTextColorUp(String colorString) {
        textViewUp.setTextColor(Color.parseColor(colorString));
    }

    // テキストが背景色と混ざっても詠めるようにテキスト色を変更する：下部のみ
    // color 色情報
    private void setTextColorDown(String colorString) {
        textViewDown.setTextColor(Color.parseColor(colorString));
    }

     private android.media.MediaPlayer voicevoxTestPlayer;
     private java.io.File voicevoxTestWav;

     private void releaseVoicevoxTestPlayer() {
         if (voicevoxTestPlayer != null) {
             voicevoxTestPlayer.release();
             voicevoxTestPlayer = null;
         }
         if (voicevoxTestWav != null) {
             voicevoxTestWav.delete();
             voicevoxTestWav = null;
         }
     }

     // UIスレッドから呼ぶ。
     private void playVoicevoxTest(java.io.File wavFile) {
         releaseVoicevoxTestPlayer();
         voicevoxTestWav = wavFile;
         android.media.MediaPlayer player = new android.media.MediaPlayer();
         voicevoxTestPlayer = player;
         try {
             player.setAudioAttributes(new android.media.AudioAttributes.Builder()
                     .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                     .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                     .build());
             try (java.io.FileInputStream in = new java.io.FileInputStream(wavFile)) {
                 player.setDataSource(in.getFD());
             }
             player.setOnPreparedListener(p -> {
                 p.start();
                 android.util.Log.i("VoicevoxCheck", "ずんだもん再生開始");
             });
             player.setOnCompletionListener(p -> {
                 android.util.Log.i("VoicevoxCheck", "ずんだもん再生完了");
                 releaseVoicevoxTestPlayer();
             });
             player.setOnErrorListener((p, what, extra) -> {
                 android.util.Log.e("VoicevoxCheck",
                         "再生エラー: what=" + what + ", extra=" + extra);
                 releaseVoicevoxTestPlayer();
                 return true;
             });
             player.prepareAsync();
         } catch (Exception e) {
             releaseVoicevoxTestPlayer();
             android.util.Log.e("VoicevoxCheck", "WAV再生準備失敗", e);
             new android.app.AlertDialog.Builder(this)
                     .setTitle("VOICEVOX 再生失敗")
                     .setMessage(e.toString())
                     .setPositiveButton("OK", null)
                     .show();
         }
     }
 }
