#include "AudioEngine.h"
#include <android/log.h>
#include <cstring>

#define LOG_TAG "AudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

bool AudioEngine::start() {
    oboe::AudioStreamBuilder builder;
    builder.setBufferSizeInFrames(4096);
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::None)
           ->setSharingMode(oboe::SharingMode::Exclusive) // <--- НАШ СВЯТОЙ ГРААЛЬ
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setDataCallback(this);

    oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK) {
        LOGI("Exclusive mode failed, falling back to Shared.");
        builder.setSharingMode(oboe::SharingMode::Shared);
        result = builder.openStream(stream_);
    }
    
    if (result != oboe::Result::OK) return false;

    if (stream_->getSharingMode() == oboe::SharingMode::Exclusive) {
        LOGI("ПОБЕДА: Мы в эксклюзивном режиме (MMAP)!");
    } else {
        LOGI("Работаем в Shared Mode (микшер активен).");
    }

    dsp_.init(stream_->getSampleRate());
    stream_->requestStart();
    return true;
}

void AudioEngine::stop() {
    if (stream_) { stream_->requestStop(); stream_->close(); stream_.reset(); }
}

void AudioEngine::setMode(int mode) { dsp_.setMode(mode); }

void AudioEngine::writePcm(const float* data, int32_t numFloats) {
    int32_t currentWrite = writeIndex_.load(std::memory_order_relaxed);
    for (int32_t i = 0; i < numFloats; ++i) {
        ringBuffer_[currentWrite] = data[i];
        currentWrite = (currentWrite + 1) % BUFFER_SIZE;
    }
    writeIndex_.store(currentWrite, std::memory_order_release);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) {
    float *outData = static_cast<float *>(audioData);
    int32_t numFloats = numFrames * 2;
    
    int32_t currentRead = readIndex_.load(std::memory_order_relaxed);
    int32_t currentWrite = writeIndex_.load(std::memory_order_acquire);
    
    int32_t available = (currentWrite - currentRead + BUFFER_SIZE) % BUFFER_SIZE;
    
    for (int32_t i = 0; i < numFloats; ++i) {
        if (i < available) {
            outData[i] = ringBuffer_[(currentRead + i) % BUFFER_SIZE];
        } else {
            outData[i] = 0.0f; // Тишина, если буфер пуст
        }
    }
    
    readIndex_.store((currentRead + numFloats) % BUFFER_SIZE, std::memory_order_release);
    
    // Прогоняем через наш DSP
    dsp_.process(outData, numFrames);
    return oboe::DataCallbackResult::Continue;
}
