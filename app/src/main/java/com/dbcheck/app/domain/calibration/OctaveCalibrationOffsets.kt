package com.dbcheck.app.domain.calibration

import com.dbcheck.app.domain.audio.RtaResolution
import java.util.Locale
import kotlin.math.abs

class OctaveCalibrationOffsets private constructor(private val offsetsByCenterFrequencyHz: Map<Float, Float>) {
    val isZero: Boolean
        get() = offsetsByCenterFrequencyHz.isEmpty()

    fun offsetFor(centerFrequencyHz: Float): Float {
        val supportedCenter =
            centerFrequencyHz.supportedCenterFrequencyOrNull() ?: return CalibrationOffsetPolicy.DEFAULT_OFFSET_DB
        return offsetsByCenterFrequencyHz[supportedCenter]
            ?: CalibrationOffsetPolicy.DEFAULT_OFFSET_DB
    }

    fun withOffset(centerFrequencyHz: Float, offsetDb: Float): OctaveCalibrationOffsets {
        val supportedCenter = centerFrequencyHz.supportedCenterFrequencyOrNull() ?: return this
        val normalizedOffset = CalibrationOffsetPolicy.normalizeOffsetDb(offsetDb)
        val updatedOffsets =
            if (normalizedOffset == CalibrationOffsetPolicy.DEFAULT_OFFSET_DB) {
                offsetsByCenterFrequencyHz - supportedCenter
            } else {
                offsetsByCenterFrequencyHz + (supportedCenter to normalizedOffset)
            }
        return OctaveCalibrationOffsets(updatedOffsets)
    }

    fun resetToZero(): OctaveCalibrationOffsets = zero()

    fun toStorageString(): String = supportedCenterFrequenciesHz
            .mapNotNull { centerFrequencyHz ->
                val offsetDb = offsetsByCenterFrequencyHz[centerFrequencyHz] ?: return@mapNotNull null
                "${centerFrequencyHz.storageKey()}=${offsetDb.storageValue()}"
            }.joinToString(separator = STORAGE_SEPARATOR.toString())

    companion object {
        const val STORAGE_ZERO = ""

        val supportedCenterFrequenciesHz: List<Float> = RtaResolution.OCTAVE.centerFrequenciesHz()

        private val zero = OctaveCalibrationOffsets(emptyMap())

        fun zero(): OctaveCalibrationOffsets = zero

        fun fromStorageString(value: String?): OctaveCalibrationOffsets {
            if (value.isNullOrBlank()) return zero()
            return value
                .split(STORAGE_SEPARATOR)
                .fold(zero()) { offsets, entry ->
                    val keyValue = entry.split(STORAGE_KEY_VALUE_SEPARATOR, limit = 2)
                    if (keyValue.size != 2) {
                        offsets
                    } else {
                        val centerFrequencyHz = keyValue[0].toFloatOrNull()
                        val offsetDb = keyValue[1].toFloatOrNull()
                        if (centerFrequencyHz == null || offsetDb == null || !offsetDb.isFinite()) {
                            offsets
                        } else {
                            offsets.withOffset(centerFrequencyHz, offsetDb)
                        }
                    }
                }
        }
    }
}

private fun Float.supportedCenterFrequencyOrNull(): Float? {
    if (!isFinite()) return null
    return OctaveCalibrationOffsets.supportedCenterFrequenciesHz
        .minByOrNull { supportedCenter -> abs(supportedCenter - this) }
        ?.takeIf { supportedCenter ->
            abs(supportedCenter - this) <= supportedCenter * CENTER_FREQUENCY_TOLERANCE_RATIO
        }
}

private fun Float.storageKey(): String = String.format(Locale.US, "%.2f", this)

private fun Float.storageValue(): String = String.format(Locale.US, "%.4f", this)

private const val CENTER_FREQUENCY_TOLERANCE_RATIO = 0.02f
private const val STORAGE_SEPARATOR = ';'
private const val STORAGE_KEY_VALUE_SEPARATOR = '='
