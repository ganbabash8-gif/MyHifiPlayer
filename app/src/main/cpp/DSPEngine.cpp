#include "DSPEngine.h"
#include <cmath>
#include <cstdlib>

void DSPEngine::init(int sampleRate) { sampleRate_ = sampleRate; }
void DSPEngine::setMode(int mode) { currentMode_ = mode; }
float DSPEngine::generateNoise() { return ((float)rand() / RAND_MAX) * 2.0f - 1.0f; }

void DSPEngine::process(float* buffer, int32_t numFrames) {
    if (currentMode_ == 0) {
        // Direct (только защита от клиппинга)
        for (int i = 0; i < numFrames * 2; ++i) {
            buffer[i] = buffer[i] / (1.0f + fabsf(buffer[i]) * 0.1f);
        }
        return;
    }
    processPhilips(buffer, numFrames);
}

void DSPEngine::processPhilips(float* buffer, int32_t numFrames) {
    // Мягкий фильтр ВЧ + дизеринг (без шума, только сглаживание)
    float alpha = 0.15f; // Коэффициент сглаживания
    float dither = 0.00005f;
    
    for (int i = 0; i < numFrames; ++i) {
        for (int ch = 0; ch < 2; ++ch) {
            int idx = i * 2 + ch;
            float sample = buffer[idx];
            
            // Сглаживание ВЧ (имитация аналогового фильтра)
            lowShelfState_[ch] = lowShelfState_[ch] * (1.0f - alpha) + sample * alpha;
            sample = lowShelfState_[ch] * 0.6f + sample * 0.4f;
            
            // Дизеринг (ультра-тихий, для аналоговой глубины)
            float noise = generateNoise() * dither;
            float shaped = sample + noise - ditherError_[ch];
            ditherError_[ch] = (sample + noise) - shaped;
            
            // Мягкое насыщение
            buffer[idx] = shaped / (1.0f + fabsf(shaped) * 0.03f);
        }
    }
}
