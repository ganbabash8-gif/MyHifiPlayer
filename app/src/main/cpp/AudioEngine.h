#ifndef AUDIOENGINE_H
#define AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <vector>
#include "DSPEngine.h"

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    bool start();
    void stop();
    void setMode(int mode);
    void writePcm(const float* data, int32_t numFloats);
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> stream_;
    DSPEngine dsp_;
    
    // Простой кольцевой буфер на атомиках
    static constexpr int32_t BUFFER_SIZE = 48000 * 2 * 2; // 2 секунды стерео
    std::vector<float> ringBuffer_ = std::vector<float>(BUFFER_SIZE, 0.0f);
    std::atomic<int32_t> writeIndex_{0};
    std::atomic<int32_t> readIndex_{0};
};

#endif
