#include <jni.h>
#include <memory>
#include <string>
#include "voicevox_core.h"
#include <limits>
#include <vector>
#include <exception>
#include <new>


namespace {
void throwIo(JNIEnv* env, const char* stage, VoicevoxResultCode code) {
    // エラーの段階と結果コードを確実にJavaへ返す。
    std::string message = std::string(stage)
            + " failed (VOICEVOX result="
            + std::to_string(static_cast<int>(code)) + ")";
    jclass cls = env->FindClass("java/io/IOException");
    if (cls != nullptr) {
        env->ThrowNew(cls, message.c_str());
        env->DeleteLocalRef(cls);
    }
}

// Javaから受け取ったパスを、処理終了時に必ず解放する。
class JniPath {
public:
    JniPath(JNIEnv* env, jstring value)
            : env_(env), value_(value),
              chars_(env->GetStringUTFChars(value, nullptr)) {}
    ~JniPath() {
        if (chars_ != nullptr) {
            env_->ReleaseStringUTFChars(value_, chars_);
        }
    }
    const char* get() const { return chars_; }
private:
    JNIEnv* env_;
    jstring value_;
    const char* chars_;
};
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_darkcat_android_testnobel_VoicevoxBridge_getCoreVersion(
        JNIEnv* env, jclass) {
    return env->NewStringUTF(voicevox_get_version());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_darkcat_android_testnobel_VoicevoxBridge_inspectData(
        JNIEnv* env, jclass, jstring dictionaryPath, jstring modelPath) {
    if (dictionaryPath == nullptr || modelPath == nullptr) {
        jclass cls = env->FindClass("java/lang/IllegalArgumentException");
        if (cls != nullptr) {
            env->ThrowNew(cls, "Paths must not be null");
            env->DeleteLocalRef(cls);
        }
        return nullptr;
    }

    JniPath dictionary(env, dictionaryPath);
    if (dictionary.get() == nullptr) return nullptr;
    JniPath modelPathUtf(env, modelPath);
    if (modelPathUtf.get() == nullptr) return nullptr;

    OpenJtalkRc* dictionaryRaw = nullptr;
    VoicevoxResultCode code = voicevox_open_jtalk_rc_new(
            dictionary.get(), &dictionaryRaw);
    if (code != VOICEVOX_RESULT_OK) {
        throwIo(env, "Open JTalk dictionary", code);
        return nullptr;
    }
    std::unique_ptr<OpenJtalkRc, decltype(&voicevox_open_jtalk_rc_delete)>
            dictionaryHandle(dictionaryRaw, voicevox_open_jtalk_rc_delete);

    VoicevoxVoiceModelFile* modelRaw = nullptr;
    code = voicevox_voice_model_file_open(modelPathUtf.get(), &modelRaw);
    if (code != VOICEVOX_RESULT_OK) {
        throwIo(env, "VVM open", code);
        return nullptr;
    }
    std::unique_ptr<VoicevoxVoiceModelFile,
            decltype(&voicevox_voice_model_file_delete)>
            model(modelRaw, voicevox_voice_model_file_delete);

    char* jsonRaw = voicevox_voice_model_file_create_metas_json(model.get());
    std::unique_ptr<char, decltype(&voicevox_json_free)>
            json(jsonRaw, voicevox_json_free);
    std::string metadata(json.get());

    // 標準UTF-8をバイト列で返し、日本語や補助文字の文字化けを避ける。
    jbyteArray output = env->NewByteArray(static_cast<jsize>(metadata.size()));
    if (output == nullptr) return nullptr;
    env->SetByteArrayRegion(output, 0, static_cast<jsize>(metadata.size()),
            reinterpret_cast<const jbyte*>(metadata.data()));
    return output;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_darkcat_android_testnobel_VoicevoxBridge_ttsOnce(
        JNIEnv* env, jclass,
        jstring dictionaryPath, jstring modelPath,
        jbyteArray textUtf8, jint styleId) {
    try {
        if (dictionaryPath == nullptr || modelPath == nullptr
                || textUtf8 == nullptr || styleId < 0) {
            jclass cls = env->FindClass("java/lang/IllegalArgumentException");
            if (cls != nullptr) {
                env->ThrowNew(cls, "Invalid paths, text or style ID");
                env->DeleteLocalRef(cls);
            }
            return nullptr;
        }

        JniPath dictionary(env, dictionaryPath);
        if (dictionary.get() == nullptr) return nullptr;
        JniPath modelPathUtf(env, modelPath);
        if (modelPathUtf.get() == nullptr) return nullptr;

        const jsize textLength = env->GetArrayLength(textUtf8);
        std::vector<char> text(static_cast<size_t>(textLength) + 1, '\0');
        if (textLength > 0) {
            env->GetByteArrayRegion(textUtf8, 0, textLength,
                    reinterpret_cast<jbyte*>(text.data()));
            if (env->ExceptionCheck()) return nullptr;
        }
        // C APIの文字列はヌル終端なので、本文中のNULは拒否する。
        for (jsize i = 0; i < textLength; ++i) {
            if (text[static_cast<size_t>(i)] == '\0') {
                jclass cls = env->FindClass("java/lang/IllegalArgumentException");
                if (cls != nullptr) {
                    env->ThrowNew(cls, "Text must not contain NUL");
                    env->DeleteLocalRef(cls);
                }
                return nullptr;
            }
        }

        // JavaでのSystem.loadLibraryだけでなく、CORE側も初期化する。
        const VoicevoxOnnxruntime* runtime = nullptr;
        auto runtimeOptions = voicevox_make_default_load_onnxruntime_options();
        runtimeOptions.filename = "libvoicevox_onnxruntime.so";
        VoicevoxResultCode code = voicevox_onnxruntime_load_once(
                runtimeOptions, &runtime);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "ONNX Runtime initialization", code);
            return nullptr;
        }

        OpenJtalkRc* dictionaryRaw = nullptr;
        code = voicevox_open_jtalk_rc_new(dictionary.get(), &dictionaryRaw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Open JTalk dictionary", code);
            return nullptr;
        }
        std::unique_ptr<OpenJtalkRc, decltype(&voicevox_open_jtalk_rc_delete)>
                dictionaryHandle(dictionaryRaw, voicevox_open_jtalk_rc_delete);

        auto initializeOptions = voicevox_make_default_initialize_options();
        initializeOptions.acceleration_mode = VOICEVOX_ACCELERATION_MODE_CPU;
        initializeOptions.cpu_num_threads = 2;

        VoicevoxSynthesizer* synthesizerRaw = nullptr;
        code = voicevox_synthesizer_new(runtime, dictionaryHandle.get(),
                initializeOptions, &synthesizerRaw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Synthesizer initialization", code);
            return nullptr;
        }
        std::unique_ptr<VoicevoxSynthesizer,
                decltype(&voicevox_synthesizer_delete)>
                synthesizer(synthesizerRaw, voicevox_synthesizer_delete);

        VoicevoxVoiceModelFile* modelRaw = nullptr;
        code = voicevox_voice_model_file_open(modelPathUtf.get(), &modelRaw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "VVM open", code);
            return nullptr;
        }
        std::unique_ptr<VoicevoxVoiceModelFile,
                decltype(&voicevox_voice_model_file_delete)>
                model(modelRaw, voicevox_voice_model_file_delete);

        code = voicevox_synthesizer_load_voice_model(
                synthesizer.get(), model.get(),
                voicevox_make_default_load_voice_model_options());
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Voice model load", code);
            return nullptr;
        }

        uintptr_t wavLength = 0;
        uint8_t* wavRaw = nullptr;
        code = voicevox_synthesizer_tts(synthesizer.get(), text.data(),
                static_cast<VoicevoxStyleId>(styleId),
                voicevox_make_default_tts_options(), &wavLength, &wavRaw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Text to speech", code);
            return nullptr;
        }
        std::unique_ptr<uint8_t, decltype(&voicevox_wav_free)>
                wav(wavRaw, voicevox_wav_free);

        if (wavLength > static_cast<uintptr_t>(
                std::numeric_limits<jsize>::max())) {
            jclass cls = env->FindClass("java/io/IOException");
            if (cls != nullptr) {
                env->ThrowNew(cls, "WAV is too large for a Java byte array");
                env->DeleteLocalRef(cls);
            }
            return nullptr;
        }

        jbyteArray output = env->NewByteArray(static_cast<jsize>(wavLength));
        if (output == nullptr) return nullptr;
        env->SetByteArrayRegion(output, 0, static_cast<jsize>(wavLength),
                reinterpret_cast<const jbyte*>(wav.get()));
        if (env->ExceptionCheck()) return nullptr;
        return output;
    } catch (const std::bad_alloc&) {
        if (!env->ExceptionCheck()) {
            jclass cls = env->FindClass("java/lang/OutOfMemoryError");
            if (cls != nullptr) {
                env->ThrowNew(cls, "Native allocation failed");
                env->DeleteLocalRef(cls);
            }
        }
        return nullptr;
    } catch (const std::exception&) {
        if (!env->ExceptionCheck()) {
            jclass cls = env->FindClass("java/io/IOException");
            if (cls != nullptr) {
                env->ThrowNew(cls, "Unexpected native exception");
                env->DeleteLocalRef(cls);
            }
        }
        return nullptr;
    }
}

namespace {
void readerException(JNIEnv* env, const char* clsName, const char* message) {
    if (env->ExceptionCheck()) return;
    jclass cls = env->FindClass(clsName);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_darkcat_android_testnobel_VoicevoxBridge_createReader(
        JNIEnv* env, jclass, jstring dictionaryPath, jstring modelPath) {
    try {
        if (!dictionaryPath || !modelPath) {
            readerException(env, "java/lang/IllegalArgumentException", "Null path");
            return 0;
        }
        JniPath dictPath(env, dictionaryPath);
        if (!dictPath.get()) return 0;
        JniPath vvmPath(env, modelPath);
        if (!vvmPath.get()) return 0;

        const VoicevoxOnnxruntime* runtime = nullptr;
        auto rtOptions = voicevox_make_default_load_onnxruntime_options();
        rtOptions.filename = "libvoicevox_onnxruntime.so";
        auto code = voicevox_onnxruntime_load_once(rtOptions, &runtime);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "ONNX Runtime initialization", code);
            return 0;
        }

        OpenJtalkRc* dictRaw = nullptr;
        code = voicevox_open_jtalk_rc_new(dictPath.get(), &dictRaw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Open JTalk dictionary", code);
            return 0;
        }
        std::unique_ptr<OpenJtalkRc, decltype(&voicevox_open_jtalk_rc_delete)>
                dict(dictRaw, voicevox_open_jtalk_rc_delete);

        auto options = voicevox_make_default_initialize_options();
        options.acceleration_mode = VOICEVOX_ACCELERATION_MODE_CPU;
        options.cpu_num_threads = 2;
        VoicevoxSynthesizer* synthRaw = nullptr;
        code = voicevox_synthesizer_new(runtime, dict.get(), options, &synthRaw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Synthesizer initialization", code);
            return 0;
        }
        std::unique_ptr<VoicevoxSynthesizer, decltype(&voicevox_synthesizer_delete)>
                synth(synthRaw, voicevox_synthesizer_delete);

        VoicevoxVoiceModelFile* modelRaw = nullptr;
        code = voicevox_voice_model_file_open(vvmPath.get(), &modelRaw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "VVM open", code);
            return 0;
        }
        std::unique_ptr<VoicevoxVoiceModelFile,
                decltype(&voicevox_voice_model_file_delete)>
                model(modelRaw, voicevox_voice_model_file_delete);
        code = voicevox_synthesizer_load_voice_model(synth.get(), model.get(),
                voicevox_make_default_load_voice_model_options());
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Voice model load", code);
            return 0;
        }
        return reinterpret_cast<jlong>(synth.release());
    } catch (const std::bad_alloc&) {
        readerException(env, "java/lang/OutOfMemoryError", "Native allocation failed");
    } catch (const std::exception&) {
        readerException(env, "java/io/IOException", "Reader initialization failed");
    }
    return 0;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_darkcat_android_testnobel_VoicevoxBridge_synthesizeReader(
        JNIEnv* env, jclass, jlong handle, jbyteArray textUtf8, jint styleId) {
    try {
        if (!handle || !textUtf8 || styleId < 0) {
            readerException(env, "java/lang/IllegalArgumentException", "Invalid argument");
            return nullptr;
        }
        const jsize n = env->GetArrayLength(textUtf8);
        std::vector<char> text(static_cast<size_t>(n) + 1, '\0');
        if (n > 0) {
            env->GetByteArrayRegion(textUtf8, 0, n,
                    reinterpret_cast<jbyte*>(text.data()));
            if (env->ExceptionCheck()) return nullptr;
        }
        for (jsize i = 0; i < n; ++i) {
            if (text[static_cast<size_t>(i)] == '\0') {
                readerException(env, "java/lang/IllegalArgumentException", "Text contains NUL");
                return nullptr;
            }
        }

        uintptr_t length = 0;
        uint8_t* raw = nullptr;
        auto code = voicevox_synthesizer_tts(
                reinterpret_cast<VoicevoxSynthesizer*>(handle), text.data(),
                static_cast<VoicevoxStyleId>(styleId),
                voicevox_make_default_tts_options(), &length, &raw);
        if (code != VOICEVOX_RESULT_OK) {
            throwIo(env, "Text to speech", code);
            return nullptr;
        }
        std::unique_ptr<uint8_t, decltype(&voicevox_wav_free)>
                wav(raw, voicevox_wav_free);
        if (length > static_cast<uintptr_t>(std::numeric_limits<jsize>::max())) {
            readerException(env, "java/io/IOException", "WAV too large");
            return nullptr;
        }
        auto output = env->NewByteArray(static_cast<jsize>(length));
        if (!output) return nullptr;
        env->SetByteArrayRegion(output, 0, static_cast<jsize>(length),
                reinterpret_cast<const jbyte*>(wav.get()));
        if (env->ExceptionCheck()) return nullptr;
        return output;
    } catch (const std::bad_alloc&) {
        readerException(env, "java/lang/OutOfMemoryError", "Native allocation failed");
    } catch (const std::exception&) {
        readerException(env, "java/io/IOException", "Speech synthesis failed");
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_darkcat_android_testnobel_VoicevoxBridge_destroyReader(
        JNIEnv*, jclass, jlong handle) {
    if (handle) {
        voicevox_synthesizer_delete(reinterpret_cast<VoicevoxSynthesizer*>(handle));
    }
}
