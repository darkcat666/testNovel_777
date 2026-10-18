#include <jni.h>
#include <string>
#include <voicevox_core.h>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_darkcat_android_testnobel_MainActivity_synthesize(JNIEnv* env, jobject thiz, jstring text_, jstring dictPath) {
    const char* text = env->GetStringUTFChars(text_, 0);

    // =====================
    // 初期化（これが今の正解）
    // =====================
    VoicevoxInitializeOptions options = voicevox_make_default_initialize_options();
    options.acceleration_mode = VOICEVOX_ACCELERATION_MODE_AUTO;
    voicevox_make_default_initialize_options();

    // voicevox_synthesizer_new()の第一引数を取得
    const VoicevoxOnnxruntime *ort1;
    VoicevoxResultCode voicevoxResultCode = voicevox_onnxruntime_load_once(
            voicevox_make_default_load_onnxruntime_options(), &ort1);
    if (voicevoxResultCode != VOICEVOX_RESULT_OK) return nullptr;

    // voicevox_synthesizer_new()の第二引数を取得
    OpenJtalkRc *open_jtalk;
    char *jtalkPath = strcat(reinterpret_cast<char *const>(dictPath), "/open_jtalk_dic_utf_8-1.11"); // （！）あやしい
    voicevoxResultCode = voicevox_open_jtalk_rc_new(
            jtalkPath, // （！）あやしい
        &open_jtalk
    );
    if (voicevoxResultCode != VOICEVOX_RESULT_OK) return nullptr;

    // voicevox_synthesizer_new()の第三引数を取得
    struct VoicevoxInitializeOptions voicevoxInitializeOptions = voicevox_make_default_initialize_options();

    // voicevox_synthesizer_new()の第四引数を取得
    VoicevoxSynthesizer* synthesizer = nullptr;

    voicevoxResultCode = voicevox_synthesizer_new (
            ort1,
            open_jtalk,
            voicevoxInitializeOptions,
            &synthesizer
            );
    if (voicevoxResultCode != VOICEVOX_RESULT_OK) return nullptr;

    // =====================
    // モデルロード
    // =====================

    // voicevox_synthesizer_load_voice_model()の第二引数を取得
    VoicevoxVoiceModelFile* voicevoxVoiceModelFile = nullptr;
    char *model_0Path = strcat(reinterpret_cast<char *const>(dictPath), "/0.vvm"); // （！）あやしい
    voicevox_voice_model_file_open(
            model_0Path,
            &voicevoxVoiceModelFile
            );

    voicevoxResultCode = voicevox_synthesizer_load_voice_model(synthesizer,
                                                               voicevoxVoiceModelFile);
    if (voicevoxResultCode != VOICEVOX_RESULT_OK) return nullptr;

    // =====================
    // AudioQuery生成
    // =====================
    char* query = nullptr;
    char* output_audio_query_json = nullptr;
    // voicevox_audio_query_create_from_accent_phrases()の第一引数を取得
    voicevox_synthesizer_create_audio_query(
            synthesizer,
            text,
            3, // 仮
            &output_audio_query_json
            );

    voicevoxResultCode = voicevox_audio_query_create_from_accent_phrases(output_audio_query_json, &query);
    if (voicevoxResultCode != VOICEVOX_RESULT_OK) return nullptr;

    // =====================
    // 音声生成
    // =====================
    uint8_t* wav = nullptr;
    uintptr_t wav_size = 0;

    voicevoxResultCode = voicevox_synthesizer_tts(synthesizer, text, 3, voicevox_make_default_tts_options(), &wav_size, &wav);
    if (voicevoxResultCode != VOICEVOX_RESULT_OK) return nullptr;

    // =====================
    // Javaへ返却
    // =====================
    jbyteArray resultArray = env->NewByteArray(wav_size);
    env->SetByteArrayRegion(resultArray, 0, wav_size, (jbyte*)wav);

    // =====================
    // 解放
    // =====================
    voicevox_wav_free(wav);
    voicevox_json_free(query);

    env->ReleaseStringUTFChars(text_, text);

    return resultArray;
}