package com.dbcheck.app.service

import android.media.AudioRecord

internal object AudioRecordReleasePolicy {
    fun release(record: AudioRecord) {
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
