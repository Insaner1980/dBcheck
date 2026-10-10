package com.dbcheck.app.ui.meter

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.service.LiveExposureState
import com.dbcheck.app.service.SessionStats
import com.dbcheck.app.ui.meter.state.DosimeterUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeterViewModelRefreshRateTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun lowRefreshBoundsStatisticsAndExposureAndFlushesOnStop() = verify(MeterRefreshRate.LOW, 1)

    @Test
    fun standardRefreshBoundsStatisticsAndExposureAndFlushesOnStop() = verify(MeterRefreshRate.STANDARD, 1)

    @Test
    fun highRefreshAllowsTheNextUpdateAt100Milliseconds() = verify(MeterRefreshRate.HIGH, 2)

    private fun verify(rate: MeterRefreshRate, expectedSamples: Int) = runTest {
        val harness = MeterViewModelTestHarness(UserPreferences(isProUser = true, refreshRate = rate))
        val viewModel = harness.createViewModel()
        harness.isRecording.value = true
        runCurrent()
        try {
            harness.sessionStats.value = SessionStats(sampleCount = 1, avgDb = 70f)
            harness.liveExposureState.value = LiveExposureState(sampleCount = 1, dosePercent = 1f)
            runCurrent()
            harness.decibelReadings.emit(reading(1_000L, 70f))
            runCurrent()
            harness.sessionStats.value = SessionStats(sampleCount = 2, avgDb = 80f)
            harness.liveExposureState.value = LiveExposureState(sampleCount = 2, dosePercent = 2f)
            runCurrent()
            harness.decibelReadings.emit(reading(1_100L, 90f))
            runCurrent()
            assertEquals(expectedSamples, viewModel.uiState.value.sampleCount)
            assertEquals(if (expectedSamples == 1) 70f else 80f, viewModel.uiState.value.avgDb)
            assertEquals(
                expectedSamples.toFloat(),
                (viewModel.uiState.value.dosimeter as DosimeterUiState.Data).dosePercent,
            )
            assertEquals(2, harness.sessionStats.value.sampleCount)
            harness.isRecording.value = false
            runCurrent()
            assertEquals(2, viewModel.uiState.value.sampleCount)
            assertEquals(80f, viewModel.uiState.value.avgDb)
            assertEquals(2f, (viewModel.uiState.value.dosimeter as DosimeterUiState.Data).dosePercent)
        } finally {
            harness.isRecording.value = false
            runCurrent()
        }
    }

    private fun reading(time: Long, level: Float) = DecibelReading(
        instantDb = level,
        weightedDb = level,
        timestamp = time,
        peakAmplitude = 0.5f,
        peakDb = level,
    )
}
