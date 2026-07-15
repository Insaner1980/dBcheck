package com.dbcheck.app.domain.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmWavWriterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun closeWritesPcmWaveHeaderAndLittleEndianSamples() {
        val wavFile = temporaryFolder.newFile("session.wav")

        PcmWavWriter.create(wavFile, sampleRateHz = TEST_SAMPLE_RATE_HZ).use { writer ->
            writer.writePcm16(
                samples = shortArrayOf(0, Short.MAX_VALUE, Short.MIN_VALUE, -1),
                size = 4,
            )
        }

        val bytes = wavFile.readBytes()
        assertEquals(PCM_WAV_HEADER_BYTES + 8, bytes.size)
        assertEquals("RIFF", bytes.ascii(0, 4))
        assertEquals(44, bytes.intLe(4))
        assertEquals("WAVE", bytes.ascii(8, 4))
        assertEquals("fmt ", bytes.ascii(12, 4))
        assertEquals(16, bytes.intLe(16))
        assertEquals(1, bytes.shortLe(20).toInt())
        assertEquals(1, bytes.shortLe(22).toInt())
        assertEquals(TEST_SAMPLE_RATE_HZ, bytes.intLe(24))
        assertEquals(TEST_SAMPLE_RATE_HZ * PCM16_BYTES_PER_SAMPLE, bytes.intLe(28))
        assertEquals(PCM16_BYTES_PER_SAMPLE, bytes.shortLe(32).toInt())
        assertEquals(16, bytes.shortLe(34).toInt())
        assertEquals("data", bytes.ascii(36, 4))
        assertEquals(8, bytes.intLe(40))
        assertArrayEquals(
            byteArrayOf(0, 0, -1, 127, 0, -128, -1, -1),
            bytes.copyOfRange(PCM_WAV_HEADER_BYTES, bytes.size),
        )
    }

    @Test
    fun durationComesFromWrittenSampleCount() {
        val wavFile = temporaryFolder.newFile("duration.wav")
        val sampleCount = TEST_SAMPLE_RATE_HZ / 10

        PcmWavWriter.create(wavFile, sampleRateHz = TEST_SAMPLE_RATE_HZ).use { writer ->
            writer.writePcm16(ShortArray(sampleCount), sampleCount)

            assertEquals(100L, writer.durationMs)
        }
    }

    @Test
    fun abortDeletesPartialWavFile() {
        val wavFile = temporaryFolder.newFile("partial.wav")
        val writer = PcmWavWriter.create(wavFile, sampleRateHz = TEST_SAMPLE_RATE_HZ)

        writer.writePcm16(shortArrayOf(1, 2, 3), size = 3)
        writer.abort()

        assertFalse(wavFile.exists())
    }

    @Test
    fun closeDeletesPartialWavFileWhenHeaderFinalizationFails() {
        val wavFile = temporaryFolder.newFile("failed-finalization.wav")
        val writer = PcmWavWriter.create(wavFile, sampleRateHz = TEST_SAMPLE_RATE_HZ)
        writer.writePcm16(shortArrayOf(1, 2, 3), size = 3)
        writer.closeOutputForTest()

        val result = runCatching { writer.close() }

        assertTrue(result.isFailure)
        assertFalse(wavFile.exists())
    }

    @Test
    fun createClosesOutputWhenInitialHeaderWriteFails() {
        val wavFile = temporaryFolder.newFile("failed-initialization.wav")
        lateinit var output: FailingHeaderRandomAccessFile

        val result = runCatching {
            PcmWavWriter.create(
                file = wavFile,
                sampleRateHz = TEST_SAMPLE_RATE_HZ,
                outputFactory = { file ->
                    FailingHeaderRandomAccessFile(file).also { output = it }
                },
            )
        }

        assertTrue(result.isFailure)
        assertTrue(output.closeCalled)
    }

    @Test
    fun writeRejectsSampleCountPastArraySize() {
        val wavFile = temporaryFolder.newFile("invalid.wav")

        PcmWavWriter.create(wavFile, sampleRateHz = TEST_SAMPLE_RATE_HZ).use { writer ->
            val result = runCatching {
                writer.writePcm16(shortArrayOf(1), size = 2)
            }

            assertTrue(result.isFailure)
            assertEquals(IllegalArgumentException::class, result.exceptionOrNull()!!::class)
        }
    }

    private fun ByteArray.ascii(offset: Int, length: Int): String =
        copyOfRange(offset, offset + length).toString(Charsets.US_ASCII)

    private fun ByteArray.intLe(offset: Int): Int = ByteBuffer.wrap(this, offset, Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int

    private fun ByteArray.shortLe(offset: Int): Short = ByteBuffer.wrap(this, offset, Short.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .short

    private fun PcmWavWriter.closeOutputForTest() {
        val outputField = PcmWavWriter::class.java.getDeclaredField("output")
        outputField.isAccessible = true
        (outputField.get(this) as java.io.RandomAccessFile).close()
    }

    private class FailingHeaderRandomAccessFile(file: java.io.File) : RandomAccessFile(file, "rw") {
        var closeCalled = false

        override fun write(bytes: ByteArray) = throw IOException("Initial WAV header write failed")

        override fun close() {
            closeCalled = true
            super.close()
        }
    }

    private companion object {
        const val TEST_SAMPLE_RATE_HZ = 44_100
        const val PCM_WAV_HEADER_BYTES = 44
        const val PCM16_BYTES_PER_SAMPLE = 2
    }
}
