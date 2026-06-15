package com.dbcheck.app.ui.meter

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.service.LiveExposureState
import com.dbcheck.app.ui.meter.state.DosimeterUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeterViewModelDosimeterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness =
        MeterViewModelTestHarness(
            initialPreferences = UserPreferences(isProUser = true),
            relaxedAudioSessionManager = false,
        )

    @Test
    fun proUserReceivesLiveDosimeterDataFromExposureState() = runTest {
        val viewModel = createViewModel()

        harness.liveExposureState.value =
            LiveExposureState(
                standard = DosimeterStandard.OSHA_PEL,
                laeqDb = 95f,
                durationMs = 7_200_000L,
                twaDb = 85f,
                dosePercent = 50f,
                projectedDosePercent = 200f,
                remainingExposureMs = 7_200_000L,
                sampleCount = 12,
            )
        runCurrent()

        val dosimeter = viewModel.uiState.value.dosimeter as DosimeterUiState.Data
        assertEquals(DosimeterStandard.OSHA_PEL, dosimeter.standard)
        assertEquals(95f, dosimeter.laeqDb, 0.001f)
        assertEquals(85f, dosimeter.twaDb, 0.001f)
        assertEquals(50f, dosimeter.dosePercent, 0.001f)
        assertEquals(200f, dosimeter.projectedDosePercent, 0.001f)
        assertEquals(7_200_000L, dosimeter.remainingExposureMs)
        assertEquals(7_200_000L, dosimeter.durationMs)
        assertEquals(12, dosimeter.sampleCount)
    }

    @Test
    fun freeUserReceivesLockedDosimeterPreview() = runTest {
        val viewModel = createViewModel()

        harness.preferencesFlow.value = UserPreferences(isProUser = false)
        harness.liveExposureState.value =
            LiveExposureState(
                laeqDb = 90f,
                durationMs = 1_000L,
                sampleCount = 3,
            )
        runCurrent()

        assertSame(DosimeterUiState.LockedPreview, viewModel.uiState.value.dosimeter)
    }

    @Test
    fun proUserWithoutLiveExposureSamplesReceivesUnavailableDosimeterState() = runTest {
        val viewModel = createViewModel()

        harness.liveExposureState.value =
            LiveExposureState(
                standard = DosimeterStandard.NIOSH_REL,
                sampleCount = 0,
            )
        runCurrent()

        assertEquals(
            DosimeterUiState.Unavailable(standard = DosimeterStandard.NIOSH_REL),
            viewModel.uiState.value.dosimeter,
        )
    }

    private fun createViewModel(): MeterViewModel = harness.createViewModel()
}
