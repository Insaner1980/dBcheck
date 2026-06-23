package com.dbcheck.app.domain.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.dbcheck.app.di.DefaultDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DecibelReading(
    val instantDb: Float,
    val weightedDb: Float,
    val aWeightedDb: Float = weightedDb,
    val timestamp: Long,
    val peakAmplitude: Float,
    val peakDb: Float = instantDb,
)

@Singleton
class AudioEngine
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val decibelCalculator: DecibelCalculator,
        private val weightingFilter: FrequencyWeightingFilter,
        private val spectralAnalyzer: SpectralAnalyzer,
        private val rtaCalculator: OctaveBandRtaCalculator,
        private val soundDetectionWindowFanout: SoundDetectionWindowFanout,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        companion object {
            private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
            private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        }

        private val _decibelFlow = MutableSharedFlow<DecibelReading>(replay = 1)
        val decibelFlow: SharedFlow<DecibelReading> = _decibelFlow
        private val _spectralFrame = MutableStateFlow<SpectralFrame?>(null)
        val spectralFrame: StateFlow<SpectralFrame?> = _spectralFrame
        private val _rtaFrame = MutableStateFlow<RtaFrame?>(null)
        val rtaFrame: StateFlow<RtaFrame?> = _rtaFrame
        private val _audioInputInfo = MutableStateFlow(AudioInputInfo())
        val audioInputInfo: StateFlow<AudioInputInfo> = _audioInputInfo
        val soundDetectionWindows: SharedFlow<FloatArray> = soundDetectionWindowFanout.windows

        private val audioRecordLock = Any()
        private val wavRecordingLock = Any()
        private var audioRecord: AudioRecord? = null
        private var wavWriter: PcmWavWriter? = null
        private val aWeightingFilter = FrequencyWeightingFilter()
        private val cPeakWeightingFilter = FrequencyWeightingFilter()
        private val responseTimeLock = Any()
        private val responseTimeProcessor = ResponseTimedDecibelReadingProcessor()

        @Volatile
        private var isRecording = false

        @Volatile
        private var currentWeighting = WeightingType.DEFAULT

        @Volatile
        private var spectralAnalysisEnabled = false

        @Volatile
        private var calibrationOffset = 0f

        fun setWeighting(weighting: WeightingType) {
            currentWeighting = weighting
            weightingFilter.reset()
        }

        fun setCalibrationOffset(offset: Float) {
            calibrationOffset = offset
        }

        fun setResponseTime(responseTime: ResponseTime) {
            synchronized(responseTimeLock) {
                responseTimeProcessor.setResponseTime(responseTime)
            }
        }

        fun setSpectralAnalysisEnabled(enabled: Boolean) {
            spectralAnalysisEnabled = enabled
            if (!enabled) {
                _spectralFrame.value = null
                _rtaFrame.value = null
            }
        }

        fun setSoundDetectionEnabled(enabled: Boolean) {
            soundDetectionWindowFanout.setEnabled(enabled)
        }

        fun startWavRecording(file: File) {
            synchronized(wavRecordingLock) {
                wavWriter?.abort()
                wavWriter = PcmWavWriter.create(file)
            }
        }

        fun stopWavRecording() {
            synchronized(wavRecordingLock) {
                wavWriter?.close()
                wavWriter = null
            }
        }

        fun abortWavRecording() {
            synchronized(wavRecordingLock) {
                wavWriter?.abort()
                wavWriter = null
            }
        }

        suspend fun startRecording(onRecordingStarted: suspend () -> Unit = {}): AudioRecordingResult =
            withContext(defaultDispatcher) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext AudioRecordingResult.Failed(AudioRecordingFailure.PermissionDenied)
                }

                val record =
                    createAudioRecord()
                        ?: return@withContext AudioRecordingResult.Failed(AudioRecordingFailure.CreationFailed)
                synchronized(audioRecordLock) {
                    audioRecord = record
                }

                try {
                    if (!startAudioRecord(record)) {
                        return@withContext AudioRecordingResult.Failed(AudioRecordingFailure.StartFailed)
                    }

                    publishAudioInputInfo(record)
                    isRecording = true
                    weightingFilter.reset()
                    aWeightingFilter.reset()
                    cPeakWeightingFilter.reset()
                    resetResponseTimeSmoothing()
                    onRecordingStarted()
                    recordLoop(record)
                } finally {
                    isRecording = false
                    releaseCurrentRecord(record)
                    _audioInputInfo.value = AudioInputInfo()
                    _spectralFrame.value = null
                    _rtaFrame.value = null
                    soundDetectionWindowFanout.setEnabled(false)
                }
            }

        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        private fun createAudioRecord(): AudioRecord? {
            val bufferSize = captureBufferSizeBytes()
            val record = bufferSize?.let(::buildAudioRecord)
            return record?.let(::initializedAudioRecordOrNull)
        }

        private fun captureBufferSizeBytes(): Int? {
            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    AudioProcessingConfig.SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                )
            return AudioRecordBufferPolicy.captureBufferSizeBytes(minBufferSize)
        }

        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        private fun buildAudioRecord(bufferSize: Int): AudioRecord? = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AudioProcessingConfig.SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize,
            )
        }.getOrNull()

        private fun initializedAudioRecordOrNull(record: AudioRecord): AudioRecord? =
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record
            } else {
                record.release()
                null
            }

        private fun startAudioRecord(record: AudioRecord): Boolean = runCatching {
            record.startRecording()
        }.isSuccess

        private fun publishAudioInputInfo(record: AudioRecord) {
            _audioInputInfo.value =
                AudioInputInfo(
                    sampleRateHz = AudioProcessingConfig.SAMPLE_RATE,
                    inputDeviceName = activeInputDeviceName(record),
                )
        }

        private fun activeInputDeviceName(record: AudioRecord): String? = runCatching {
            record.routedDevice
                ?.productName
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()

        private suspend fun recordLoop(record: AudioRecord): AudioRecordingResult {
            val buffer = ShortArray(AudioProcessingConfig.CHUNK_SIZE)

            while (true) {
                kotlin.coroutines.coroutineContext.ensureActive()
                val readCount = readAudioChunk(record, buffer)
                when (val action = AudioRecordReadPolicy.actionFor(readCount, isRecording)) {
                    AudioRecordReadAction.Process -> processAudioChunk(buffer, readCount)

                    AudioRecordReadAction.Continue -> Unit

                    AudioRecordReadAction.Stop -> return AudioRecordingResult.Stopped

                    is AudioRecordReadAction.Fail ->
                        return AudioRecordingResult.Failed(
                            AudioRecordingFailure.ReadFailed(action.errorCode),
                        )
                }
            }
        }

        private fun readAudioChunk(record: AudioRecord, buffer: ShortArray): Int = runCatching {
            record.read(
                buffer,
                0,
                AudioProcessingConfig.CHUNK_SIZE,
                AudioRecord.READ_BLOCKING,
            )
        }.getOrElse {
            AudioRecord.ERROR_INVALID_OPERATION
        }

        private suspend fun processAudioChunk(buffer: ShortArray, readCount: Int) {
            writeWavChunk(buffer, readCount)
            soundDetectionWindowFanout.processPcm16(buffer, readCount)
            if (spectralAnalysisEnabled) {
                val timestamp = System.currentTimeMillis()
                _spectralFrame.value = spectralAnalyzer.analyze(buffer, readCount, timestamp)
                _rtaFrame.value =
                    rtaCalculator.analyze(
                        buffer = buffer,
                        size = readCount,
                        resolution = RtaResolution.OCTAVE,
                        timestamp = timestamp,
                    )
            }
            val weightedBuffer = weightingFilter.applyWeighting(buffer, readCount, currentWeighting)
            val aWeightedBuffer = aWeightingFilter.applyWeighting(buffer, readCount, WeightingType.A)
            val cWeightedPeakBuffer = cPeakWeightingFilter.applyWeighting(buffer, readCount, WeightingType.C)
            _decibelFlow.emit(
                smoothResponseTime(
                    DecibelReading(
                        instantDb = decibelCalculator.calculateDb(buffer, readCount, calibrationOffset),
                        weightedDb = decibelCalculator.calculateDb(weightedBuffer, readCount, calibrationOffset),
                        aWeightedDb = decibelCalculator.calculateDb(aWeightedBuffer, readCount, calibrationOffset),
                        timestamp = System.currentTimeMillis(),
                        peakAmplitude = decibelCalculator.findPeakAmplitude(buffer, readCount),
                        peakDb = decibelCalculator.calculatePeakDb(cWeightedPeakBuffer, readCount, calibrationOffset),
                    ),
                ),
            )
        }

        private fun smoothResponseTime(reading: DecibelReading): DecibelReading = synchronized(responseTimeLock) {
            responseTimeProcessor.process(reading)
        }

        private fun resetResponseTimeSmoothing() {
            synchronized(responseTimeLock) {
                responseTimeProcessor.reset()
            }
        }

        private fun writeWavChunk(buffer: ShortArray, readCount: Int) {
            synchronized(wavRecordingLock) {
                wavWriter?.writePcm16(buffer, readCount)
            }
        }

        fun stopRecording() {
            isRecording = false
            releaseCurrentRecord()
            _audioInputInfo.value = AudioInputInfo()
            _spectralFrame.value = null
            _rtaFrame.value = null
            soundDetectionWindowFanout.setEnabled(false)
        }

        private fun releaseCurrentRecord(expectedRecord: AudioRecord? = null) {
            val recordToRelease =
                synchronized(audioRecordLock) {
                    val current = audioRecord
                    if (expectedRecord != null && current !== expectedRecord) {
                        null
                    } else {
                        audioRecord = null
                        current
                    }
                }
            recordToRelease?.let(::releaseAudioRecord)
        }

        private fun releaseAudioRecord(record: AudioRecord) {
            val recordingState =
                runCatching {
                    record.recordingState
                }.getOrDefault(AudioRecord.RECORDSTATE_STOPPED)
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                runCatching {
                    record.stop()
                }
            }
            runCatching {
                record.release()
            }
        }
    }
