package com.dbcheck.app.domain.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.sqrt

@Singleton
class FFTProcessor @Inject constructor() {

    companion object {
        const val FFT_SIZE = 4096
    }

    fun process(buffer: ShortArray, size: Int): FloatArray {
        val n = FFT_SIZE.coerceAtMost(size)

        // Apply Hann window and convert to double
        val real = DoubleArray(n)
        val imag = DoubleArray(n)
        for (i in 0 until n) {
            val window = 0.5 * (1 - cos(2 * PI * i / (n - 1)))
            real[i] = buffer[i].toDouble() * window
            imag[i] = 0.0
        }

        // Cooley-Tukey FFT (in-place, radix-2, DIT)
        fft(real, imag, n)

        // Compute magnitude spectrum (first half only - Nyquist)
        val magnitudes = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i]).toFloat()
        }

        return magnitudes
    }

    fun findDominantFrequency(magnitudes: FloatArray, sampleRate: Int = 44100): Float {
        if (magnitudes.isEmpty()) return 0f
        val maxIndex = magnitudes.indices.maxByOrNull { magnitudes[it] } ?: 0
        return maxIndex.toFloat() * sampleRate / (magnitudes.size * 2)
    }

    fun getBandwidth(magnitudes: FloatArray, sampleRate: Int = 44100): String {
        if (magnitudes.isEmpty()) return "Unknown"
        val threshold = (magnitudes.maxOrNull() ?: 0f) * 0.5f
        val activeBins = magnitudes.count { it > threshold }
        val bandwidthHz = activeBins.toFloat() * sampleRate / (magnitudes.size * 2)
        return when {
            bandwidthHz < 1000 -> "Narrow"
            bandwidthHz < 5000 -> "Medium"
            else -> "Wide"
        }
    }

    private fun fft(real: DoubleArray, imag: DoubleArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                var temp = real[i]; real[i] = real[j]; real[j] = temp
                temp = imag[i]; imag[i] = imag[j]; imag[j] = temp
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        // Butterfly computations
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            for (i in 0 until n step len) {
                for (k in 0 until halfLen) {
                    val tReal = cos(angle * k) * real[i + k + halfLen] - kotlin.math.sin(angle * k) * imag[i + k + halfLen]
                    val tImag = kotlin.math.sin(angle * k) * real[i + k + halfLen] + cos(angle * k) * imag[i + k + halfLen]
                    real[i + k + halfLen] = real[i + k] - tReal
                    imag[i + k + halfLen] = imag[i + k] - tImag
                    real[i + k] += tReal
                    imag[i + k] += tImag
                }
            }
            len *= 2
        }
    }
}
