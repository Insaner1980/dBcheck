package com.dbcheck.app.domain.audio

data class AudioInputInfo(
    val sampleRateHz: Int = AudioProcessingConfig.SAMPLE_RATE,
    val inputDeviceName: String? = null,
    val selectedDeviceId: Int? = null,
    val selectedDeviceName: String? = null,
    val routedDeviceName: String? = inputDeviceName,
)
