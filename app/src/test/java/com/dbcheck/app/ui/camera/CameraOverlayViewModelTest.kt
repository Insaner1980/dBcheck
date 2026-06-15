package com.dbcheck.app.ui.camera

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.service.AudioSessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraOverlayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val decibelReadings = MutableSharedFlow<DecibelReading>(replay = 1)
    private val isRecording = MutableStateFlow(false)
    private val activeSessionStartTimeMs = MutableStateFlow<Long?>(null)
    private val preferences =
        MutableStateFlow(
            UserPreferences(
                isProUser = true,
                frequencyWeighting = WeightingType.C.name,
            ),
        )
    private val audioEngine =
        mockk<AudioEngine> {
            every { decibelFlow } returns decibelReadings
        }
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every {
                this@mockk.isRecording
            } returns this@CameraOverlayViewModelTest.isRecording
            every { this@mockk.activeSessionStartTimeMs } returns
                this@CameraOverlayViewModelTest.activeSessionStartTimeMs
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }
    private val shareGenerator = mockk<CameraOverlayShareGenerator>(relaxed = true)

    @Test
    fun idleStateDoesNotShowReplayedReadingWhenNotRecording() = runTest {
        decibelReadings.emit(reading(weightedDb = 81.2f, timestamp = 1_000L))

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(CameraOverlayReadoutStatus.READY, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.currentDb)
        assertNull(viewModel.uiState.value.timestampMs)
        viewModel.clearForTest()
    }

    @Test
    fun activeRecordingShowsLatestWeightedDbLabelAndTimestamp() = runTest {
        isRecording.value = true
        activeSessionStartTimeMs.value = 1_000L
        val viewModel = createViewModel()

        decibelReadings.emit(reading(weightedDb = 72.4f, timestamp = 1_500L))
        runCurrent()

        assertEquals(CameraOverlayReadoutStatus.LIVE, viewModel.uiState.value.status)
        assertEquals(72.4f, viewModel.uiState.value.currentDb ?: 0f, 0.001f)
        assertEquals("LCeq", viewModel.uiState.value.levelLabel)
        assertEquals(1_500L, viewModel.uiState.value.timestampMs)
        viewModel.clearForTest()
    }

    @Test
    fun ignoresReadingOlderThanActiveSessionStart() = runTest {
        isRecording.value = true
        activeSessionStartTimeMs.value = 2_000L
        val viewModel = createViewModel()

        decibelReadings.emit(reading(weightedDb = 75f, timestamp = 1_900L))
        runCurrent()

        assertNull(viewModel.uiState.value.currentDb)

        decibelReadings.emit(reading(weightedDb = 76f, timestamp = 2_100L))
        runCurrent()

        assertEquals(76f, viewModel.uiState.value.currentDb ?: 0f, 0.001f)
        assertEquals(2_100L, viewModel.uiState.value.timestampMs)
        viewModel.clearForTest()
    }

    @Test
    fun stoppingRecordingClearsLiveReading() = runTest {
        isRecording.value = true
        activeSessionStartTimeMs.value = 1_000L
        val viewModel = createViewModel()
        decibelReadings.emit(reading(weightedDb = 78f, timestamp = 1_500L))
        runCurrent()

        isRecording.value = false
        runCurrent()

        assertEquals(CameraOverlayReadoutStatus.READY, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.currentDb)
        assertNull(viewModel.uiState.value.timestampMs)
        viewModel.clearForTest()
    }

    @Test
    fun capturedPhotoCreatesBurnedInShareIntentFromCurrentReadout() = runTest {
        isRecording.value = true
        activeSessionStartTimeMs.value = 1_000L
        val viewModel = createViewModel()
        decibelReadings.emit(reading(weightedDb = 73.6f, timestamp = 1_500L))
        runCurrent()
        val captureFile = mockk<java.io.File>()
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
        val readoutSlot = slot<CameraOverlayUiState>()
        coEvery { shareGenerator.createPhotoShareIntent(any(), capture(readoutSlot)) } returns shareIntent
        val emittedIntent = async { viewModel.photoShareIntents.first() }
        runCurrent()

        viewModel.onPhotoCaptureStarted()
        assertEquals(true, viewModel.uiState.value.isCapturingPhoto)
        viewModel.onPhotoCaptured(captureFile)
        runCurrent()

        assertEquals(shareIntent, emittedIntent.await())
        assertEquals(false, viewModel.uiState.value.isCapturingPhoto)
        assertEquals(73.6f, readoutSlot.captured.currentDb ?: 0f, 0.001f)
        assertEquals(CameraOverlayReadoutStatus.LIVE, readoutSlot.captured.status)
        assertEquals(1_500L, readoutSlot.captured.timestampMs)
        coVerify { shareGenerator.createPhotoShareIntent(captureFile, any()) }
        viewModel.clearForTest()
    }

    @Test
    fun silentVideoRecordingStateStartsStopsAndReportsFailures() = runTest {
        val viewModel = createViewModel()

        viewModel.onVideoRecordingStarted()

        assertEquals(true, viewModel.uiState.value.isRecordingVideo)
        assertEquals(false, viewModel.uiState.value.videoCaptureFailed)

        viewModel.onVideoRecordingFinished()

        assertEquals(false, viewModel.uiState.value.isRecordingVideo)
        assertEquals(false, viewModel.uiState.value.videoCaptureFailed)

        viewModel.onVideoRecordingStarted()
        viewModel.onVideoRecordingFailed()

        assertEquals(false, viewModel.uiState.value.isRecordingVideo)
        assertEquals(true, viewModel.uiState.value.videoCaptureFailed)
        viewModel.clearForTest()
    }

    @Test
    fun createsSilentVideoFileThroughCameraOverlayGenerator() = runTest {
        val viewModel = createViewModel()
        val videoFile = mockk<java.io.File>()
        every { shareGenerator.createSilentVideoFile(any()) } returns videoFile

        assertEquals(videoFile, viewModel.createSilentVideoFile())
        viewModel.clearForTest()
    }

    private fun createViewModel(): CameraOverlayViewModel = CameraOverlayViewModel(
            audioEngine = audioEngine,
            audioSessionManager = audioSessionManager,
            preferencesRepository = preferencesRepository,
            shareGenerator = shareGenerator,
        )

    private fun reading(weightedDb: Float, timestamp: Long): DecibelReading = DecibelReading(
            instantDb = weightedDb,
            weightedDb = weightedDb,
            aWeightedDb = weightedDb,
            timestamp = timestamp,
            peakAmplitude = 0.5f,
            peakDb = weightedDb,
        )
}
