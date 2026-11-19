#include <jni.h>
#include "AudioEngine.h"

static AudioEngine engine;

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_startAudioEngine(JNIEnv *env, jobject) { engine.start(); }

extern "C" JNIEXPORT void JNICALL

extern "C" JNIEXPORT void JNICALL

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_setDspMode(JNIEnv *env, jobject thiz, jint mode) {
    // ВАЖНО: Если у тебя в AudioEngine.cpp есть глобальный указатель, 
    // например std::unique_ptr<AudioEngine> g_audioEngine;, 
    // то раскомментируй и используй его. 
    // Если его нет — код ниже создаст временный объект (заглушку).
    
    // Пока используем заглушку, чтобы сборка прошла, а звук не крашился:
    // (Позже мы свяжем это с твоим реальным движком)
    // extern std::unique_ptr<AudioEngine> g_audioEngine;
    // if (g_audioEngine) { g_audioEngine->setDspMode(mode); }
}
