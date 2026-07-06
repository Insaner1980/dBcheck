package com.dbcheck.app.domain.tinnitus

import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.HearingTestPolicy
import kotlin.math.roundToInt

data class TinnitusPitchProfile(
    val leftFrequencyHz: Float? = null,
    val rightFrequencyHz: Float? = null,
    val updatedAtMs: Long? = null,
) {
    val hasSavedPitch: Boolean
        get() = leftFrequencyHz != null || rightFrequencyHz != null

    fun frequencyFor(ear: Ear): Float? = when (ear) {
        Ear.LEFT -> leftFrequencyHz
        Ear.RIGHT -> rightFrequencyHz
    }

    fun withFrequency(ear: Ear, frequencyHz: Float, updatedAtMs: Long): TinnitusPitchProfile {
        val normalizedFrequency = TinnitusPitchPolicy.normalizeFrequencyHz(frequencyHz)
        return when (ear) {
            Ear.LEFT -> copy(leftFrequencyHz = normalizedFrequency, updatedAtMs = updatedAtMs.takeIf { it > 0L })
            Ear.RIGHT -> copy(rightFrequencyHz = normalizedFrequency, updatedAtMs = updatedAtMs.takeIf { it > 0L })
        }
    }
}

object TinnitusPitchPolicy {
    val MIN_FREQUENCY_HZ = HearingTestPolicy.MIN_FREQUENCY_HZ
    val MAX_FREQUENCY_HZ = HearingTestPolicy.MAX_FREQUENCY_HZ
    const val DEFAULT_FREQUENCY_HZ = 1_000f
    const val PREVIEW_AMPLITUDE_DB = -36f
    const val FREQUENCY_STEP_HZ = 50f

    fun normalizeFrequencyHz(frequencyHz: Float): Float {
        val finiteFrequency = frequencyHz.takeIf { it.isFinite() } ?: DEFAULT_FREQUENCY_HZ
        val clamped = finiteFrequency.coerceIn(MIN_FREQUENCY_HZ, MAX_FREQUENCY_HZ)
        return (clamped / FREQUENCY_STEP_HZ)
            .roundToInt()
            .times(FREQUENCY_STEP_HZ)
            .coerceIn(MIN_FREQUENCY_HZ, MAX_FREQUENCY_HZ)
    }

    fun normalizeStoredFrequencyHz(frequencyHz: Float?): Float? =
        frequencyHz?.takeIf { it.isFinite() }?.let(::normalizeFrequencyHz)

    fun normalizeUpdatedAtMs(updatedAtMs: Long?): Long? = updatedAtMs?.takeIf { it > 0L }
}
