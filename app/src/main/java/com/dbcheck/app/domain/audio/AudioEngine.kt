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
import javax.inject.Inject
import javax.inject.Singleton

data class DecibelReading(
    val instantDb: Float,
    val weightedDb: Float,
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

        private val audioRecordLock = Any()
        private var audioRecord: AudioRecord? = null
        private val cPeakWeightingFilter = FrequencyWeightingFilter()

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

        fun setSpectralAnalysisEnabled(enabled: Boolean) {
            spectralAnalysisEnabled = enabled
            if (!enabled) {
                _spectralFrame.value = null
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

                    isRecording = true
                    weightingFilter.reset()
                    cPeakWeightingFilter.reset()
                    onRecordingStarted()
                    recordLoop(record)
                } finally {
                    isRecording = false
                    releaseCurrentRecord(record)
                    _spectralFrame.value = null
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
            if (spectralAnalysisEnabled) {
                _spectralFrame.value = spectralAnalyzer.analyze(buffer, readCount)
            }
            val weightedBuffer = weightingFilter.applyWeighting(buffer, readCount, currentWeighting)
            val cWeightedPeakBuffer = cPeakWeightingFilter.applyWeighting(buffer, readCount, WeightingType.C)
            _decibelFlow.emit(
                DecibelReading(
                    instantDb = decibelCalculator.calculateDb(buffer, readCount, calibrationOffset),
                    weightedDb = decibelCalculator.calculateDb(weightedBuffer, readCount, calibrationOffset),
                    timestamp = System.currentTimeMillis(),
                    peakAmplitude = decibelCalculator.findPeakAmplitude(buffer, readCount),
                    peakDb = decibelCalculator.calculatePeakDb(cWeightedPeakBuffer, readCount, calibrationOffset),
                ),
            )
        }

        fun stopRecording() {
            isRecording = false
            releaseCurrentRecord()
            _spectralFrame.value = null
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
