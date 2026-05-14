package com.dbcheck.app.domain.audio

import android.media.AudioRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioRecordReadPolicyTest {
    @Test
    fun positiveReadCountIsProcessable() {
        assertEquals(
            AudioRecordReadAction.Process,
            AudioRecordReadPolicy.actionFor(readCount = 512, recordingActive = true),
        )
    }

    @Test
    fun zeroReadCountContinuesWhileRecordingIsActive() {
        assertEquals(
            AudioRecordReadAction.Continue,
            AudioRecordReadPolicy.actionFor(readCount = 0, recordingActive = true),
        )
    }

    @Test
    fun audioRecordErrorsFailWhileRecordingIsActive() {
        assertEquals(
            AudioRecordReadAction.Fail(AudioRecord.ERROR_DEAD_OBJECT),
            AudioRecordReadPolicy.actionFor(
                readCount = AudioRecord.ERROR_DEAD_OBJECT,
                recordingActive = true,
            ),
        )
    }

    @Test
    fun readResultAfterStopCompletesWithoutFailure() {
        assertEquals(
            AudioRecordReadAction.Stop,
            AudioRecordReadPolicy.actionFor(
                readCount = AudioRecord.ERROR_INVALID_OPERATION,
                recordingActive = false,
            ),
        )
    }
}
