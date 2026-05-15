package com.dbcheck.app.domain.audio

import android.media.AudioRecord

sealed interface AudioRecordingResult {
    data object Stopped : AudioRecordingResult

    data class Failed(val failure: AudioRecordingFailure) : AudioRecordingResult
}

sealed interface AudioRecordingFailure {
    data object PermissionDenied : AudioRecordingFailure
    data object CreationFailed : AudioRecordingFailure
    data object StartFailed : AudioRecordingFailure

    data class ReadFailed(val errorCode: Int) : AudioRecordingFailure
}

internal object AudioRecordBufferPolicy {
    private const val BYTES_PER_PCM16_SAMPLE = 2
    private const val BUFFER_HEADROOM_FACTOR = 2

    val readChunkSizeBytes: Int = AudioProcessingConfig.CHUNK_SIZE * BYTES_PER_PCM16_SAMPLE

    fun captureBufferSizeBytes(minBufferSizeBytes: Int): Int? {
        if (minBufferSizeBytes <= 0 ||
            minBufferSizeBytes == AudioRecord.ERROR ||
            minBufferSizeBytes == AudioRecord.ERROR_BAD_VALUE
        ) {
            return null
        }
        return maxOf(minBufferSizeBytes, readChunkSizeBytes) * BUFFER_HEADROOM_FACTOR
    }
}

internal sealed interface AudioRecordReadAction {
    data object Process : AudioRecordReadAction
    data object Continue : AudioRecordReadAction
    data object Stop : AudioRecordReadAction

    data class Fail(val errorCode: Int) : AudioRecordReadAction
}

internal object AudioRecordReadPolicy {
    fun actionFor(readCount: Int, recordingActive: Boolean): AudioRecordReadAction {
        if (!recordingActive) return AudioRecordReadAction.Stop
        return when {
            readCount > 0 -> AudioRecordReadAction.Process
            readCount == 0 -> AudioRecordReadAction.Continue
            else -> AudioRecordReadAction.Fail(readCount)
        }
    }
}
