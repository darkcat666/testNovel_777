package com.darkcat.android.testnobel;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class VoicevoxAssets {
    private VoicevoxAssets() {}

    // 動作確認用。同時実行を避け、呼ぶたびにassetsからコピーし直します。
    public static synchronized String prepare(Context context) throws IOException {
        File root = new File(context.getFilesDir(), "voicevox");
        copyTree(context.getAssets(), "voicevox", root);

        File dictionary = new File(root, "open_jtalk_dic_utf_8-1.11");
        File model = new File(root, "models/0.vvm");

        requireFile(new File(dictionary, "sys.dic"));
        requireFile(new File(dictionary, "char.bin"));
        requireFile(new File(dictionary, "matrix.bin"));
        requireFile(model);

        return "辞書・モデルのコピー成功"
                + "\n\n辞書: " + dictionary.getAbsolutePath()
                + "\n\nモデル: " + model.getAbsolutePath()
                + "\nモデル容量: " + model.length() + " bytes";
    }

    private static void copyTree(AssetManager assets, String source, File dest)
            throws IOException {
        String[] children = assets.list(source);
        if (children == null) {
            throw new IOException("assetsを確認できません: " + source);
        }

        if (children.length > 0) {
            if (!dest.isDirectory() && !dest.mkdirs()) {
                throw new IOException("フォルダを作成できません: " + dest);
            }
            for (String child : children) {
                copyTree(assets, source + "/" + child, new File(dest, child));
            }
            return;
        }

        // このコピー対象には空フォルダを含めず、末端をファイルとして扱います。
        File parent = dest.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("フォルダを作成できません: " + parent);
        }

        File temp = new File(dest.getAbsolutePath() + ".tmp");
        try {
            try (InputStream in = assets.open(source);
                 FileOutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
            }
            if (!temp.renameTo(dest)) {
                throw new IOException("コピー先を更新できません: " + dest);
            }
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    private static void requireFile(File file) throws IOException {
        if (!file.isFile() || file.length() == 0) {
            throw new IOException("必要なファイルがないか、空です: " + file);
        }
    }
}
