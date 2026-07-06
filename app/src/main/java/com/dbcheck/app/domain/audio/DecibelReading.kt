package com.dbcheck.app.domain.audio

data class DecibelReading(
    val instantDb: Float,
    val weightedDb: Float,
    val aWeightedDb: Float = weightedDb,
    val timestamp: Long,
    val peakAmplitude: Float,
    val peakDb: Float = instantDb,
)
