package com.darkcat.android.testnobel;

public final class VoicevoxBridge {
    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("voicevox_onnxruntime");
        System.loadLibrary("voicevox_core");
        System.loadLibrary("voicevox_jni");
    }

    // 下記のハンドル操作はVoicevoxReaderの専用スレッドからのみ呼びます。
    public static native long createReader(String dictionaryPath, String modelPath)
            throws java.io.IOException;
    public static native byte[] synthesizeReader(long handle, byte[] textUtf8, int styleId)
            throws java.io.IOException;
    public static native void destroyReader(long handle);


    public static native byte[] ttsOnce(
            String dictionaryPath,
            String modelPath,
            byte[] textUtf8,
            int styleId) throws java.io.IOException;


    private VoicevoxBridge() {
    }

    public static native String getCoreVersion();

    public static native byte[] inspectData(String dictionaryPath, String modelPath)
            throws java.io.IOException;

}
