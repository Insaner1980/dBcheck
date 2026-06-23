package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionMeasurementSummary
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.data.repository.SoundDetectionRepository
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.SoundClassification
import com.dbcheck.app.domain.audio.SoundClassifier
import com.dbcheck.app.domain.audio.SoundDetectionError
import com.dbcheck.app.domain.audio.SoundDetectionEvent
import com.dbcheck.app.domain.audio.SoundDetectionState
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionLocationMetadata
import com.dbcheck.app.domain.session.SessionMeasurement
import com.dbcheck.app.sync.HealthConnectAvailability
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectPermissions
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.sync.HealthConnectSyncResult
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class AudioSessionManagerAudioStartTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>()
    private val decibelReadings = MutableSharedFlow<DecibelReading>(replay = 1)
    private val soundDetectionWindows = MutableSharedFlow<FloatArray>(extraBufferCapacity = 1)
    private val audioEngine =
        mockk<AudioEngine>(relaxed = true) {
            every { decibelFlow } returns decibelReadings
            every { soundDetectionWindows } returns this@AudioSessionManagerAudioStartTest.soundDetectionWindows
        }
    private val soundClassifier = mockk<SoundClassifier>(relaxed = true)
    private var userPreferencesFlow: Flow<UserPreferences> = MutableStateFlow(UserPreferences())
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val measurementRepository = mockk<MeasurementRepository>(relaxed = true)
    private val soundDetectionRepository = mockk<SoundDetectionRepository>(relaxed = true)
    private val wavRecordingFile = File("build/test-wav/session.wav")
    private val wavRecordingFileStore =
        mockk<WavRecordingFileStore>(relaxed = true) {
            every { createRecordingFile(any(), any()) } returns wavRecordingFile
        }
    private val sessionLocationCapturePort = FakeSessionLocationCapturePort()
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
        coVerify(exactly = 0) { sessionRepository.createActiveSession(any(), any()) }
    }

    @Test
    fun reportedForegroundStartFailurePublishesRecordingFailure() = runTest(dispatcher) {
        val manager = createManager()
        val failures = mutableListOf<AudioRecordingFailure>()
        val failureJob =
            launch {
                manager.recordingFailures.collect { failures += it }
            }

        manager.reportRecordingFailure(AudioRecordingFailure.StartFailed)
        runCurrent()

        assertEquals(listOf(AudioRecordingFailure.StartFailed), failures)
        assertFalse(manager.isRecording.value)

        failureJob.cancel()
    }

    @Test
    fun recordingStateIsPublishedOnlyAfterAudioEngineStarts() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        val startResult = async { manager.startSession() }

        assertTrue(startResult.await())
        assertTrue(manager.isRecording.value)
        coVerify(exactly = 1) { sessionRepository.createActiveSession(any(), any()) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun activeSessionStartTimeIsPublishedWhileRecordingAndClearedOnStop() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertNull(manager.activeSessionStartTimeMs.value)

        assertTrue(manager.startSession())

        assertNotNull(manager.activeSessionStartTimeMs.value)

        manager.stopSession()
        releaseRecording.complete(Unit)

        assertNull(manager.activeSessionStartTimeMs.value)
    }

    @Test
    fun startSessionPersistsCapturedSessionLocation() = runTest(dispatcher) {
        grantMicrophonePermission()
        sessionLocationCapturePort.enqueue(sessionLocation(capturedAt = START_LOCATION_CAPTURED_AT))
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()

        coVerify(exactly = 1) {
            sessionRepository.updateSessionLocation(
                id = DEFAULT_SESSION_ID,
                location = sessionLocation(capturedAt = START_LOCATION_CAPTURED_AT),
            )
        }

        manager.stopSession()
        runCurrent()

        assertEquals(1, sessionLocationCapturePort.captureCount)

        releaseRecording.complete(Unit)
    }

    @Test
    fun stopSessionCapturesLocationWhenStartLocationIsUnavailable() = runTest(dispatcher) {
        grantMicrophonePermission()
        sessionLocationCapturePort.enqueue(null, sessionLocation(capturedAt = STOP_LOCATION_CAPTURED_AT))
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()

        coVerify(exactly = 0) {
            sessionRepository.updateSessionLocation(id = any(), location = any())
        }

        manager.stopSession()
        runCurrent()

        coVerify(exactly = 1) {
            sessionRepository.updateSessionLocation(
                id = DEFAULT_SESSION_ID,
                location = sessionLocation(capturedAt = STOP_LOCATION_CAPTURED_AT),
            )
        }
        assertEquals(2, sessionLocationCapturePort.captureCount)

        releaseRecording.complete(Unit)
    }

    @Test
    fun missingLocationDoesNotFailSessionStartOrCompletion() = runTest(dispatcher) {
        grantMicrophonePermission()
        sessionLocationCapturePort.enqueue(null, null)
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        assertTrue(manager.isRecording.value)

        manager.stopSession()
        runCurrent()

        coVerify(exactly = 0) {
            sessionRepository.updateSessionLocation(id = any(), location = any())
        }
        coVerify {
            sessionRepository.completeSessionWithMeasurements(
                id = DEFAULT_SESSION_ID,
                endTime = any(),
                measurements = emptyList(),
                summary = sessionMeasurementSummary(
                    minDb = 0f,
                    avgDb = 0f,
                    maxDb = 0f,
                    peakDb = 0f,
                    frequencyWeighting = WeightingType.A,
                ),
            )
        }

        releaseRecording.complete(Unit)
    }

    @Test
    fun startSessionWaitsForInitialRuntimeAudioPreferencesBeforeRecording() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releasePreferences = CompletableDeferred<Unit>()
        val startRecordingCalled = CompletableDeferred<Unit>()
        userPreferencesFlow =
            flow {
                releasePreferences.await()
                emit(
                    UserPreferences(
                        isProUser = true,
                        micSensitivityOffset = 4f,
                        frequencyWeighting = WeightingType.C.name,
                        responseTime = ResponseTime.SLOW,
                    ),
                )
                awaitCancellation()
            }
        val releaseRecording = stubStartedRecordingSession(
            onStartRecording = { startRecordingCalled.complete(Unit) },
        )
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
            audioEngine.setResponseTime(ResponseTime.SLOW)
            audioEngine.startRecording(any())
        }
        coVerify {
            sessionRepository.createActiveSession(
                startTime = any(),
                frequencyWeighting = WeightingType.C.name,
            )
        }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun freeUserResponseTimeResolvesToFastBeforeRecording() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow =
            MutableStateFlow(
                UserPreferences(
                    isProUser = false,
                    responseTime = ResponseTime.SLOW,
                ),
            )
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())

        verify(exactly = 1) { audioEngine.setResponseTime(ResponseTime.FAST) }
        verify(exactly = 0) { audioEngine.setResponseTime(ResponseTime.SLOW) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun refreshRateChangeDoesNotReapplyAudioWeighting() = runTest(dispatcher) {
        grantMicrophonePermission()
        val runtimePreferences = MutableStateFlow(UserPreferences())
        userPreferencesFlow = runtimePreferences
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())

        runtimePreferences.value = UserPreferences(refreshRate = MeterRefreshRate.LOW)
        runCurrent()

        verify(exactly = 1) { audioEngine.setWeighting(WeightingType.A) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun refreshRateChangeDoesNotReapplyResponseTime() = runTest(dispatcher) {
        grantMicrophonePermission()
        val runtimePreferences =
            MutableStateFlow(
                UserPreferences(
                    isProUser = true,
                    responseTime = ResponseTime.SLOW,
                ),
            )
        userPreferencesFlow = runtimePreferences
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())

        runtimePreferences.value =
            UserPreferences(
                isProUser = true,
                responseTime = ResponseTime.SLOW,
                refreshRate = MeterRefreshRate.LOW,
            )
        runCurrent()

        verify(exactly = 1) { audioEngine.setResponseTime(ResponseTime.SLOW) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun proUpgradeDuringActiveSessionDoesNotChangeMeasurementAudioPreferences() = runTest(dispatcher) {
        grantMicrophonePermission()
        val runtimePreferences = MutableStateFlow(UserPreferences(isProUser = false))
        userPreferencesFlow = runtimePreferences
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())

        runtimePreferences.value =
            UserPreferences(
                isProUser = true,
                micSensitivityOffset = 4f,
                frequencyWeighting = WeightingType.C.name,
            )
        runCurrent()

        verify(exactly = 0) { audioEngine.setWeighting(WeightingType.C) }
        verify(exactly = 0) { audioEngine.setCalibrationOffset(4f) }
        verify { audioEngine.setSpectralAnalysisEnabled(true) }
        coVerify(exactly = 1) { audioEngine.startRecording(any()) }
        coVerify(exactly = 1) {
            sessionRepository.createActiveSession(
                startTime = any(),
                frequencyWeighting = WeightingType.A.name,
            )
        }

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
        val releaseRecording = stubStartedRecordingSession()
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
        coEvery { sessionRepository.createActiveSession(any(), any()) } returns 42L
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
    fun flushPersistsActiveSessionSummaryForInterruptedRecovery() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        decibelReadings.emit(
            DecibelReading(
                instantDb = 92f,
                weightedDb = 91f,
                timestamp = System.currentTimeMillis() + 2_000L,
                peakAmplitude = 0.8f,
                peakDb = 104f,
            ),
        )
        runCurrent()

        coVerify {
            sessionRepository.recordActiveSessionMeasurements(
                id = DEFAULT_SESSION_ID,
                measurements = match { measurements -> isSingleSessionMeasurement(measurements, expectedDb = 91f) },
                summary =
                    sessionMeasurementSummary(
                        minDb = 91f,
                        avgDb = 91f,
                        maxDb = 91f,
                        peakDb = 104f,
                        frequencyWeighting = WeightingType.A,
                    ),
            )
        }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun persistedMeasurementIncludesAWeightedDbAndResponseTimeMetadata() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow =
            MutableStateFlow(
                UserPreferences(
                    isProUser = true,
                    responseTime = ResponseTime.SLOW,
                ),
            )
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        decibelReadings.emit(
            DecibelReading(
                instantDb = 92f,
                weightedDb = 91f,
                aWeightedDb = 88f,
                timestamp = System.currentTimeMillis() + 2_000L,
                peakAmplitude = 0.8f,
                peakDb = 104f,
            ),
        )
        runCurrent()

        coVerify {
            sessionRepository.recordActiveSessionMeasurements(
                id = DEFAULT_SESSION_ID,
                measurements =
                    match { measurements ->
                        measurements.size == 1 &&
                            measurements.single().dbWeighted == 91f &&
                            measurements.single().aWeightedDb == 88f &&
                            measurements.single().responseTime == ResponseTime.SLOW.name
                    },
                summary = any(),
            )
        }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun stopSessionPersistsSameEquivalentLevelAsLiveStats() = runTest(dispatcher) {
        val startedSession = startRecordingSession()
        val manager = startedSession.manager

        decibelReadings.emit(readingAt(timestamp = startedSession.startTimeMs + 2_000L, weightedDb = 70f))
        decibelReadings.emit(readingAt(timestamp = startedSession.startTimeMs + 3_000L, weightedDb = 80f))
        runCurrent()

        val liveEquivalentLevelDb = manager.sessionStats.value.avgDb

        manager.stopSession()
        runCurrent()

        coVerify {
            sessionRepository.completeSessionWithMeasurements(
                id = DEFAULT_SESSION_ID,
                endTime = any(),
                measurements = any(),
                summary =
                    match { summary ->
                        abs(summary.avgDb - liveEquivalentLevelDb) < 0.001f &&
                            summary.frequencyWeighting == WeightingType.A.name
                    },
            )
        }

        startedSession.releaseRecording.complete(Unit)
    }

    @Test
    fun liveExposureStateUsesAWeightedReadingsAndEffectiveDosimeterStandard() = runTest(dispatcher) {
        userPreferencesFlow =
            MutableStateFlow(
                UserPreferences(
                    isProUser = true,
                    dosimeterStandard = DosimeterStandard.OSHA_PEL,
                ),
            )
        val startedSession = startRecordingSession()
        val manager = startedSession.manager

        decibelReadings.emit(
            DecibelReading(
                instantDb = 60f,
                weightedDb = 60f,
                aWeightedDb = 95f,
                timestamp = startedSession.startTimeMs + TWO_HOURS_MS,
                peakAmplitude = 0.8f,
                peakDb = 104f,
            ),
        )
        runCurrent()

        val exposure = manager.liveExposureState.value
        assertEquals(DosimeterStandard.OSHA_PEL, exposure.standard)
        assertEquals(95f, exposure.laeqDb, 0.1f)
        assertEquals(TWO_HOURS_MS, exposure.durationMs)
        assertEquals(50f, exposure.dosePercent, 0.1f)
        assertEquals(85f, exposure.twaDb, 0.1f)
        assertEquals(200f, exposure.projectedDosePercent, 0.1f)
        assertEquals(TWO_HOURS_MS, exposure.remainingExposureMs)

        manager.stopSession()
        startedSession.releaseRecording.complete(Unit)
    }

    @Test
    fun liveExposureStateUpdatesWithoutChangingSubSecondPersistenceCadence() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        val sessionStartTime = manager.activeSessionStartTimeMs.value ?: error("Missing active session start")
        decibelReadings.emit(readingAt(sessionStartTime + 100L, aWeightedDb = 85f))
        decibelReadings.emit(readingAt(sessionStartTime + 200L, aWeightedDb = 85f))
        runCurrent()

        assertEquals(2, manager.liveExposureState.value.sampleCount)
        assertEquals(200L, manager.liveExposureState.value.durationMs)
        coVerify(exactly = 0) {
            sessionRepository.recordActiveSessionMeasurements(
                id = any(),
                measurements = any(),
                summary = any(),
            )
        }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun liveEnvironmentMixCountsTrackWeightedReadingsFromActiveSession() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        val sessionStartTime = manager.activeSessionStartTimeMs.value ?: error("Missing active session start")
        decibelReadings.emit(readingAt(sessionStartTime + 100L, weightedDb = 39.9f))
        decibelReadings.emit(readingAt(sessionStartTime + 200L, weightedDb = 40f))
        decibelReadings.emit(readingAt(sessionStartTime + 300L, weightedDb = 70f))
        decibelReadings.emit(readingAt(sessionStartTime + 400L, weightedDb = 85f))
        runCurrent()

        assertEquals(
            EnvironmentExposureMixCounts(
                quietCount = 1,
                moderateCount = 1,
                loudCount = 1,
                criticalCount = 1,
                totalCount = 4,
            ),
            manager.liveEnvironmentMixCounts.value,
        )

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun freeUserDoesNotRunSoundDetectionInferenceEvenWhenToggleIsEnabled() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(isProUser = false, soundDetectionEnabled = true))
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()
        soundDetectionWindows.emit(floatArrayOf(0f))
        runCurrent()

        verify { audioEngine.setSoundDetectionEnabled(false) }
        verify(exactly = 0) { soundClassifier.classify(any()) }
        assertEquals(SoundDetectionState(), manager.soundDetectionState.value)

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun proUserWithSoundDetectionToggleOffDoesNotRunInference() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(isProUser = true, soundDetectionEnabled = false))
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()
        soundDetectionWindows.emit(floatArrayOf(0f))
        runCurrent()

        verify { audioEngine.setSoundDetectionEnabled(false) }
        verify(exactly = 0) { soundClassifier.classify(any()) }
        assertEquals(SoundDetectionState(), manager.soundDetectionState.value)

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun proUserWithSoundDetectionToggleOnPublishesCurrentAndRecentDetections() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(isProUser = true, soundDetectionEnabled = true))
        every { soundClassifier.classify(any()) } returns SoundClassification(label = "Speech", confidence = 0.82f)
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()
        soundDetectionWindows.emit(floatArrayOf(0.1f, 0.2f))
        runCurrent()

        val state = manager.soundDetectionState.value
        verify { audioEngine.setSoundDetectionEnabled(true) }
        verify(exactly = 1) { soundClassifier.classify(match { it.contentEquals(floatArrayOf(0.1f, 0.2f)) }) }
        assertTrue(state.isEnabled)
        assertEquals("Speech", state.current?.label)
        assertEquals(0.82f, state.current?.confidence ?: 0f, 0f)
        assertEquals(listOf("Speech"), state.recentDetections.map { it.label })

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun soundDetectionClassifierFailurePublishesErrorState() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(isProUser = true, soundDetectionEnabled = true))
        every { soundClassifier.classify(any()) } throws IllegalStateException("classifier unavailable")
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()
        soundDetectionWindows.emit(floatArrayOf(0.1f, 0.2f))
        runCurrent()

        val state = manager.soundDetectionState.value
        verify { audioEngine.setSoundDetectionEnabled(true) }
        verify(exactly = 1) { soundClassifier.classify(match { it.contentEquals(floatArrayOf(0.1f, 0.2f)) }) }
        assertTrue(state.isEnabled)
        assertEquals(SoundDetectionError.CLASSIFICATION_UNAVAILABLE, state.error)
        assertNull(state.current)

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun soundDetectionPersistenceRequiresSeparateOptIn() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow =
            MutableStateFlow(
                UserPreferences(
                    isProUser = true,
                    soundDetectionEnabled = true,
                    soundDetectionPersistenceEnabled = false,
                ),
            )
        every { soundClassifier.classify(any()) } returns SoundClassification(label = "Speech", confidence = 0.82f)
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()
        soundDetectionWindows.emit(floatArrayOf(0.1f, 0.2f))
        runCurrent()

        assertEquals("Speech", manager.soundDetectionState.value.current?.label)
        coVerify(exactly = 0) { soundDetectionRepository.recordEvent(any()) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun soundDetectionPersistenceStoresOnlyAggregatedLabelChanges() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow =
            MutableStateFlow(
                UserPreferences(
                    isProUser = true,
                    soundDetectionEnabled = true,
                    soundDetectionPersistenceEnabled = true,
                ),
            )
        every { soundClassifier.classify(any()) } returnsMany
            listOf(
                SoundClassification(label = "Speech", confidence = 0.82f),
                SoundClassification(label = "Speech", confidence = 0.87f),
                SoundClassification(label = "Traffic", confidence = 0.74f),
            )
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()
        soundDetectionWindows.emit(floatArrayOf(0.1f))
        soundDetectionWindows.emit(floatArrayOf(0.2f))
        soundDetectionWindows.emit(floatArrayOf(0.3f))
        runCurrent()

        coVerify(exactly = 1) {
            soundDetectionRepository.recordEvent(
                match {
                    it.matchesEvent(sessionId = DEFAULT_SESSION_ID, label = "Speech", confidence = 0.82f)
                },
            )
        }
        coVerify(exactly = 1) {
            soundDetectionRepository.recordEvent(
                match {
                    it.matchesEvent(sessionId = DEFAULT_SESSION_ID, label = "Traffic", confidence = 0.74f)
                },
            )
        }
        coVerify(exactly = 2) { soundDetectionRepository.recordEvent(any()) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun freeUserDoesNotStartWavRecordingEvenWhenDefaultIsEnabled() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(isProUser = false, wavRecordingDefaultEnabled = true))
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()

        verify(exactly = 0) { wavRecordingFileStore.createRecordingFile(any(), any()) }
        verify(exactly = 0) { audioEngine.startWavRecording(any()) }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun proUserWithWavRecordingDefaultStartsAndStopsWavWriter() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(isProUser = true, wavRecordingDefaultEnabled = true))
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()

        verify(exactly = 1) { wavRecordingFileStore.createRecordingFile(DEFAULT_SESSION_ID, any()) }
        verify(exactly = 1) { audioEngine.startWavRecording(wavRecordingFile) }

        manager.stopSession()
        releaseRecording.complete(Unit)

        verify(exactly = 1) { audioEngine.stopWavRecording() }
    }

    @Test
    fun wavRecordingIsAbortedWhenRecordingFailsMidSession() = runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(isProUser = true, wavRecordingDefaultEnabled = true))
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        runCurrent()

        manager.reportRecordingFailure(AudioRecordingFailure.ReadFailed(-3))
        releaseRecording.complete(Unit)

        verify(exactly = 1) { audioEngine.abortWavRecording() }
    }

    @Test
    fun resetStatsClearsLiveExposureStateAfterSilentCompletion() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        startSessionAndEmitReading(
            manager = manager,
            weightedDb = 71f,
            aWeightedDb = 88f,
        )

        manager.resetStats()
        assertEquals(1, manager.liveExposureState.value.sampleCount)

        manager.stopSession(emitCompleted = false)
        runCurrent()

        assertEquals(LiveExposureState(), manager.liveExposureState.value)

        releaseRecording.complete(Unit)
    }

    @Test
    fun recoverInterruptedSessionDoesNotPublishLiveExposureState() = runTest(dispatcher) {
        mockWidgetUpdates()
        val manager = createManager()
        stubSinglePersistedInterruptedSession()

        manager.recoverInterruptedSession()

        assertEquals(LiveExposureState(), manager.liveExposureState.value)
    }

    @Test
    fun stopSessionFinalizesPendingMeasurementsThroughSessionRepository() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        startSessionAndEmitReading(manager)

        manager.stopSession()
        runCurrent()

        verifySessionCompletedWithSingleMeasurement()

        releaseRecording.complete(Unit)
    }

    @Test
    fun resetStatsWhileRecordingDefersClearingUntilSilentCompletionPersistsSnapshot() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val completedSessions = mutableListOf<Long>()
        val manager = createManager()
        val completedJob =
            launch {
                manager.completedSessionIds.collect { completedSessions += it }
            }

        startSessionAndEmitReading(manager)

        manager.resetStats()
        assertEquals(1, manager.sessionStats.value.sampleCount)

        manager.stopSession(emitCompleted = false)
        runCurrent()

        verifySessionCompletedWithSingleMeasurement()
        assertEquals(SessionStats(), manager.sessionStats.value)
        assertTrue(completedSessions.isEmpty())

        releaseRecording.complete(Unit)
        completedJob.cancel()
    }

    @Test
    fun stopSessionKeepsPendingMeasurementsRetryableWhenFinalizationFails() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val failures = mutableListOf<AudioRecordingFailure>()
        var completionAttempts = 0
        coEvery {
            sessionRepository.completeSessionWithMeasurements(
                id = any(),
                endTime = any(),
                measurements = any(),
                summary = any(),
            )
        } coAnswers {
            completionAttempts += 1
            if (completionAttempts == 1) {
                throw IllegalStateException("database unavailable")
            }
        }
        val manager = createManager()
        val failureJob =
            launch {
                manager.recordingFailures.collect { failures += it }
            }

        startSessionAndEmitReading(manager)

        manager.stopSession()
        runCurrent()
        manager.stopSession()
        runCurrent()

        assertEquals(listOf(AudioRecordingFailure.PersistenceFailed), failures)
        assertEquals(2, completionAttempts)
        coVerify(exactly = 2) {
            sessionRepository.completeSessionWithMeasurements(
                id = DEFAULT_SESSION_ID,
                endTime = any(),
                measurements = match { measurements -> isSingleSessionMeasurement(measurements, expectedDb = 71f) },
                summary = any(),
            )
        }

        releaseRecording.complete(Unit)
        failureJob.cancel()
    }

    @Test
    fun startSessionDoesNotReplaceUnfinalizedSessionAfterStopFinalizationFailure() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        coEvery {
            sessionRepository.completeSessionWithMeasurements(
                id = any(),
                endTime = any(),
                measurements = any(),
                summary = any(),
            )
        } throws IllegalStateException("database unavailable")
        val manager = createManager()

        assertTrue(manager.startSession())
        manager.stopSession()
        runCurrent()

        assertFalse(manager.startSession())
        coVerify(exactly = 1) { sessionRepository.createActiveSession(any(), any()) }

        releaseRecording.complete(Unit)
    }

    @Test
    fun activeMeasurementFlushFailureStopsRecordingAndPublishesPersistenceFailure() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val failures = mutableListOf<AudioRecordingFailure>()
        coEvery {
            sessionRepository.recordActiveSessionMeasurements(
                id = any(),
                measurements = any(),
                summary = any(),
            )
        } throws IllegalStateException("database unavailable")
        val manager = createManager()
        val failureJob =
            launch {
                manager.recordingFailures.collect { failures += it }
            }

        assertTrue(manager.startSession())
        decibelReadings.emit(
            DecibelReading(
                instantDb = 92f,
                weightedDb = 91f,
                timestamp = System.currentTimeMillis() + 2_000L,
                peakAmplitude = 0.8f,
                peakDb = 104f,
            ),
        )
        runCurrent()

        assertFalse(manager.isRecording.value)
        assertEquals(listOf(AudioRecordingFailure.PersistenceFailed), failures)
        verify { audioEngine.stopRecording() }

        releaseRecording.complete(Unit)
        failureJob.cancel()
    }

    @Test
    fun stopSessionPublishesHealthConnectWriteFailureWithoutBlockingCompletion() = runTest(dispatcher) {
        assertStopSessionPublishesHealthConnectFailure("Health Connect write failed") {
            coEvery { healthConnectManager.writeNoiseDose(any()) } returns
                HealthConnectSyncResult.Failed("Health Connect write failed")
        }
    }

    @Test
    fun stopSessionPublishesHealthConnectExceptionWithoutBlockingCompletion() = runTest(dispatcher) {
        assertStopSessionPublishesHealthConnectFailure("Health Connect write failed") {
            every { context.getString(R.string.health_connect_sync_failed) } returns "Health Connect write failed"
            coEvery { healthConnectManager.writeNoiseDose(any()) } throws
                IllegalStateException("Health Connect unavailable")
        }
    }

    @Test
    fun stopSessionPublishesHealthConnectSkippedReasonWithoutBlockingCompletion() = runTest(dispatcher) {
        assertStopSessionPublishesHealthConnectFailure("Health Connect write permission missing") {
            coEvery { healthConnectManager.writeNoiseDose(any()) } returns
                HealthConnectSyncResult.Skipped("Health Connect write permission missing")
        }
    }

    @Test
    fun stopSessionPublishesHealthConnectMissingPermissionWhenStoredSyncEnabledButPermissionIsMissing() =
        runTest(dispatcher) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(healthConnectEnabled = true))
        coEvery { healthConnectManager.getStatus() } returns
            HealthConnectStatus(
                availability = HealthConnectAvailability.AVAILABLE,
                grantedPermissions = emptySet(),
            )
        coEvery { healthConnectManager.writeNoiseDose(any()) } returns
            HealthConnectSyncResult.Skipped("Health Connect noise sync permission missing")
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()
        every {
            measurementRepository.getReportMeasurementsForSession(DEFAULT_SESSION_ID)
        } returns MutableStateFlow(emptyList())
        val stopEvents = collectStopSessionEvents(manager)

        startAndStopSession(manager)

        assertEquals(listOf("Health Connect noise sync permission missing"), stopEvents.syncFailures)
        assertEquals(listOf(DEFAULT_SESSION_ID), stopEvents.completedSessions)
        coVerify(exactly = 1) { healthConnectManager.writeNoiseDose(any()) }

        releaseRecording.complete(Unit)
        stopEvents.cancel()
    }

    @Test
    fun recoverInterruptedSessionCompletesActiveSessionFromPersistedMeasurements() = runTest(dispatcher) {
        mockWidgetUpdates()
        val manager = createManager()
        stubSinglePersistedInterruptedSession()

        manager.recoverInterruptedSession()

        verifySinglePersistedInterruptedSessionCompleted()
    }

    @Test
    fun recoverInterruptedSessionIgnoresWidgetUpdateFailure() = runTest(dispatcher) {
        mockkObject(DbCheckWidgetReceiver.Companion)
        coEvery { DbCheckWidgetReceiver.updateAllWidgets(any()) } throws IllegalStateException("widget")
        val manager = createManager()
        stubSinglePersistedInterruptedSession()

        manager.recoverInterruptedSession()

        verifySinglePersistedInterruptedSessionCompleted()
    }

    @Test
    fun recoverInterruptedSessionPreservesFlushedActiveSessionSummary() = runTest(dispatcher) {
        mockWidgetUpdates()
        val manager = createManager()
        stubInterruptedSession(
            activeSession =
                activeSession(
                    minDb = 64f,
                    avgDb = 88f,
                    maxDb = 96f,
                    peakDb = 121f,
                    frequencyWeighting = WeightingType.C.name,
                ),
            measurements =
                listOf(
                    measurement(timestamp = 2_500L, dbWeighted = 64f),
                    measurement(timestamp = 3_500L, dbWeighted = 70f),
                ),
        )

        manager.recoverInterruptedSession()

        coVerify {
            sessionRepository.completeSessionWithMeasurements(
                id = 42L,
                endTime = 3_500L,
                measurements = emptyList(),
                summary =
                    SessionMeasurementSummary(
                        minDb = 64f,
                        avgDb = 88f,
                        maxDb = 96f,
                        peakDb = 121f,
                        frequencyWeighting = WeightingType.C.name,
                    ),
            )
        }
    }

    @Test
    fun recoverInterruptedSessionDoesNotCompleteCurrentInMemoryRecordingSession() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        every { sessionRepository.getActiveSession() } returns
            MutableStateFlow(
                activeSession(
                    startTime = System.currentTimeMillis(),
                    frequencyWeighting = WeightingType.A.name,
                ),
            )
        every { measurementRepository.getSessionMeasurements(42L) } returns MutableStateFlow(emptyList())

        manager.recoverInterruptedSession()
        runCurrent()

        coVerify(exactly = 0) {
            sessionRepository.completeSessionWithMeasurements(
                id = 42L,
                endTime = any(),
                measurements = any(),
                summary = any(),
            )
        }

        manager.stopSession()
        releaseRecording.complete(Unit)
    }

    @Test
    fun stopSessionWaitsForInFlightFlushBeforeCompletingSession() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val flushStarted = CompletableDeferred<Unit>()
        val releaseFlush = CompletableDeferred<Unit>()
        coEvery {
            sessionRepository.recordActiveSessionMeasurements(
                id = any(),
                measurements = any(),
                summary = any(),
            )
        } coAnswers {
            flushStarted.complete(Unit)
            releaseFlush.await()
        }
        val manager = createManager()

        assertTrue(manager.startSession())
        val emitJob =
            launch {
                decibelReadings.emit(
                    DecibelReading(
                        instantDb = 92f,
                        weightedDb = 91f,
                        timestamp = System.currentTimeMillis() + 2_000L,
                        peakAmplitude = 0.8f,
                        peakDb = 104f,
                    ),
                )
            }
        flushStarted.await()

        manager.stopSession()
        runCurrent()

        coVerify(exactly = 0) {
            sessionRepository.completeSessionWithMeasurements(
                id = any(),
                endTime = any(),
                measurements = any(),
                summary = any(),
            )
        }

        releaseFlush.complete(Unit)
        releaseRecording.complete(Unit)
        emitJob.join()
        runCurrent()

        coVerify {
            sessionRepository.completeSessionWithMeasurements(
                id = 42L,
                endTime = any(),
                measurements = emptyList(),
                summary =
                    SessionMeasurementSummary(
                        minDb = 91f,
                        avgDb = 91f,
                        maxDb = 91f,
                        peakDb = 104f,
                        frequencyWeighting = WeightingType.A.name,
                    ),
            )
        }
    }

    private fun createManager() = AudioSessionManager(
        context = context,
        audioEngine = audioEngine,
        soundClassifier = soundClassifier,
        sessionRepository = sessionRepository,
        measurementRepository = measurementRepository,
        soundDetectionRepository = soundDetectionRepository,
        wavRecordingFileStore = wavRecordingFileStore,
        sessionLocationCapturePort = sessionLocationCapturePort,
        preferencesRepository = preferencesRepository,
        healthConnectManager = healthConnectManager,
        notificationHelper = notificationHelper,
        defaultDispatcher = dispatcher,
    )

    private suspend fun startRecordingSession(): StartedRecordingSession {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecordingSession()
        val manager = createManager()

        assertTrue(manager.startSession())
        val startTimeMs = manager.activeSessionStartTimeMs.value ?: error("Missing active session start")

        return StartedRecordingSession(
            manager = manager,
            releaseRecording = releaseRecording,
            startTimeMs = startTimeMs,
        )
    }

    private suspend fun TestScope.startSessionAndEmitReading(
        manager: AudioSessionManager,
        instantDb: Float = 72f,
        weightedDb: Float = 71f,
        aWeightedDb: Float = weightedDb,
        timestamp: Long = System.currentTimeMillis() + 100L,
        peakAmplitude: Float = 0.5f,
        peakDb: Float = 83f,
    ) {
        assertTrue(manager.startSession())
        decibelReadings.emit(
            DecibelReading(
                instantDb = instantDb,
                weightedDb = weightedDb,
                aWeightedDb = aWeightedDb,
                timestamp = timestamp,
                peakAmplitude = peakAmplitude,
                peakDb = peakDb,
            ),
        )
        runCurrent()
    }

    private suspend fun TestScope.assertStopSessionPublishesHealthConnectFailure(
        expectedFailure: String,
        stubHealthConnectWrite: () -> Unit,
    ) {
        grantMicrophonePermission()
        userPreferencesFlow = MutableStateFlow(UserPreferences(healthConnectEnabled = true))
        coEvery { healthConnectManager.getStatus() } returns
            HealthConnectStatus(
                availability = HealthConnectAvailability.AVAILABLE,
                grantedPermissions = HealthConnectPermissions.NOISE_SYNC,
            )
        val releaseRecording = stubStartedRecordingSession()
        every {
            measurementRepository.getReportMeasurementsForSession(DEFAULT_SESSION_ID)
        } returns MutableStateFlow(emptyList())
        stubHealthConnectWrite()
        val manager = createManager()
        val stopEvents = collectStopSessionEvents(manager)

        startAndStopSession(manager)

        assertEquals(listOf(expectedFailure), stopEvents.syncFailures)
        assertEquals(listOf(DEFAULT_SESSION_ID), stopEvents.completedSessions)

        releaseRecording.complete(Unit)
        stopEvents.cancel()
    }

    private fun TestScope.collectStopSessionEvents(manager: AudioSessionManager): StopSessionEvents {
        val syncFailures = mutableListOf<String>()
        val completedSessions = mutableListOf<Long>()
        val syncFailureJob =
            launch {
                manager.healthConnectSyncFailures.collect { syncFailures += it }
            }
        val completedJob =
            launch {
                manager.completedSessionIds.collect { completedSessions += it }
            }

        return StopSessionEvents(syncFailures, completedSessions, listOf(syncFailureJob, completedJob))
    }

    private data class StartedRecordingSession(
        val manager: AudioSessionManager,
        val releaseRecording: CompletableDeferred<Unit>,
        val startTimeMs: Long,
    )

    private suspend fun TestScope.startAndStopSession(manager: AudioSessionManager) {
        assertTrue(manager.startSession())
        Thread.sleep(2L)
        manager.stopSession()
        runCurrent()
    }

    private data class StopSessionEvents(
        val syncFailures: List<String>,
        val completedSessions: List<Long>,
        private val jobs: List<Job>,
    ) {
        fun cancel() {
            jobs.forEach { it.cancel() }
        }
    }

    private fun stubStartedRecordingSession(
        sessionId: Long = DEFAULT_SESSION_ID,
        onStartRecording: () -> Unit = {},
    ): CompletableDeferred<Unit> {
        val releaseRecording = CompletableDeferred<Unit>()
        coEvery { sessionRepository.createActiveSession(any(), any()) } returns sessionId
        coEvery { audioEngine.startRecording(any()) } coAnswers {
            onStartRecording()
            firstArg<suspend () -> Unit>().invoke()
            releaseRecording.await()
            AudioRecordingResult.Stopped
        }
        return releaseRecording
    }

    private fun stubInterruptedSession(activeSession: Session, measurements: List<SessionMeasurement>) {
        every { sessionRepository.getActiveSession() } returns MutableStateFlow(activeSession)
        every { measurementRepository.getSessionMeasurements(activeSession.id) } returns
            MutableStateFlow(measurements)
    }

    private fun stubSinglePersistedInterruptedSession() {
        stubInterruptedSession(
            activeSession = activeSession(frequencyWeighting = WeightingType.C.name),
            measurements = listOf(measurement(timestamp = 2_500L, dbWeighted = 72f)),
        )
    }

    private fun verifySinglePersistedInterruptedSessionCompleted() {
        coVerify {
            sessionRepository.completeSessionWithMeasurements(
                id = DEFAULT_SESSION_ID,
                endTime = 2_500L,
                measurements = emptyList(),
                summary =
                    sessionMeasurementSummary(
                        minDb = 72f,
                        avgDb = 72f,
                        maxDb = 72f,
                        peakDb = 0f,
                        frequencyWeighting = WeightingType.C,
                    ),
            )
        }
    }

    private fun verifySessionCompletedWithSingleMeasurement(
        expectedDb: Float = 71f,
        expectedPeakDb: Float = 83f,
        expectedWeighting: WeightingType = WeightingType.A,
    ) {
        coVerify {
            sessionRepository.completeSessionWithMeasurements(
                id = DEFAULT_SESSION_ID,
                endTime = any(),
                measurements = match { measurements ->
                    isSingleSessionMeasurement(measurements, expectedDb = expectedDb)
                },
                summary =
                    sessionMeasurementSummary(
                        minDb = expectedDb,
                        avgDb = expectedDb,
                        maxDb = expectedDb,
                        peakDb = expectedPeakDb,
                        frequencyWeighting = expectedWeighting,
                    ),
            )
        }
    }

    private fun activeSession(
        startTime: Long = 1_000L,
        minDb: Float = 0f,
        avgDb: Float = 0f,
        maxDb: Float = 0f,
        peakDb: Float = 0f,
        frequencyWeighting: String,
    ): Session = Session(
            id = DEFAULT_SESSION_ID,
            startTime = startTime,
            endTime = null,
            minDb = minDb,
            avgDb = avgDb,
            maxDb = maxDb,
            peakDb = peakDb,
            name = null,
            emoji = null,
            tags = emptyList(),
            isActive = true,
            frequencyWeighting = frequencyWeighting,
        )

    private fun measurement(timestamp: Long, dbWeighted: Float): SessionMeasurement = SessionMeasurement(
            timestamp = timestamp,
            dbValue = dbWeighted,
            dbWeighted = dbWeighted,
            peakDb = dbWeighted,
        )

    private fun isSingleSessionMeasurement(measurements: List<SessionMeasurement>, expectedDb: Float): Boolean =
        measurements.size == 1 &&
            measurements.single().dbWeighted == expectedDb

    private fun readingAt(
        timestamp: Long,
        weightedDb: Float = 71f,
        aWeightedDb: Float = weightedDb,
        peakDb: Float = 83f,
    ): DecibelReading = DecibelReading(
        instantDb = weightedDb,
        weightedDb = weightedDb,
        aWeightedDb = aWeightedDb,
        timestamp = timestamp,
        peakAmplitude = 0.5f,
        peakDb = peakDb,
    )

    private fun SoundDetectionEvent.matchesEvent(sessionId: Long, label: String, confidence: Float): Boolean =
        this.sessionId == sessionId &&
            this.label == label &&
            this.confidence == confidence &&
            timestamp > 0L

    private fun sessionMeasurementSummary(
        minDb: Float,
        avgDb: Float,
        maxDb: Float,
        peakDb: Float,
        frequencyWeighting: WeightingType,
    ): SessionMeasurementSummary = SessionMeasurementSummary(
        minDb = minDb,
        avgDb = avgDb,
        maxDb = maxDb,
        peakDb = peakDb,
        frequencyWeighting = frequencyWeighting.name,
    )

    private fun grantMicrophonePermission() {
        mockkStatic(ContextCompat::class)
        mockWidgetUpdates()
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
        every { sessionRepository.getActiveSession() } returns flowOf(null)
    }

    private fun mockWidgetUpdates() {
        mockkObject(DbCheckWidgetReceiver.Companion)
        coEvery { DbCheckWidgetReceiver.updateAllWidgets(any()) } returns Unit
    }

    private companion object {
        const val DEFAULT_SESSION_ID = 42L
        const val ONE_HOUR_MS = 60 * 60 * 1_000L
        const val TWO_HOURS_MS = 2 * ONE_HOUR_MS
        const val START_LOCATION_CAPTURED_AT = 1_700_000_010_000L
        const val STOP_LOCATION_CAPTURED_AT = 1_700_000_020_000L
    }
}

private class FakeSessionLocationCapturePort : SessionLocationCapturePort {
    private val queuedLocations = mutableListOf<SessionLocationMetadata?>()
    var captureCount: Int = 0
        private set

    fun enqueue(vararg locations: SessionLocationMetadata?) {
        queuedLocations.addAll(locations)
    }

    override suspend fun captureOneShotLocation(): SessionLocationMetadata? {
        captureCount += 1
        return if (queuedLocations.isEmpty()) null else queuedLocations.removeAt(0)
    }
}

private fun sessionLocation(capturedAt: Long): SessionLocationMetadata = SessionLocationMetadata(
        latitude = 60.1699,
        longitude = 24.9384,
        accuracyMeters = 18.5f,
        capturedAt = capturedAt,
    )
