package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.passive.PassiveMonitoringSample
import com.dbcheck.app.service.AudioEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassiveMonitoringManagerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>()
    private val decibelReadings = MutableSharedFlow<DecibelReading>(replay = 1)
    private val audioEngine =
        mockk<AudioEngine>(relaxed = true) {
            every { decibelFlow } returns decibelReadings
        }
    private val passiveMonitoringRepository = mockk<PassiveMonitoringRepository>(relaxed = true)
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns MutableStateFlow(UserPreferences(isProUser = true))
        }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun passiveMonitoringPersistsOneAggregateSampleOnStop() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecording()
        val manager = createManager()
        val sample = slot<PassiveMonitoringSample>()

        assertTrue(manager.startMonitoring())
        decibelReadings.emit(reading(weightedDb = 70f, peakDb = 75f))
        decibelReadings.emit(reading(weightedDb = 80f, peakDb = 92f))
        runCurrent()

        manager.stopMonitoring()
        releaseRecording.complete(Unit)
        runCurrent()

        coVerify(exactly = 1) { passiveMonitoringRepository.recordSample(capture(sample)) }
        assertEquals(2, sample.captured.readingCount)
        assertEquals(92f, sample.captured.peakDb, FLOAT_TOLERANCE)
        assertFalse(manager.isMonitoring.value)
    }

    @Test
    fun passiveMonitoringDoesNotEnableRawOrTriggerPipelines() = runTest(dispatcher) {
        grantMicrophonePermission()
        val releaseRecording = stubStartedRecording()
        val manager = createManager()

        assertTrue(manager.startMonitoring())
        manager.stopMonitoring()
        releaseRecording.complete(Unit)

        verify { audioEngine.setSoundDetectionEnabled(false) }
        verify { audioEngine.setSpectralAnalysisEnabled(false) }
        coVerify(exactly = 0) { audioEngine.startWavRecording(any()) }
        coVerify(exactly = 0) { passiveMonitoringRepository.recordSample(match { it.readingCount == 0 }) }
    }

    @Test
    fun missingMicrophonePermissionDoesNotStartPassiveMonitoring() = runTest(dispatcher) {
        denyMicrophonePermission()
        val manager = createManager()

        assertFalse(manager.startMonitoring())

        coVerify(exactly = 0) { audioEngine.startRecording(any()) }
        assertFalse(manager.isMonitoring.value)
    }

    private fun createManager(): PassiveMonitoringManager = PassiveMonitoringManager(
            context = context,
            audioEngine = audioEngine,
            preferencesRepository = preferencesRepository,
            passiveMonitoringRepository = passiveMonitoringRepository,
            defaultDispatcher = dispatcher,
        )

    private fun stubStartedRecording(): CompletableDeferred<Unit> {
        val releaseRecording = CompletableDeferred<Unit>()
        coEvery { audioEngine.startRecording(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            releaseRecording.await()
            AudioRecordingResult.Stopped
        }
        return releaseRecording
    }

    private fun grantMicrophonePermission() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
    }

    private fun denyMicrophonePermission() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_DENIED
    }

    private fun reading(weightedDb: Float, peakDb: Float): DecibelReading = DecibelReading(
            instantDb = weightedDb,
            weightedDb = weightedDb,
            aWeightedDb = weightedDb,
            timestamp = System.currentTimeMillis(),
            peakAmplitude = 0.5f,
            peakDb = peakDb,
        )

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
