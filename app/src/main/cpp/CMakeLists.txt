#cmake_minimum_required(VERSION 3.22)
#
#project(testFGO)
#
#set(CMAKE_ANDROID_STL_TYPE c++_shared)
#
#add_library(native-lib SHARED
##        native-lib.cpp
#        voicevox_jni.cpp
#)
#
### 16KB page support
##target_link_options(native-lib PRIVATE
##        "-Wl,-z,max-page-size=16384"
##)
#
## =========================
## IMPORTED LIBRARIES
## =========================
#
##add_library(voicevox_core SHARED IMPORTED GLOBAL)
##
##set_target_properties(voicevox_core PROPERTIES
##        IMPORTED_NO_SONAME FALSE
##        IMPORTED_LOCATION
##        "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libvoicevox_core.so"
##        IMPORTED_SONAME
##        "libvoicevox_core.so"
##)
##
##add_library(voicevox_onnxruntime SHARED IMPORTED GLOBAL)
##
##set_target_properties(voicevox_onnxruntime PROPERTIES
##        IMPORTED_NO_SONAME FALSE
##        IMPORTED_LOCATION
##        "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libvoicevox_onnxruntime.so"
##        IMPORTED_SONAME
##        "libvoicevox_onnxruntime.so"
##)
#
## IMPORTEDライブラリ（パス指定しない）
#add_library(voicevox_core SHARED IMPORTED)
#add_library(voicevox_onnxruntime SHARED IMPORTED)
#
## ★ここが重要（ビルド用パス）
#set_target_properties(voicevox_core PROPERTIES
#        IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libvoicevox_core.so
#        IMPORTED_NO_SONAME TRUE   # ★これ追加
#)
#
#set_target_properties(voicevox_onnxruntime PROPERTIES
#        IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libvoicevox_onnxruntime.so
#        IMPORTED_NO_SONAME TRUE   # ★これ追加
#)
#
#include_directories(${CMAKE_SOURCE_DIR}/include)
#
#find_library(VOICEVOX_CORE voicevox_core)
#find_library(VOICEVOX_ONNX voicevox_onnxruntime)
#
#target_link_libraries(
#        native-lib
#        PRIVATE
#        voicevox_core
#        voicevox_onnxruntime
#        log
#        atomic
#        m
#)