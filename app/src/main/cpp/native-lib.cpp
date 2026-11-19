#include <jni.h>
#include "AudioEngine.h"

static AudioEngine engine;

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_startAudioEngine(JNIEnv *env, jobject) { engine.start(); }

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_setDspMode(JNIEnv *env, jobject, jint mode) { engine.setMode(mode); }

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_writePcm(JNIEnv *env, jobject, jfloatArray pcmData) {
    jsize len = env->GetArrayLength(pcmData);
    jfloat *body = env->GetFloatArrayElements(pcmData, 0);
    engine.writePcm(body, len);
    env->ReleaseFloatArrayElements(pcmData, body, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_setDspMode(JNIEnv *env, jobject thiz, jint mode) {
    // Здесь мы вызываем функцию из твоего DSPEngine (или глобальную переменную)
    // Предполагаем, что у тебя есть объект dsp в AudioEngine, 
    // или глобальный указатель. Если что-то не так, мы поправим имя.
    if (g_audioEngine) {
        g_audioEngine->setDspMode(static_cast<int>(mode));
    }
}
