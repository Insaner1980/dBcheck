package com.dbcheck.app.ui.meter.state

import com.dbcheck.app.data.model.NoiseLevel

data class MeterUiState(
    val currentDb: Float = 0f,
    val minDb: Float = 0f,
    val avgDb: Float = 0f,
    val maxDb: Float = 0f,
    val noiseLevel: NoiseLevel = NoiseLevel.QUIET,
    val isRecording: Boolean = false,
    val sessionDurationMs: Long = 0L,
    val isMicPermissionGranted: Boolean = false,
    val error: String? = null,
    val waveformData: List<Float> = emptyList(),
)
