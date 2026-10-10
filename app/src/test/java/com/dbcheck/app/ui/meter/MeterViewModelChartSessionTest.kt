package com.dbcheck.app.ui.meter

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.domain.audio.DecibelReading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeterViewModelChartSessionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun nextSessionDoesNotRetainPreviousSessionChartPoints() = runTest {
        val harness = MeterViewModelTestHarness()
        val viewModel = harness.createViewModel()
        harness.isRecording.value = true
        harness.decibelReadings.emit(reading(1_000L, 70f))
        runCurrent()
        assertEquals(1, viewModel.uiState.value.liveChartPoints.size)
        harness.isRecording.value = false
        runCurrent()
        harness.isRecording.value = true
        runCurrent()
        assertTrue(viewModel.uiState.value.liveChartPoints.isEmpty())
        harness.decibelReadings.emit(reading(1_100L, 80f))
        runCurrent()
        assertEquals(listOf(80f), viewModel.uiState.value.liveChartPoints.map { it.db })
        harness.isRecording.value = false
    }

    private fun reading(time: Long, level: Float) = DecibelReading(
        instantDb = level,
        weightedDb = level,
        timestamp = time,
        peakAmplitude = 0.5f,
        peakDb = level,
    )
}
