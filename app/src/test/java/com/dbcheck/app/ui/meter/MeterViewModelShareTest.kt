package com.dbcheck.app.ui.meter

import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import app.cash.turbine.test
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.noise.SoundReferenceCatalog
import com.dbcheck.app.domain.noise.SoundReferenceId
import com.dbcheck.app.service.SessionStats
import com.dbcheck.app.ui.meter.state.LiveChartPointUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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

    private val harness =
        MeterViewModelTestHarness(
            initialPreferences =
                UserPreferences(
                    waveformStyle = WaveformStyle.BARS,
                    refreshRate = MeterRefreshRate.LOW,
                ),
            relaxedAudioSessionManager = false,
        )

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun shareBeforeMeasurementSampleReturnsNullAndShowsError() = runTest {
            val viewModel = createViewModel()

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                expectNoEvents()
            }
            assertEquals("Start measuring before sharing results", viewModel.uiState.value.error)
            coVerify(exactly = 0) {
                harness.shareResultsGenerator.shareSessionStats(any(), any(), any(), any())
            }
        }

    @Test
    fun shareWithMeasurementStatsUsesCurrentAveragePeakAndDuration() = runTest {
            val intent = Intent(Intent.ACTION_SEND)
            coEvery {
                harness.shareResultsGenerator.shareSessionStats(
                    avgDb = 72.4f,
                    peakDb = 91.2f,
                    durationMs = 0L,
                    equivalentLevelLabel = "LCeq",
                )
            } returns intent
            val viewModel = createViewModel()
            harness.preferencesFlow.value =
                harness.preferencesFlow.value.copy(
                    isProUser = true,
                    frequencyWeighting = "C",
                )
            runCurrent()
            harness.sessionStats.value =
                SessionStats(
                    minDb = 55.1f,
                    avgDb = 72.4f,
                    maxDb = 88.6f,
                    peakDb = 91.2f,
                    sampleCount = 4,
                )

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                assertSame(intent, awaitItem())
            }
            assertNull(viewModel.uiState.value.error)
            coVerify(exactly = 1) {
                harness.shareResultsGenerator.shareSessionStats(
                    avgDb = 72.4f,
                    peakDb = 91.2f,
                    durationMs = 0L,
                    equivalentLevelLabel = "LCeq",
                )
            }
        }

    @Test
    fun shareWhileRecordingUsesElapsedDurationAtShareTime() = runTest {
            val intent = Intent(Intent.ACTION_SEND)
            val capturedDurationMs = stubShareIntentCapturingDuration(intent)
            val viewModel = createViewModel()

            try {
                harness.isRecording.value = true
                runCurrent()
                Thread.sleep(50L)
                harness.sessionStats.value =
                    SessionStats(
                        avgDb = 72.4f,
                        peakDb = 91.2f,
                        sampleCount = 1,
                    )
                runCurrent()

                viewModel.shareIntents.test {
                    viewModel.createShareIntent()
                    assertSame(intent, awaitItem())
                }

                coVerify(exactly = 1) {
                harness.shareResultsGenerator.shareSessionStats(
                    avgDb = 72.4f,
                    peakDb = 91.2f,
                    durationMs = any(),
                    equivalentLevelLabel = "LAeq",
                )
            }
                assertTrue(capturedDurationMs() > 0L)
            } finally {
                harness.isRecording.value = false
                runCurrent()
            }
        }

    @Test
    fun shareAfterRecordingStopsUsesFinalElapsedDuration() = runTest {
            val intent = Intent(Intent.ACTION_SEND)
            val capturedDurationMs = stubShareIntentCapturingDuration(intent)
            val viewModel = createViewModel()

            harness.isRecording.value = true
            runCurrent()
            Thread.sleep(50L)
            harness.isRecording.value = false
            harness.sessionStats.value =
                SessionStats(
                    avgDb = 70.1f,
                    peakDb = 89.8f,
                    sampleCount = 2,
                )
            runCurrent()

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                assertSame(intent, awaitItem())
            }

            assertTrue(capturedDurationMs() > 0L)
        }

    @Test
    fun shareGeneratorFailureReturnsNullAndShowsError() = runTest {
            coEvery { harness.shareResultsGenerator.shareSessionStats(any(), any(), any(), any()) } throws
                IllegalStateException("Disk full")
            val viewModel = createViewModel()
            harness.sessionStats.value = SessionStats(avgDb = 70f, peakDb = 90f, sampleCount = 2)

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                advanceUntilIdle()
                expectNoEvents()
            }
            assertEquals("Unable to share meter results", viewModel.uiState.value.error)
        }

    @Test
    fun healthConnectSyncFailureShowsMeterError() = runTest {
            val viewModel = createViewModel()

            harness.healthConnectSyncFailures.emit("Health Connect write failed")

            assertEquals("Health Connect write failed", viewModel.uiState.value.error)
        }

    @Test
    fun audioRecordingFailureShowsMeterError() = runTest {
            val viewModel = createViewModel()

            harness.recordingFailures.emit(AudioRecordingFailure.StartFailed)

            assertEquals("Unable to start measurement", viewModel.uiState.value.error)
        }

    @Test
    fun decibelReadingsUpdateUiAtConfiguredRefreshRate() = runTest {
            val viewModel = createViewModel()

            harness.decibelReadings.emit(reading(timestamp = 1_000L, db = 60f))
            harness.decibelReadings.emit(reading(timestamp = 1_500L, db = 70f))

            assertEquals(60f, viewModel.uiState.value.currentDb)
            assertEquals(1, viewModel.uiState.value.waveformData.size)
            assertEquals(WaveformStyle.BARS, viewModel.uiState.value.waveformStyle)

            harness.decibelReadings.emit(reading(timestamp = 2_000L, db = 70f))

            assertEquals(70f, viewModel.uiState.value.currentDb)
            assertEquals(2, viewModel.uiState.value.waveformData.size)
        }

    @Test
    fun toggleRecordingRechecksActualMicrophonePermissionBeforeStartingSession() = runTest {
            mockkStatic(ContextCompat::class)
            every { ContextCompat.checkSelfPermission(any(), any()) } returns
                PackageManager.PERMISSION_DENIED
            val viewModel = createViewModel()
            viewModel.onMicPermissionResult(granted = true)

            viewModel.toggleRecording()

            assertFalse(viewModel.uiState.value.isRecording)
            assertFalse(viewModel.uiState.value.isMicPermissionGranted)
            assertTrue(viewModel.uiState.value.showMicDeniedPrompt)
            coVerify(exactly = 0) { harness.audioSessionManager.startSession() }
        }

    @Test
    fun grantedMicrophonePermissionClearsDeniedPromptAfterSettingsReturn() = runTest {
            val viewModel = createViewModel()
            viewModel.onMicPermissionDenied()

            viewModel.onMicPermissionResult(granted = true)

            assertTrue(viewModel.uiState.value.isMicPermissionGranted)
            assertFalse(viewModel.uiState.value.showMicDeniedPrompt)
        }

    @Test
    fun notificationPermissionRequestStateSurvivesMeterReset() = runTest {
            val viewModel = createViewModel()
            every { harness.audioSessionManager.resetStats() } returns Unit

            viewModel.onNotificationPermissionRequested()
            viewModel.resetMeasurement()

            assertTrue(viewModel.uiState.value.notificationPermissionAlreadyRequested)
        }

    @Test
    fun stopServiceFailureKeepsRecordingStateAndShowsError() = runTest {
            every { harness.context.startService(any()) } throws IllegalStateException("service unavailable")
            val viewModel = createViewModel()

            try {
                harness.isRecording.value = true
                runCurrent()

                viewModel.toggleRecording()

                assertTrue(viewModel.uiState.value.isRecording)
                assertEquals("Unable to stop measurement", viewModel.uiState.value.error)
            } finally {
                harness.isRecording.value = false
                runCurrent()
            }
        }

    @Test
    fun resetDuringStopServiceFailureDoesNotClearMeasurementState() = runTest {
            every { harness.context.startService(any()) } throws IllegalStateException("service unavailable")
            val viewModel = createViewModel()

            try {
                harness.isRecording.value = true
                harness.sessionStats.value = SessionStats(avgDb = 70f, peakDb = 90f, sampleCount = 2)
                runCurrent()

                viewModel.resetMeasurement()

                assertTrue(viewModel.uiState.value.isRecording)
                assertEquals(70f, viewModel.uiState.value.avgDb)
                assertEquals("Unable to stop measurement", viewModel.uiState.value.error)
                verify(exactly = 0) { harness.audioSessionManager.resetStats() }
            } finally {
                harness.isRecording.value = false
                runCurrent()
            }
        }

    @Test
    fun liveChartBufferUpdatesOnlyWhileRecordingAndResetClearsIt() = runTest {
            val viewModel = createViewModel()
            every { harness.audioSessionManager.resetStats() } returns Unit

            harness.isRecording.value = true
            runCurrent()
            harness.decibelReadings.emit(reading(timestamp = 1_000L, db = 60f))

            assertEquals(
                listOf(LiveChartPointUiState(timestampMs = 1_000L, db = 60f)),
                viewModel.uiState.value.liveChartPoints,
            )

            harness.isRecording.value = false
            runCurrent()
            harness.decibelReadings.emit(reading(timestamp = 2_000L, db = 70f))

            assertEquals(
                listOf(LiveChartPointUiState(timestampMs = 1_000L, db = 60f)),
                viewModel.uiState.value.liveChartPoints,
            )

            viewModel.resetMeasurement()

            assertTrue(viewModel.uiState.value.liveChartPoints.isEmpty())
        }

    @Test
    fun decibelReadingsPublishSoundReferenceStateForUi() = runTest {
            val viewModel = createViewModel()

            harness.decibelReadings.emit(reading(timestamp = 1_000L, db = 67f))

            assertEquals(SoundReferenceCatalog.referenceMarkers, viewModel.uiState.value.soundReferenceMarkers)
            assertEquals(
                SoundReferenceId.CONVERSATION,
                viewModel.uiState.value.nearestSoundReferenceMarker.reference.id,
            )
            assertEquals(67f / 130f, viewModel.uiState.value.soundReferenceCurrentPosition, 0.001f)
        }

    private fun createViewModel(): MeterViewModel = harness.createViewModel()

    private fun stubShareIntentCapturingDuration(intent: Intent): () -> Long {
        var capturedDurationMs = 0L
        coEvery { harness.shareResultsGenerator.shareSessionStats(any(), any(), any(), any()) } answers {
            capturedDurationMs = thirdArg()
            intent
        }
        return { capturedDurationMs }
    }

    private fun reading(timestamp: Long, db: Float) = DecibelReading(
        instantDb = db,
        weightedDb = db,
        timestamp = timestamp,
        peakAmplitude = db / 100f,
    )
}
