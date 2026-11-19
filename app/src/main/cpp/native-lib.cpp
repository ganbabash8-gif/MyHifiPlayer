#include <jni.h>
#include "AudioEngine.h"

static AudioEngine engine;

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_startAudioEngine(JNIEnv *env, jobject thiz) {
    engine.start();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_stopAudioEngine(JNIEnv *env, jobject thiz) {
    engine.stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_setDspMode(JNIEnv *env, jobject thiz, jint mode) {
    engine.setMode(mode);
}
