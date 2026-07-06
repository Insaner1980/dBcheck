package com.dbcheck.app.domain.audio

data class AudioInputDevice(
    val id: Int,
    val displayName: String,
    val type: AudioInputDeviceType,
    val isExternal: Boolean,
    val sampleRatesHz: List<Int> = emptyList(),
    val channelCounts: List<Int> = emptyList(),
)

enum class AudioInputDeviceType {
    BUILT_IN_MIC,
    WIRED_HEADSET,
    USB,
    BLUETOOTH,
    OTHER,
}
