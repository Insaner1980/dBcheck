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
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionMeasurement
import com.dbcheck.app.sync.HealthConnectManager
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

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
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
    fun proUpgradeDuringActiveSessionAppliesProAudioPreferencesWithoutRestart() = runTest(dispatcher) {
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

        verify { audioEngine.setWeighting(WeightingType.C) }
        verify { audioEngine.setCalibrationOffset(4f) }
        verify { audioEngine.setSpectralAnalysisEnabled(true) }
        coVerify(exactly = 1) { audioEngine.startRecording(any()) }
        coVerify(exactly = 1) { sessionRepository.createActiveSession(any(), any()) }

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
        sessionRepository = sessionRepository,
        measurementRepository = measurementRepository,
        preferencesRepository = preferencesRepository,
        healthConnectManager = healthConnectManager,
        notificationHelper = notificationHelper,
        defaultDispatcher = dispatcher,
    )

    private suspend fun TestScope.startSessionAndEmitReading(
        manager: AudioSessionManager,
        instantDb: Float = 72f,
        weightedDb: Float = 71f,
        timestamp: Long = System.currentTimeMillis() + 100L,
        peakAmplitude: Float = 0.5f,
        peakDb: Float = 83f,
    ) {
        assertTrue(manager.startSession())
        decibelReadings.emit(
            DecibelReading(
                instantDb = instantDb,
                weightedDb = weightedDb,
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
        val releaseRecording = stubStartedRecordingSession()
        val syncFailures = mutableListOf<String>()
        val completedSessions = mutableListOf<Long>()
        every {
            measurementRepository.getReportMeasurementsForSession(DEFAULT_SESSION_ID)
        } returns MutableStateFlow(emptyList())
        stubHealthConnectWrite()
        val manager = createManager()
        val syncFailureJob =
            launch {
                manager.healthConnectSyncFailures.collect { syncFailures += it }
            }
        val completedJob =
            launch {
                manager.completedSessionIds.collect { completedSessions += it }
            }

        assertTrue(manager.startSession())
        Thread.sleep(2L)
        manager.stopSession()
        runCurrent()

        assertEquals(listOf(expectedFailure), syncFailures)
        assertEquals(listOf(DEFAULT_SESSION_ID), completedSessions)

        releaseRecording.complete(Unit)
        syncFailureJob.cancel()
        completedJob.cancel()
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
    }
}
