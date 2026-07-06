package com.dbcheck.app.domain.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecordStartPolicyTest {
    @Test
    fun recordingStateConfirmsAudioRecordStart() {
        assertTrue(
            AudioRecordStartPolicy.hasStarted(
                recordingState = RECORDSTATE_RECORDING,
                expectedRecordingState = RECORDSTATE_RECORDING,
            ),
        )
    }

    @Test
    fun stoppedStateRejectsAudioRecordStart() {
        assertFalse(
            AudioRecordStartPolicy.hasStarted(
                recordingState = RECORDSTATE_STOPPED,
                expectedRecordingState = RECORDSTATE_RECORDING,
            ),
        )
    }

    private companion object {
        const val RECORDSTATE_STOPPED = 1
        const val RECORDSTATE_RECORDING = 3
    }
}
