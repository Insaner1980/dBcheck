package com.dbcheck.app.ui.hearing

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingRecoveryRepository
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.hearing.HearingHealthStatus
import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.domain.voice.VoiceBaselineCapture
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.testHearingRecoveryResult
import com.dbcheck.app.testHearingResult
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HearingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Test
    fun freeStateHidesProOnlyHearingDataAndVoiceBaseline() = runHearingTest(
        initialPreferences =
            UserPreferences(
                isProUser = false,
                tinnitusPitchProfile = SAVED_TINNITUS_PROFILE,
                sleepCardEnabled = true,
                soundDetectionEnabled = true,
                voiceBaselineLevelDb = 68.5f,
                voiceBaselineSampleCount = 7,
                voiceBaselineCapturedAtMs = CAPTURED_AT_MS,
            ),
        initialHearingTest = testHearingResult(),
    ) { harness, state ->
        harness.recording.value = true
        runCurrent()
        val updatedState = harness.viewModel.uiState.value

        assertFalse(updatedState.isProUser)
        assertEquals(HearingTestUiState.NoResult, updatedState.latestHearingTest)
        assertEquals(HearingRecoveryUiState.LockedPreview, updatedState.hearingRecovery)
        assertEquals(TinnitusPitchProfile(), updatedState.tinnitusPitchProfile)
        assertFalse(updatedState.sleepCardVisible)
        assertNull(updatedState.voiceBaselineLevelDb)
        assertEquals(0, updatedState.voiceBaselineSampleCount)
        assertNull(updatedState.voiceBaselineCapturedAtMs)
        assertFalse(updatedState.canCalibrateVoiceBaseline)
        assertFalse(state.isProUser)
    }

    @Test
    fun proStateExposesLatestHearingTestAndEffectivePreferences() = runHearingTest(
        initialPreferences =
            UserPreferences(
                isProUser = true,
                tinnitusPitchProfile = SAVED_TINNITUS_PROFILE,
                sleepCardEnabled = true,
                soundDetectionEnabled = true,
                voiceBaselineLevelDb = 68.5f,
                voiceBaselineSampleCount = 7,
                voiceBaselineCapturedAtMs = CAPTURED_AT_MS,
            ),
        initialHearingTest = testHearingResult(),
    ) { harness, state ->
        assertTrue(state.isProUser)
        assertEquals(
            HearingTestUiState.Result(
                id = 42L,
                timestamp = 1_700_000_000_000L,
                overallScore = 86,
                rating = "Good",
                avgThreshold = -27.5f,
            ),
            state.latestHearingTest,
        )
        assertEquals(SAVED_TINNITUS_PROFILE, state.tinnitusPitchProfile)
        assertTrue(state.sleepCardVisible)
        assertEquals(68.5f, state.voiceBaselineLevelDb ?: 0f, 0f)
        assertEquals(7, state.voiceBaselineSampleCount)
        assertEquals(CAPTURED_AT_MS, state.voiceBaselineCapturedAtMs)

        harness.recording.value = true
        runCurrent()
        assertTrue(harness.viewModel.uiState.value.canCalibrateVoiceBaseline)
    }

    @Test
    fun hearingHealthSummaryIsAbsentWithoutExposureSamples() = runHearingTest { _, state ->
        assertNull(state.hearingHealthSummary)
    }

    @Test
    fun hearingHealthSummaryUsesSharedCalculatorWhenExposureSamplesExist() = runHearingTest(
        initialDailyAverages =
            listOf(
                DailyExposureAverage(
                    dayStartMs = 1_700_000_000_000L,
                    avgDb = 85f,
                    maxDb = 90f,
                    sampleCount = 2,
                ),
            ),
    ) { _, state ->
        val summary = requireNotNull(state.hearingHealthSummary)

        assertEquals(85f, summary.weeklyAverageDb, 0f)
        assertEquals(HearingHealthStatus.DANGER, summary.healthStatus)
    }

    @Test
    fun proRecoveryRequiresAStoredBaseline() = runHearingTest { _, state ->
        assertEquals(HearingRecoveryUiState.MissingBaseline, state.hearingRecovery)
    }

    @Test
    fun proRecoveryIsReadyWhenBaselineExistsWithoutCurrentRecoveryResult() = runHearingTest(
        initialHearingTest = testHearingResult(),
    ) { _, state ->
        assertEquals(HearingRecoveryUiState.Ready, state.hearingRecovery)
    }

    @Test
    fun proRecoveryMapsCurrentRecoveryResult() = runHearingTest(
        initialHearingTest = testHearingResult(),
        initialRecoveryResult = testHearingRecoveryResult(),
    ) { _, state ->
        assertEquals(
            HearingRecoveryUiState.Result(
                averageShiftDb = 6f,
                maxShiftDb = 12f,
                status = HearingRecoveryStatus.SMALL_SHIFT,
                timestamp = 2_000L,
            ),
            state.hearingRecovery,
        )
    }

    @Test
    fun sleepCardRequiresBothPreferenceAndProEntitlement() = runHearingTest { harness, state ->
        assertFalse(state.sleepCardVisible)

        harness.preferences.value = UserPreferences(isProUser = true, sleepCardEnabled = true)
        runCurrent()
        assertTrue(harness.viewModel.uiState.value.sleepCardVisible)

        harness.preferences.value = UserPreferences(isProUser = false, sleepCardEnabled = true)
        runCurrent()
        assertFalse(harness.viewModel.uiState.value.sleepCardVisible)
    }

    @Test
    fun freeEntitlementBlocksVoiceBaselineCaptureIndependently() = runHearingTest(
        initialPreferences = UserPreferences(isProUser = false, soundDetectionEnabled = true),
        initialRecording = true,
    ) { harness, state ->
        assertFalse(state.canCalibrateVoiceBaseline)

        harness.viewModel.calibrateVoiceBaseline()
        runCurrent()

        verify(exactly = 0) { harness.audioSessionManager.captureVoiceBaseline(any()) }
        coVerify(exactly = 0) { harness.preferencesRepository.updateVoiceBaseline(any(), any(), any()) }
    }

    @Test
    fun inactiveMeasurementBlocksVoiceBaselineCaptureIndependently() = runHearingTest(
        initialPreferences = UserPreferences(isProUser = true, soundDetectionEnabled = true),
        initialRecording = false,
    ) { harness, state ->
        assertFalse(state.canCalibrateVoiceBaseline)

        harness.viewModel.calibrateVoiceBaseline()
        runCurrent()

        verify(exactly = 0) { harness.audioSessionManager.captureVoiceBaseline(any()) }
        coVerify(exactly = 0) { harness.preferencesRepository.updateVoiceBaseline(any(), any(), any()) }
    }

    @Test
    fun disabledSoundDetectionBlocksVoiceBaselineCaptureIndependently() = runHearingTest(
        initialPreferences = UserPreferences(isProUser = true, soundDetectionEnabled = false),
        initialRecording = true,
    ) { harness, state ->
        assertFalse(state.canCalibrateVoiceBaseline)

        harness.viewModel.calibrateVoiceBaseline()
        runCurrent()

        verify(exactly = 0) { harness.audioSessionManager.captureVoiceBaseline(any()) }
        coVerify(exactly = 0) { harness.preferencesRepository.updateVoiceBaseline(any(), any(), any()) }
    }

    @Test
    fun successfulVoiceBaselineCaptureIsPersistedThroughPreferencesRepository() = runHearingTest(
        initialPreferences = UserPreferences(isProUser = true, soundDetectionEnabled = true),
        initialRecording = true,
    ) { harness, state ->
        every { harness.audioSessionManager.captureVoiceBaseline(isProUser = true) } returns
            VoiceBaselineCapture(
                levelDb = 68.5f,
                sampleCount = 7,
                capturedAtMs = CAPTURED_AT_MS,
            )
        assertTrue(state.canCalibrateVoiceBaseline)

        harness.viewModel.calibrateVoiceBaseline()
        runCurrent()

        coVerify(exactly = 1) {
            harness.preferencesRepository.updateVoiceBaseline(
                levelDb = 68.5f,
                sampleCount = 7,
                capturedAtMs = CAPTURED_AT_MS,
            )
        }
    }

    @Test
    fun missingVoiceBaselineCaptureIsNotPersisted() = runHearingTest(
        initialPreferences = UserPreferences(isProUser = true, soundDetectionEnabled = true),
        initialRecording = true,
    ) { harness, state ->
        assertTrue(state.canCalibrateVoiceBaseline)

        harness.viewModel.calibrateVoiceBaseline()
        runCurrent()

        verify(exactly = 1) { harness.audioSessionManager.captureVoiceBaseline(isProUser = true) }
        coVerify(exactly = 0) { harness.preferencesRepository.updateVoiceBaseline(any(), any(), any()) }
    }

    private fun runHearingTest(
        initialPreferences: UserPreferences = UserPreferences(isProUser = true),
        initialDailyAverages: List<DailyExposureAverage> = emptyList(),
        initialHearingTest: HearingTestResult? = null,
        initialRecoveryResult: HearingRecoveryResult? = null,
        initialRecording: Boolean = false,
        assertions: suspend HearingTestScope.(HearingViewModelHarness, HearingUiState) -> Unit,
    ) = runTest(testDispatcher.scheduler) {
        val harness =
            HearingViewModelHarness(
                initialPreferences = initialPreferences,
                initialDailyAverages = initialDailyAverages,
                initialHearingTest = initialHearingTest,
                initialRecoveryResult = initialRecoveryResult,
                initialRecording = initialRecording,
            )
        try {
            runCurrent()
            assertions(HearingTestScope(this), harness, harness.viewModel.uiState.value)
        } finally {
            harness.viewModel.clearForTest()
            runCurrent()
        }
    }

    private class HearingTestScope(private val scope: kotlinx.coroutines.test.TestScope) {
        fun runCurrent() = scope.testScheduler.runCurrent()
    }

    private companion object {
        const val CAPTURED_AT_MS = 1_700_000_000_001L
        val SAVED_TINNITUS_PROFILE =
            TinnitusPitchProfile(
                leftFrequencyHz = 1_000f,
                rightFrequencyHz = 4_000f,
                updatedAtMs = 1_700_000_000_000L,
            )
    }
}

private class HearingViewModelHarness(
    initialPreferences: UserPreferences,
    initialDailyAverages: List<DailyExposureAverage>,
    initialHearingTest: HearingTestResult?,
    initialRecoveryResult: HearingRecoveryResult?,
    initialRecording: Boolean,
) {
    val preferences = MutableStateFlow(initialPreferences)
    val dailyAverages = MutableStateFlow(initialDailyAverages)
    val latestHearingTest = MutableStateFlow(initialHearingTest)
    val latestRecoveryResult = MutableStateFlow(initialRecoveryResult)
    val recording = MutableStateFlow(initialRecording)
    val preferencesRepository =
        mockk<PreferencesRepository>(relaxed = true) {
            every { userPreferences } returns preferences
        }
    private val measurementRepository =
        mockk<MeasurementRepository> {
            every { getDailyAveragesLast7Days() } returns dailyAverages
        }
    private val hearingTestRepository =
        mockk<HearingTestRepository> {
            every { getLatestResult() } returns latestHearingTest
        }
    private val hearingRecoveryRepository =
        mockk<HearingRecoveryRepository> {
            every { getLatestResult() } returns latestRecoveryResult
        }
    val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns recording
            every { captureVoiceBaseline(isProUser = true) } returns null
        }
    val viewModel =
        HearingViewModel(
            preferencesRepository = preferencesRepository,
            measurementRepository = measurementRepository,
            hearingTestRepository = hearingTestRepository,
            hearingRecoveryRepository = hearingRecoveryRepository,
            audioSessionManager = audioSessionManager,
        )
}
