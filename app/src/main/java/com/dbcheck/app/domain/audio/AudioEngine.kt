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
            private const val BUFFER_SIZE_FACTOR = 2
        }

        private val _decibelFlow = MutableSharedFlow<DecibelReading>(replay = 1)
        val decibelFlow: SharedFlow<DecibelReading> = _decibelFlow
        private val _spectralFrame = MutableStateFlow<SpectralFrame?>(null)
        val spectralFrame: StateFlow<SpectralFrame?> = _spectralFrame

        private var audioRecord: AudioRecord? = null

        @Volatile
        private var isRecording = false

        @Volatile
        private var currentWeighting = WeightingType.DEFAULT

        @Volatile
        private var spectralAnalysisEnabled = false
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

        suspend fun startRecording() =
            withContext(defaultDispatcher) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext
                }

                val record = createAudioRecord() ?: return@withContext

                audioRecord = record
                record.startRecording()
                isRecording = true
                weightingFilter.reset()

                recordLoop(record)
            }

        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        private fun createAudioRecord(): AudioRecord? {
            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    AudioProcessingConfig.SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                )
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                return null
            }

            val record =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AudioProcessingConfig.SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    minBufferSize * BUFFER_SIZE_FACTOR,
                )

            return if (record.state == AudioRecord.STATE_INITIALIZED) {
                record
            } else {
                record.release()
                null
            }
        }

        private suspend fun recordLoop(record: AudioRecord) {
            val buffer = ShortArray(AudioProcessingConfig.CHUNK_SIZE)

            while (isRecording) {
                kotlin.coroutines.coroutineContext.ensureActive()
                val readCount = record.read(buffer, 0, AudioProcessingConfig.CHUNK_SIZE)
                if (readCount > 0) {
                    processAudioChunk(buffer, readCount)
                }
            }
        }

        private suspend fun processAudioChunk(
            buffer: ShortArray,
            readCount: Int,
        ) {
            if (spectralAnalysisEnabled) {
                _spectralFrame.value = spectralAnalyzer.analyze(buffer, readCount)
            }
            val weightedBuffer = weightingFilter.applyWeighting(buffer, readCount, currentWeighting)
            _decibelFlow.emit(
                DecibelReading(
                    instantDb = decibelCalculator.calculateDb(buffer, readCount, calibrationOffset),
                    weightedDb = decibelCalculator.calculateDb(weightedBuffer, readCount, calibrationOffset),
                    timestamp = System.currentTimeMillis(),
                    peakAmplitude = decibelCalculator.findPeakAmplitude(buffer, readCount),
                ),
            )
        }

        fun stopRecording() {
            isRecording = false
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
            audioRecord = null
            _spectralFrame.value = null
        }
    }
