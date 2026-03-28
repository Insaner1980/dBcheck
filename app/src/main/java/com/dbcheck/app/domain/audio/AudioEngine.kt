package com.dbcheck.app.domain.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
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
    ) {
        companion object {
            private const val SAMPLE_RATE = 44100
            private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
            private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
            private const val BUFFER_SIZE_FACTOR = 2
            private const val READ_CHUNK_SIZE = 4096
        }

        private val _decibelFlow = MutableSharedFlow<DecibelReading>(replay = 1)
        val decibelFlow: SharedFlow<DecibelReading> = _decibelFlow

        private var audioRecord: AudioRecord? = null

        @Volatile
        private var isRecording = false

        private var currentWeighting = WeightingType.A
        private var calibrationOffset = 0f

        fun setWeighting(weighting: WeightingType) {
            currentWeighting = weighting
            weightingFilter.reset()
        }

        fun setCalibrationOffset(offset: Float) {
            calibrationOffset = offset
        }

        suspend fun startRecording() =
            withContext(Dispatchers.Default) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext
                }

                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    return@withContext
                }

                val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR

                audioRecord =
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize,
                    ).also { record ->
                        if (record.state != AudioRecord.STATE_INITIALIZED) {
                            record.release()
                            audioRecord = null
                            return@withContext
                        }

                        record.startRecording()
                        isRecording = true
                        weightingFilter.reset()

                        val buffer = ShortArray(READ_CHUNK_SIZE)

                        while (isActive && isRecording) {
                            val readCount = record.read(buffer, 0, READ_CHUNK_SIZE)
                            if (readCount > 0) {
                                val weightedBuffer =
                                    weightingFilter.applyWeighting(
                                        buffer,
                                        readCount,
                                        currentWeighting,
                                    )
                                val instantDb = decibelCalculator.calculateDb(buffer, readCount, calibrationOffset)
                                val weightedDb = decibelCalculator.calculateDb(weightedBuffer, readCount, calibrationOffset)
                                val peakAmplitude = decibelCalculator.findPeakAmplitude(buffer, readCount)

                                _decibelFlow.emit(
                                    DecibelReading(
                                        instantDb = instantDb,
                                        weightedDb = weightedDb,
                                        timestamp = System.currentTimeMillis(),
                                        peakAmplitude = peakAmplitude,
                                    ),
                                )
                            }
                        }
                    }
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
        }
    }
