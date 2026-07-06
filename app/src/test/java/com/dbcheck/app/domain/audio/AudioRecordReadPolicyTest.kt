package com.dbcheck.app.domain.audio

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
            AudioRecordReadAction.Fail(READ_ERROR_CODE),
            AudioRecordReadPolicy.actionFor(
                readCount = READ_ERROR_CODE,
                recordingActive = true,
            ),
        )
    }

    @Test
    fun readResultAfterStopCompletesWithoutFailure() {
        assertEquals(
            AudioRecordReadAction.Stop,
            AudioRecordReadPolicy.actionFor(
                readCount = READ_ERROR_CODE,
                recordingActive = false,
            ),
        )
    }

    private companion object {
        const val READ_ERROR_CODE = -6
    }
}
