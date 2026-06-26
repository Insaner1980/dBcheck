package com.dbcheck.app.ui.meter.state

import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.noise.SoundReferenceCatalog
import com.dbcheck.app.domain.noise.SoundReferenceMarker
import com.dbcheck.app.domain.report.equivalentLevelLabelForWeighting

enum class MeasurementMode {
    DB_METER,
    DOSIMETER,
}

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
    val notificationPermissionAlreadyRequested: Boolean = false,
    val error: String? = null,
    val waveformData: List<Float> = emptyList(),
    val liveChartPoints: List<LiveChartPointUiState> = emptyList(),
    val soundReferenceMarkers: List<SoundReferenceMarker> = SoundReferenceCatalog.referenceMarkers,
    val nearestSoundReferenceMarker: SoundReferenceMarker = SoundReferenceCatalog.nearestReferenceMarker(currentDb),
    val soundReferenceCurrentPosition: Float = SoundReferenceCatalog.markerPosition(currentDb),
    val waveformStyle: WaveformStyle = WaveformStyle.LINE,
    val refreshRate: MeterRefreshRate = MeterRefreshRate.STANDARD,
    val equivalentLevelDb: Float? = null,
    val equivalentLevelLabel: String = equivalentLevelLabelForWeighting(UserPreferenceDefaults.FREQUENCY_WEIGHTING),
    val dosimeter: DosimeterUiState = DosimeterUiState.LockedPreview,
    val sessionInfo: MeterSessionInfoUiState = MeterSessionInfoUiState(),
    val isProUser: Boolean = false,
    val dosimeterCardEnabled: Boolean = false,
    val sleepCardEnabled: Boolean = false,
    val measurementMode: MeasurementMode = MeasurementMode.DB_METER,
    val completedSessionId: Long? = null,
) {
    val canShare: Boolean = sampleCount > 0
}
