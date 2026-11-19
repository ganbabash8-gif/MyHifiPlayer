#ifndef DSPENGINE_H
#define DSPENGINE_H
#include <cstdint>

class DSPEngine {
public:
    void init(int sampleRate);
    void setMode(int mode);
    void process(float* buffer, int32_t numFrames);
private:
    int sampleRate_ = 48000;
    int currentMode_ = 1; // По умолчанию Philips Smooth
    float lowShelfState_[2] = {0.0f, 0.0f};
    float ditherError_[2] = {0.0f, 0.0f};
    float generateNoise();
    void processPhilips(float* buffer, int32_t numFrames);
};
#endif
