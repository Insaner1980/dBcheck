package com.dbcheck.app.ui.meter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import app.cash.turbine.test
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.SessionStats
import com.dbcheck.app.util.HapticFeedbackHelper
import com.dbcheck.app.util.ShareResultsGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeterViewModelShareTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val decibelReadings = MutableSharedFlow<DecibelReading>()
    private val sessionStats = MutableStateFlow(SessionStats())
    private val completedSessions = MutableSharedFlow<Long>()
    private val isRecording = MutableStateFlow(false)
    private val preferencesFlow =
        MutableStateFlow(
            UserPreferences(
                waveformStyle = WaveformStyle.BARS,
                refreshRate = MeterRefreshRate.LOW,
            ),
        )
    private val context = mockk<Context>(relaxed = true)
    private val audioEngine =
        mockk<AudioEngine> {
            every { decibelFlow } returns decibelReadings
        }
    private val audioSessionManager = mockk<AudioSessionManager>()
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }
    private val hapticHelper = mockk<HapticFeedbackHelper>(relaxed = true)
    private val shareResultsGenerator = mockk<ShareResultsGenerator>()

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun shareBeforeMeasurementSampleReturnsNullAndShowsError() =
        runTest {
            val viewModel = createViewModel()

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                expectNoEvents()
            }
            assertEquals("Start measuring before sharing results", viewModel.uiState.value.error)
            coVerify(exactly = 0) {
                shareResultsGenerator.shareSessionStats(any(), any(), any())
            }
        }

    @Test
    fun shareWithMeasurementStatsUsesCurrentAveragePeakAndDuration() =
        runTest {
            val intent = Intent(Intent.ACTION_SEND)
            coEvery {
                shareResultsGenerator.shareSessionStats(
                    avgDb = 72.4f,
                    peakDb = 91.2f,
                    durationMs = 0L,
                )
            } returns intent
            val viewModel = createViewModel()
            sessionStats.value =
                SessionStats(
                    minDb = 55.1f,
                    avgDb = 72.4f,
                    maxDb = 88.6f,
                    peakDb = 91.2f,
                    sampleCount = 4,
                    totalDb = 289.6,
                )

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                assertSame(intent, awaitItem())
            }
            assertNull(viewModel.uiState.value.error)
            coVerify(exactly = 1) {
                shareResultsGenerator.shareSessionStats(
                    avgDb = 72.4f,
                    peakDb = 91.2f,
                    durationMs = 0L,
                )
            }
        }

    @Test
    fun shareGeneratorFailureReturnsNullAndShowsError() =
        runTest {
            coEvery { shareResultsGenerator.shareSessionStats(any(), any(), any()) } throws
                IllegalStateException("Disk full")
            val viewModel = createViewModel()
            sessionStats.value = SessionStats(avgDb = 70f, peakDb = 90f, sampleCount = 2)

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                advanceUntilIdle()
                expectNoEvents()
            }
            assertEquals("Unable to share meter results", viewModel.uiState.value.error)
        }

    @Test
    fun decibelReadingsUpdateUiAtConfiguredRefreshRate() =
        runTest {
            val viewModel = createViewModel()

            decibelReadings.emit(reading(timestamp = 1_000L, db = 60f))
            decibelReadings.emit(reading(timestamp = 1_500L, db = 70f))

            assertEquals(60f, viewModel.uiState.value.currentDb)
            assertEquals(1, viewModel.uiState.value.waveformData.size)
            assertEquals(WaveformStyle.BARS, viewModel.uiState.value.waveformStyle)

            decibelReadings.emit(reading(timestamp = 2_000L, db = 70f))

            assertEquals(70f, viewModel.uiState.value.currentDb)
            assertEquals(2, viewModel.uiState.value.waveformData.size)
        }

    @Test
    fun toggleRecordingRechecksActualMicrophonePermissionBeforeStartingSession() = runTest {
            mockkStatic(ContextCompat::class)
            every { ContextCompat.checkSelfPermission(any(), any()) } returns
                PackageManager.PERMISSION_DENIED
            every { audioSessionManager.startSession() } returns true
            val viewModel = createViewModel()
            viewModel.onMicPermissionResult(granted = true)

            viewModel.toggleRecording()

            assertFalse(viewModel.uiState.value.isRecording)
            assertFalse(viewModel.uiState.value.isMicPermissionGranted)
            assertTrue(viewModel.uiState.value.showMicDeniedPrompt)
            verify(exactly = 0) { audioSessionManager.startSession() }
        }

    @Test
    fun grantedMicrophonePermissionClearsDeniedPromptAfterSettingsReturn() = runTest {
            val viewModel = createViewModel()
            viewModel.onMicPermissionDenied()

            viewModel.onMicPermissionResult(granted = true)

            assertTrue(viewModel.uiState.value.isMicPermissionGranted)
            assertFalse(viewModel.uiState.value.showMicDeniedPrompt)
        }

    private fun createViewModel(): MeterViewModel {
        every { audioSessionManager.sessionStats } returns sessionStats
        every { audioSessionManager.completedSessionIds } returns completedSessions
        every { audioSessionManager.isRecording } returns isRecording
        return MeterViewModel(
            context = context,
            audioEngine = audioEngine,
            audioSessionManager = audioSessionManager,
            preferencesRepository = preferencesRepository,
            hapticHelper = hapticHelper,
            shareResultsGenerator = shareResultsGenerator,
        )
    }

    private fun reading(
        timestamp: Long,
        db: Float,
    ) = DecibelReading(
        instantDb = db,
        weightedDb = db,
        timestamp = timestamp,
        peakAmplitude = db / 100f,
    )
}
