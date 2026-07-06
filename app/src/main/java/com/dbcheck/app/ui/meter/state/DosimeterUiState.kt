package com.dbcheck.app.ui.meter.state

import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.service.LiveExposureState

sealed interface DosimeterUiState {
    data object LockedPreview : DosimeterUiState

    data class Unavailable(val standard: DosimeterStandard) : DosimeterUiState

    data class Data(
        val standard: DosimeterStandard,
        val laeqDb: Float,
        val twaDb: Float,
        val dosePercent: Float,
        val projectedDosePercent: Float,
        val remainingExposureMs: Long?,
        val durationMs: Long,
        val sampleCount: Int,
    ) : DosimeterUiState
}

internal fun LiveExposureState.toDosimeterUiState(isProUser: Boolean): DosimeterUiState = when {
        !isProUser -> DosimeterUiState.LockedPreview

        sampleCount <= 0 -> DosimeterUiState.Unavailable(standard = standard)

        else ->
            DosimeterUiState.Data(
                standard = standard,
                laeqDb = laeqDb,
                twaDb = twaDb,
                dosePercent = dosePercent,
                projectedDosePercent = projectedDosePercent,
                remainingExposureMs = remainingExposureMs,
                durationMs = durationMs,
                sampleCount = sampleCount,
            )
}
