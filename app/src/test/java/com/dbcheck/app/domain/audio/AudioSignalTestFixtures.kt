package com.dbcheck.app.domain.audio

import kotlin.math.PI
import kotlin.math.sin

internal fun sineWaveChunk(frequencyHz: Double, amplitude: Int = 12_000): ShortArray =
    ShortArray(AudioProcessingConfig.CHUNK_SIZE) { index ->
        (sin(2.0 * PI * frequencyHz * index / AudioProcessingConfig.SAMPLE_RATE) * amplitude)
            .toInt()
            .toShort()
    }
