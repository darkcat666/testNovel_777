cmake_minimum_required(VERSION 3.22.1)
project(voicevox_android LANGUAGES CXX)

add_library(voicevox_core SHARED IMPORTED)
set_target_properties(voicevox_core PROPERTIES
        IMPORTED_LOCATION
        "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libvoicevox_core.so"
        IMPORTED_NO_SONAME TRUE
)

add_library(voicevox_jni SHARED voicevox_jni.cpp)

target_compile_features(voicevox_jni PRIVATE cxx_std_17)
target_include_directories(voicevox_jni PRIVATE
        "${CMAKE_CURRENT_SOURCE_DIR}/include"
)

target_link_libraries(voicevox_jni PRIVATE voicevox_core)
