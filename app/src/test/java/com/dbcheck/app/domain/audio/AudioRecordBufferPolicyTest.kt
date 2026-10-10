package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecordBufferPolicyTest {
    @Test
    fun invalidMinBufferResultDoesNotCreateCaptureBuffer() {
        assertNull(AudioRecordBufferPolicy.captureBufferSizeBytes(0))
        assertNull(AudioRecordBufferPolicy.captureBufferSizeBytes(-1))
        assertNull(AudioRecordBufferPolicy.captureBufferSizeBytes(-2))
    }

    @Test
    fun bufferHeadroomOverflowIsRejected() {
        assertNull(AudioRecordBufferPolicy.captureBufferSizeBytes(Int.MAX_VALUE))
        assertNull(AudioRecordBufferPolicy.captureBufferSizeBytes(Int.MAX_VALUE / 2 + 1))
        assertEquals(Int.MAX_VALUE - 1, AudioRecordBufferPolicy.captureBufferSizeBytes(Int.MAX_VALUE / 2))
    }

    @Test
    fun captureBufferIsLargerThanReadChunkWhenDeviceMinimumIsSmall() {
        val bufferSize = AudioRecordBufferPolicy.captureBufferSizeBytes(minBufferSizeBytes = 1024)

        assertEquals(AudioRecordBufferPolicy.readChunkSizeBytes * 2, bufferSize)
    }

    @Test
    fun captureBufferKeepsHeadroomWhenDeviceMinimumIsLarge() {
        val deviceMinimum = AudioRecordBufferPolicy.readChunkSizeBytes * 3
        val bufferSize = AudioRecordBufferPolicy.captureBufferSizeBytes(deviceMinimum)

        assertTrue(bufferSize!! > deviceMinimum)
    }
}
