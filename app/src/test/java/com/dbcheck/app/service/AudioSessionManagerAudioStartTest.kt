package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSessionManagerAudioStartTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>()
    private val decibelReadings = MutableSharedFlow<DecibelReading>(replay = 1)
    private val audioEngine =
        mockk<AudioEngine>(relaxed = true) {
            every { decibelFlow } returns decibelReadings
        }
    private var userPreferencesFlow: Flow<UserPreferences> = MutableStateFlow(UserPreferences())
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val measurementRepository = mockk<MeasurementRepository>(relaxed = true)
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } answers { userPreferencesFlow }
        }
    private val healthConnectManager = mockk<HealthConnectManager>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkObject(DbCheckWidgetReceiver.Companion)
    }

    @Test
    fun failedAudioEngineStartDoesNotCreateRecordingSession() = runTest(dispatcher) {
        grantMicrophonePermission()
        coEvery { audioEngine.startRecording(any()) } returns
            AudioRecordingResult.Failed(AudioRecordingFailure.StartFailed)
        val manager = createManager()

        val started = manager.startSession()

        assertFalse(started)
        assertFalse(manager.isRecording.value)
        coVerify(exactly = 0) { sessionRepository.createSession(any()) }
    }

    @Test
    fun recordingStateIsPublishedOnlyAfterAudioEngineStarts() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = CompletableDeferred<Unit>()
        coEvery { sessionRepository.createSession(any()) } returns 42L
        coEvery { audioEngine.startRecording(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            releaseRecording.await()
            AudioRecordingResult.Stopped
        }
        val manager = createManager()

        val startResult = async { manager.startSession() }

        assertTrue(startResult.await())
        assertTrue(manager.isRecording.value)
        coVerify(exactly = 1) { sessionRepository.createSession(any()) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun startSessionWaitsForInitialRuntimeAudioPreferencesBeforeRecording() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releasePreferences = CompletableDeferred<Unit>()
        val releaseRecording = CompletableDeferred<Unit>()
        val startRecordingCalled = CompletableDeferred<Unit>()
        userPreferencesFlow =
            flow {
                releasePreferences.await()
                emit(
                    UserPreferences(
                        isProUser = true,
                        micSensitivityOffset = 4f,
                        frequencyWeighting = WeightingType.C.name,
                    ),
                )
                awaitCancellation()
            }
        coEvery { sessionRepository.createSession(any()) } returns 42L
        coEvery { audioEngine.startRecording(any()) } coAnswers {
            startRecordingCalled.complete(Unit)
            firstArg<suspend () -> Unit>().invoke()
            releaseRecording.await()
            AudioRecordingResult.Stopped
        }
        val manager = createManager()

        val startResult = async { manager.startSession() }

        assertFalse(startRecordingCalled.isCompleted)
        assertFalse(startResult.isCompleted)

        releasePreferences.complete(Unit)

        startRecordingCalled.await()
        assertTrue(startResult.await())
        coVerifyOrder {
            audioEngine.setWeighting(WeightingType.C)
            audioEngine.setCalibrationOffset(4f)
            audioEngine.startRecording(any())
        }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun refreshRateChangeDoesNotReapplyAudioWeighting() = runTest(dispatcher) {
        grantMicrophonePermission()
        val runtimePreferences = MutableStateFlow(UserPreferences())
        val releaseRecording = CompletableDeferred<Unit>()
        userPreferencesFlow = runtimePreferences
        coEvery { sessionRepository.createSession(any()) } returns 42L
        coEvery { audioEngine.startRecording(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            releaseRecording.await()
            AudioRecordingResult.Stopped
        }
        val manager = createManager()

        assertTrue(manager.startSession())

        runtimePreferences.value = UserPreferences(refreshRate = MeterRefreshRate.LOW)
        runCurrent()

        verify(exactly = 1) { audioEngine.setWeighting(WeightingType.A) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun replayedReadingFromPreviousSessionIsIgnoredOnNewSessionStart() = runTest(dispatcher) {
        grantMicrophonePermission()
        decibelReadings.emit(
            DecibelReading(
                instantDb = 70f,
                weightedDb = 70f,
                timestamp = 1_000L,
                peakAmplitude = 0.5f,
                peakDb = 75f,
            ),
        )
        val releaseRecording = CompletableDeferred<Unit>()
        coEvery { sessionRepository.createSession(any()) } returns 42L
        coEvery { audioEngine.startRecording(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            releaseRecording.await()
            AudioRecordingResult.Stopped
        }
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()

        assertEquals(0, manager.sessionStats.value.sampleCount)

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun recordingFailureStoppedByServiceDoesNotEmitCompletedSession() = runTest(dispatcher) {
        grantMicrophonePermission()
        val completedSessions = mutableListOf<Long>()
        val failureObserved = CompletableDeferred<Unit>()
        coEvery { sessionRepository.createSession(any()) } returns 42L
        coEvery { audioEngine.startRecording(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            AudioRecordingResult.Failed(AudioRecordingFailure.ReadFailed(-3))
        }
        val manager = createManager()
        val completedJob =
            launch {
                manager.completedSessionIds.collect { completedSessions += it }
            }
        val failureJob =
            launch {
                manager.recordingFailures.collect {
                    manager.stopSession(emitCompleted = false)
                    failureObserved.complete(Unit)
                }
            }

        assertTrue(manager.startSession())
        failureObserved.await()
        runCurrent()

        assertTrue(completedSessions.isEmpty())

        completedJob.cancel()
        failureJob.cancel()
    }

    @Test
    fun recoverInterruptedSessionCompletesActiveSessionFromPersistedMeasurements() = runTest(dispatcher) {
        mockWidgetUpdates()
        val manager = createManager()
        val activeSession =
            Session(
                id = 42L,
                startTime = 1_000L,
                endTime = null,
                minDb = 0f,
                avgDb = 0f,
                maxDb = 0f,
                peakDb = 0f,
                name = null,
                emoji = null,
                tags = emptyList(),
                isActive = true,
                frequencyWeighting = WeightingType.C.name,
            )
        every { sessionRepository.getActiveSession() } returns MutableStateFlow(activeSession)
        every { measurementRepository.getMeasurementsForSession(42L) } returns
            MutableStateFlow(
                listOf(
                    MeasurementEntity(
                        sessionId = 42L,
                        timestamp = 2_500L,
                        dbValue = 72f,
                        dbWeighted = 72f,
                    ),
                ),
            )

        manager.recoverInterruptedSession()

        coVerify {
            sessionRepository.completeSession(
                id = 42L,
                endTime = 2_500L,
                minDb = 72f,
                avgDb = 72f,
                maxDb = 72f,
                peakDb = 0f,
                frequencyWeighting = WeightingType.C.name,
            )
        }
    }

    private fun createManager() = AudioSessionManager(
        context = context,
        audioEngine = audioEngine,
        sessionRepository = sessionRepository,
        measurementRepository = measurementRepository,
        preferencesRepository = preferencesRepository,
        healthConnectManager = healthConnectManager,
        notificationHelper = notificationHelper,
        defaultDispatcher = dispatcher,
    )

    private fun grantMicrophonePermission() {
        mockkStatic(ContextCompat::class)
        mockWidgetUpdates()
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
    }

    private fun mockWidgetUpdates() {
        mockkObject(DbCheckWidgetReceiver.Companion)
        coEvery { DbCheckWidgetReceiver.updateAllWidgets(any()) } returns Unit
    }
}
