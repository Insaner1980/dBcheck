package com.dbcheck.app.ui.meter.state

import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.noise.NoiseLevel

data class MeterUiState(
    val currentDb: Float = 0f,
    val minDb: Float = 0f,
    val avgDb: Float = 0f,
    val maxDb: Float = 0f,
    val peakDb: Float = 0f,
    val sampleCount: Int = 0,
    val noiseLevel: NoiseLevel = NoiseLevel.QUIET,
    val isRecording: Boolean = false,
    val sessionDurationMs: Long = 0L,
    val isMicPermissionGranted: Boolean = false,
    val showMicDeniedPrompt: Boolean = false,
    val error: String? = null,
    val waveformData: List<Float> = emptyList(),
    val waveformStyle: WaveformStyle = WaveformStyle.LINE,
    val refreshRate: MeterRefreshRate = MeterRefreshRate.STANDARD,
    val completedSessionId: Long? = null,
) {
    val canShare: Boolean = sampleCount > 0
}
