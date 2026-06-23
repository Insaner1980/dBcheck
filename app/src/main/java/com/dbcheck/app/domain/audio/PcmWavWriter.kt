package com.dbcheck.app.domain.audio

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min

class PcmWavWriter private constructor(
    private val file: File,
    private val output: RandomAccessFile,
    private val sampleRateHz: Int,
    private val channelCount: Int,
    private val bitsPerSample: Int,
) : Closeable {
    private val writeBuffer = ByteArray(DEFAULT_WRITE_BUFFER_SAMPLES * PCM16_BYTES_PER_SAMPLE)
    private val blockAlign = channelCount * bitsPerSample / Byte.SIZE_BITS
    private var dataBytesWritten = 0L
    private var closed = false

    val durationMs: Long
        get() {
            val samplesWritten = dataBytesWritten / blockAlign
            return samplesWritten * MILLIS_PER_SECOND / sampleRateHz
        }

    fun writePcm16(samples: ShortArray, size: Int = samples.size) {
        check(!closed) { "WAV writer is closed" }
        require(size in 0..samples.size) { "Invalid PCM sample count: $size" }

        var offset = 0
        while (offset < size) {
            val sampleCount = min(size - offset, writeBuffer.size / PCM16_BYTES_PER_SAMPLE)
            copyPcm16ToLittleEndianBytes(samples, offset, sampleCount)
            output.write(writeBuffer, 0, sampleCount * PCM16_BYTES_PER_SAMPLE)
            dataBytesWritten += sampleCount * PCM16_BYTES_PER_SAMPLE
            offset += sampleCount
        }
    }

    override fun close() {
        if (closed) return
        writeHeader(dataBytesWritten)
        closed = true
        output.close()
    }

    fun abort() {
        if (!closed) {
            closed = true
            output.close()
        }
        if (!file.delete() && file.exists()) {
            file.deleteOnExit()
        }
    }

    private fun copyPcm16ToLittleEndianBytes(samples: ShortArray, offset: Int, sampleCount: Int) {
        repeat(sampleCount) { index ->
            val sample = samples[offset + index].toInt()
            val byteIndex = index * PCM16_BYTES_PER_SAMPLE
            writeBuffer[byteIndex] = sample.toByte()
            writeBuffer[byteIndex + 1] = (sample ushr Byte.SIZE_BITS).toByte()
        }
    }

    private fun writeHeader(dataSizeBytes: Long) {
        output.seek(0L)
        output.write(wavHeader(dataSizeBytes))
        output.seek(PCM_WAV_HEADER_BYTES + dataSizeBytes)
    }

    private fun wavHeader(dataSizeBytes: Long): ByteArray = ByteArray(PCM_WAV_HEADER_BYTES).also { header ->
            header.writeAscii(offset = 0, value = RIFF_CHUNK_ID)
            header.writeIntLe(offset = 4, value = RIFF_BASE_SIZE_BYTES + dataSizeBytes)
            header.writeAscii(offset = 8, value = WAVE_FORMAT)
            header.writeAscii(offset = 12, value = FORMAT_CHUNK_ID)
            header.writeIntLe(offset = 16, value = FORMAT_CHUNK_SIZE_BYTES)
            header.writeShortLe(offset = 20, value = PCM_AUDIO_FORMAT)
            header.writeShortLe(offset = 22, value = channelCount)
            header.writeIntLe(offset = 24, value = sampleRateHz)
            header.writeIntLe(offset = 28, value = sampleRateHz * blockAlign)
            header.writeShortLe(offset = 32, value = blockAlign)
            header.writeShortLe(offset = 34, value = bitsPerSample)
            header.writeAscii(offset = 36, value = DATA_CHUNK_ID)
            header.writeIntLe(offset = 40, value = dataSizeBytes)
        }

    private fun ByteArray.writeAscii(offset: Int, value: String) {
        val bytes = value.toByteArray(Charsets.US_ASCII)
        bytes.copyInto(this, destinationOffset = offset)
    }

    private fun ByteArray.writeIntLe(offset: Int, value: Long) {
        this[offset] = value.toByte()
        this[offset + 1] = (value ushr 8).toByte()
        this[offset + 2] = (value ushr 16).toByte()
        this[offset + 3] = (value ushr 24).toByte()
    }

    private fun ByteArray.writeIntLe(offset: Int, value: Int) {
        writeIntLe(offset, value.toLong())
    }

    private fun ByteArray.writeShortLe(offset: Int, value: Int) {
        this[offset] = value.toByte()
        this[offset + 1] = (value ushr 8).toByte()
    }

    companion object {
        private const val DEFAULT_WRITE_BUFFER_SAMPLES = 4_096
        private const val PCM16_BYTES_PER_SAMPLE = 2
        private const val PCM_WAV_HEADER_BYTES = 44
        private const val RIFF_BASE_SIZE_BYTES = 36
        private const val FORMAT_CHUNK_SIZE_BYTES = 16
        private const val PCM_AUDIO_FORMAT = 1
        private const val MILLIS_PER_SECOND = 1_000L
        private const val RIFF_CHUNK_ID = "RIFF"
        private const val WAVE_FORMAT = "WAVE"
        private const val FORMAT_CHUNK_ID = "fmt "
        private const val DATA_CHUNK_ID = "data"

        fun create(
            file: File,
            sampleRateHz: Int = AudioProcessingConfig.SAMPLE_RATE,
            channelCount: Int = 1,
            bitsPerSample: Int = 16,
        ): PcmWavWriter {
            require(sampleRateHz > 0) { "Sample rate must be positive" }
            require(channelCount > 0) { "Channel count must be positive" }
            require(bitsPerSample == 16) { "Only PCM16 WAV output is supported" }
            file.parentFile?.mkdirs()
            val output = RandomAccessFile(file, "rw")
            output.setLength(0L)
            return PcmWavWriter(
                file = file,
                output = output,
                sampleRateHz = sampleRateHz,
                channelCount = channelCount,
                bitsPerSample = bitsPerSample,
            ).also { writer ->
                writer.writeHeader(dataSizeBytes = 0L)
            }
        }
    }
}
