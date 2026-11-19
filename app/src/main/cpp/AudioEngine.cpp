#include "AudioEngine.h"
#include <android/log.h>
#include <algorithm>
#include <cstring>

#define LOG_TAG "AudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

bool AudioEngine::start() {
    oboe::AudioStreamBuilder builder;
    builder.setBufferCapacityInFrames(8192)
           ->setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::None)
           ->setSharingMode(oboe::SharingMode::Exclusive)
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
        LOGI("Работаем в Shared Mode (миксер активен).");
    }

    dsp_.init(stream_->getSampleRate());
    stream_->requestStart();
    return true;
}

void AudioEngine::stop() {
    if (stream_) {
        stream_->requestStop();
        stream_->close();
        stream_.reset();
    }
}

void AudioEngine::setMode(int mode) {
    dsp_.setMode(mode);
}

void AudioEngine::writePcm(const float* data, int32_t numFloats) {
    if (!data || numFloats <= 0) return;

    int32_t currentWrite = writeIndex_.load(std::memory_order_relaxed);
    int32_t currentRead = readIndex_.load(std::memory_order_acquire);

    int32_t used = (currentWrite - currentRead + BUFFER_SIZE) % BUFFER_SIZE;
    int32_t freeSpace = (BUFFER_SIZE - 1) - used;
    int32_t toWrite = std::min(numFloats, freeSpace);

    for (int32_t i = 0; i < toWrite; ++i) {
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
    int32_t toRead = std::min(numFloats, available);

    for (int32_t i = 0; i < toRead; ++i) {
        outData[i] = ringBuffer_[(currentRead + i) % BUFFER_SIZE];
    }

    for (int32_t i = toRead; i < numFloats; ++i) {
        outData[i] = 0.0f;
    }

    int32_t nextRead = (currentRead + toRead) % BUFFER_SIZE;
    readIndex_.store(nextRead, std::memory_order_release);

    dsp_.process(outData, numFrames);
    return oboe::DataCallbackResult::Continue;
}
