package com.dbcheck.app.ui.meter

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.audio.AudioInputInfo
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.WeightingType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeterViewModelSessionInfoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness =
        MeterViewModelTestHarness(
            initialPreferences =
                UserPreferences(
                    isProUser = true,
                    frequencyWeighting = WeightingType.C.name,
                    responseTime = ResponseTime.SLOW,
                ),
        )

    @Test
    fun proSessionInfoUsesEffectivePreferencesAndAudioInputInfo() = runTest {
        val viewModel = createViewModel()

        harness.audioInputInfo.value =
            AudioInputInfo(
                sampleRateHz = 48_000,
                inputDeviceName = "USB-C microphone",
            )
        runCurrent()

        val sessionInfo = viewModel.uiState.value.sessionInfo
        assertTrue(sessionInfo.showProDetails)
        assertEquals(WeightingType.C, sessionInfo.weighting)
        assertEquals(ResponseTime.SLOW, sessionInfo.responseTime)
        assertEquals(48_000, sessionInfo.sampleRateHz)
        assertEquals("USB-C microphone", sessionInfo.inputDeviceName)
    }

    @Test
    fun freeSessionInfoUsesEffectiveDefaultAudioPreferencesAndHidesProDetails() = runTest {
        val viewModel = createViewModel()

        harness.preferencesFlow.value =
            UserPreferences(
                isProUser = false,
                frequencyWeighting = WeightingType.C.name,
                responseTime = ResponseTime.SLOW,
            )
        runCurrent()

        val sessionInfo = viewModel.uiState.value.sessionInfo
        assertFalse(sessionInfo.showProDetails)
        assertEquals(WeightingType.A, sessionInfo.weighting)
        assertEquals(ResponseTime.FAST, sessionInfo.responseTime)
    }

    @Test
    fun sessionInfoDurationUsesActiveSessionStartTimeWhenViewModelReconnects() = runTest {
        val sessionStartTimeMs = System.currentTimeMillis() - 5_000L
        harness.activeSessionStartTimeMs.value = sessionStartTimeMs
        val viewModel = createViewModel()

        try {
            harness.isRecording.value = true
            runCurrent()

            val sessionInfo = viewModel.uiState.value.sessionInfo
            assertTrue(sessionInfo.isRecording)
            assertTrue(sessionInfo.durationMs >= 4_000L)
        } finally {
            harness.isRecording.value = false
            runCurrent()
        }
    }

    private fun createViewModel(): MeterViewModel = harness.createViewModel()
}
