package com.dbcheck.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import androidx.core.content.ContextCompat
import com.dbcheck.app.domain.audio.AudioProcessingConfig
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import com.dbcheck.app.domain.audio.DecibelCalculator
import com.dbcheck.app.domain.audio.FFTProcessor
import com.dbcheck.app.domain.audio.FrequencyWeightingFilter
import com.dbcheck.app.domain.audio.OctaveBandRtaCalculator
import com.dbcheck.app.domain.audio.SoundDetectionWindowFanout
import com.dbcheck.app.domain.audio.SpectralAnalyzer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Modifier
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEngineRuntimePreferenceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun tearDown() {
        unmockkConstructor(AudioRecord::class)
        unmockkStatic(AudioRecord::class)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun calibrationOffsetIsVolatileForRuntimePreferenceUpdates() {
        val field = AudioEngine::class.java.getDeclaredField("calibrationOffset")

        assertTrue(Modifier.isVolatile(field.modifiers))
    }

    @Test
    fun audioEngineKeepsAWeightingFilterSeparateFromSelectedAndPeakFilters() {
        val selectedWeightingFilter = FrequencyWeightingFilter()
        val engine = createEngine(selectedWeightingFilter = selectedWeightingFilter)
        val aWeightingFilter = AudioEngine::class.java
            .getDeclaredField("aWeightingFilter")
            .also { it.isAccessible = true }
            .get(engine)
        val cPeakWeightingFilter = AudioEngine::class.java
            .getDeclaredField("cPeakWeightingFilter")
            .also { it.isAccessible = true }
            .get(engine)

        assertNotSame(selectedWeightingFilter, aWeightingFilter)
        assertNotSame(cPeakWeightingFilter, aWeightingFilter)
    }

    @Test
    fun wavWriterLifecycleRunsOnIoDispatcher() = runTest {
        val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "wav-writer-io") }
        val dispatcher = executor.asCoroutineDispatcher()
        val wavFile = ThreadRecordingFile(temporaryFolder.newFile("session.wav").absolutePath)
        val engine = createEngine(ioDispatcher = dispatcher)

        try {
            engine.startWavRecording(wavFile)
            engine.stopWavRecording()

            assertEquals("wav-writer-io", wavFile.parentLookupThreadName)
        } finally {
            dispatcher.close()
            executor.shutdown()
        }
    }

    @Test
    fun audioRecordStartFailsWhenRecordingStateDoesNotEnterRecording() {
        val audioRecord =
            mockk<AudioRecord> {
                every { startRecording() } just Runs
                every { recordingState } returns AudioRecord.RECORDSTATE_STOPPED
            }

        assertFalse(invokeStartAudioRecord(createEngine(), audioRecord))
    }

    @Test
    fun audioRecordStartSucceedsWhenRecordingStateIsRecording() {
        val audioRecord =
            mockk<AudioRecord> {
                every { startRecording() } just Runs
                every { recordingState } returns AudioRecord.RECORDSTATE_RECORDING
            }

        assertTrue(invokeStartAudioRecord(createEngine(), audioRecord))
    }

    @Test
    fun routeResolutionFailureReleasesCreatedAudioRecord() = runTest {
        mockInitializedAudioRecord()
        val engine = createEngine(audioInputDeviceRouter = ThrowingAudioInputDeviceRouter)

        val result = runCatching { engine.startRecording() }

        verify(exactly = 1) { anyConstructed<AudioRecord>().release() }
        assertEquals(
            AudioRecordingResult.Failed(AudioRecordingFailure.StartFailed),
            result.getOrThrow(),
        )
    }

    @Test
    fun preferredDeviceApplicationFailureKeepsResolvedRouteForDefaultFallback() {
        val engine = createEngine(audioInputDeviceRouter = PreferredAudioInputDeviceRouter(applied = false))

        val route = invokeConfigureAudioInputRoute(engine, mockk())

        assertEquals(12, route?.selectedDeviceId)
        assertEquals("USB-C microphone", route?.selectedDeviceName)
    }

    @Test
    fun preferredDeviceApplicationSuccessKeepsResolvedRoute() {
        val engine = createEngine(audioInputDeviceRouter = PreferredAudioInputDeviceRouter(applied = true))

        val route = invokeConfigureAudioInputRoute(engine, mockk())

        assertEquals(12, route?.selectedDeviceId)
        assertEquals("USB-C microphone", route?.selectedDeviceName)
    }

    private fun invokeStartAudioRecord(engine: AudioEngine, audioRecord: AudioRecord): Boolean {
        val method = AudioEngine::class.java
            .getDeclaredMethod("startAudioRecord", AudioRecord::class.java)
            .also { it.isAccessible = true }
        return method.invoke(engine, audioRecord) as Boolean
    }

    private fun invokeConfigureAudioInputRoute(
        engine: AudioEngine,
        audioRecord: AudioRecord,
    ): ResolvedAudioInputDeviceRoute? {
        val method =
            AudioEngine::class.java
                .getDeclaredMethod("configureAudioInputRoute", AudioRecord::class.java)
                .also { it.isAccessible = true }
        return method.invoke(engine, audioRecord) as ResolvedAudioInputDeviceRoute?
    }

    private fun createEngine(
        selectedWeightingFilter: FrequencyWeightingFilter = FrequencyWeightingFilter(),
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = UnconfinedTestDispatcher(),
        audioInputDeviceRouter: AudioInputDeviceRouter = FakeAudioInputDeviceRouter,
    ): AudioEngine = AudioEngine(
        context = mockk<Context>(),
        decibelCalculator = DecibelCalculator(),
        weightingFilter = selectedWeightingFilter,
        spectralAnalyzer = SpectralAnalyzer(FFTProcessor()),
        rtaCalculator = OctaveBandRtaCalculator(FFTProcessor()),
        soundDetectionWindowFanout = SoundDetectionWindowFanout(),
        audioInputDeviceRouter = audioInputDeviceRouter,
        defaultDispatcher = UnconfinedTestDispatcher(),
        ioDispatcher = ioDispatcher,
    )

    private fun mockInitializedAudioRecord() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), Manifest.permission.RECORD_AUDIO) } returns
            PackageManager.PERMISSION_GRANTED
        mockkStatic(AudioRecord::class)
        every {
            AudioRecord.getMinBufferSize(any(), any(), any())
        } returns AUDIO_RECORD_BUFFER_SIZE
        mockkConstructor(AudioRecord::class)
        every { anyConstructed<AudioRecord>().state } returns AudioRecord.STATE_INITIALIZED
        every { anyConstructed<AudioRecord>().recordingState } returns AudioRecord.RECORDSTATE_STOPPED
        every { anyConstructed<AudioRecord>().release() } just Runs
        every { anyConstructed<AudioRecord>().stop() } just Runs
    }

    private companion object {
        const val AUDIO_RECORD_BUFFER_SIZE = AudioProcessingConfig.CHUNK_SIZE * 2
    }
}

private class ThreadRecordingFile(path: String) : File(path) {
    var parentLookupThreadName: String? = null
        private set

    override fun getParentFile(): File? {
        parentLookupThreadName = Thread.currentThread().name
        return super.getParentFile()
    }
}

private object FakeAudioInputDeviceRouter : AudioInputDeviceRouter {
    override fun resolvePreferredDevice(preferredDeviceId: Int?): ResolvedAudioInputDeviceRoute =
        ResolvedAudioInputDeviceRoute(
            preferredDevice = null,
            selectedDeviceId = null,
            selectedDeviceName = null,
        )

    override fun applyPreferredDevice(audioRecord: AudioRecord, preferredDevice: AudioInputRoute?): Boolean = true

    override fun routedDeviceName(audioRecord: AudioRecord): String? = null
}

private object ThrowingAudioInputDeviceRouter : AudioInputDeviceRouter {
    override fun resolvePreferredDevice(preferredDeviceId: Int?): ResolvedAudioInputDeviceRoute =
        error("Device route resolution failed")

    override fun applyPreferredDevice(audioRecord: AudioRecord, preferredDevice: AudioInputRoute?): Boolean = true

    override fun routedDeviceName(audioRecord: AudioRecord): String? = null
}

private class PreferredAudioInputDeviceRouter(private val applied: Boolean) : AudioInputDeviceRouter {
    override fun resolvePreferredDevice(preferredDeviceId: Int?): ResolvedAudioInputDeviceRoute =
        ResolvedAudioInputDeviceRoute(
            preferredDevice =
                object : AudioInputRoute {
                    override val id: Int = 12
                    override val displayName: String = "USB-C microphone"
                },
            selectedDeviceId = 12,
            selectedDeviceName = "USB-C microphone",
        )

    override fun applyPreferredDevice(audioRecord: AudioRecord, preferredDevice: AudioInputRoute?): Boolean = applied

    override fun routedDeviceName(audioRecord: AudioRecord): String? = null
}
