#include <jni.h>
#include <vector>
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

extern "C" JNIEXPORT void JNICALL
Java_com_example_myhifiplayer_MainActivity_writePcm(JNIEnv *env, jobject thiz, jfloatArray pcmData) {
    if (pcmData == nullptr) return;
    jsize len = env->GetArrayLength(pcmData);
    if (len <= 0) return;

    std::vector<jfloat> buffer(len);
    env->GetFloatArrayRegion(pcmData, 0, len, buffer.data());
    engine.writePcm(reinterpret_cast<const float*>(buffer.data()), static_cast<int32_t>(len));
}
