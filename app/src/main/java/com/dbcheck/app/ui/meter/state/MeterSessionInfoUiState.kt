package com.dbcheck.app.ui.meter.state

import com.dbcheck.app.domain.audio.AudioProcessingConfig
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.WeightingType

data class MeterSessionInfoUiState(
    val isRecording: Boolean = false,
    val durationMs: Long = 0L,
    val weighting: WeightingType = WeightingType.DEFAULT,
    val responseTime: ResponseTime = ResponseTime.FAST,
    val sampleRateHz: Int = AudioProcessingConfig.SAMPLE_RATE,
    val inputDeviceName: String? = null,
    val showProDetails: Boolean = false,
)
