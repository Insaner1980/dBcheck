package com.dbcheck.app.domain.noise

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

data class DosimeterExposure(
    val standard: DosimeterStandard,
    val twaDb: Float,
    val dosePercent: Float,
    val projectedDosePercent: Float,
    val remainingExposureMs: Long?,
)

object DosimeterCalculator {
    fun calculate(standard: DosimeterStandard, laeqDb: Float, durationMs: Long): DosimeterExposure {
        val policy = policyFor(standard)
        val durationHours = durationMs / MILLIS_PER_HOUR
        if (durationHours <= 0.0 || laeqDb <= 0f || laeqDb < policy.thresholdDb) {
            return DosimeterExposure(
                standard = standard,
                twaDb = 0f,
                dosePercent = 0f,
                projectedDosePercent = 0f,
                remainingExposureMs = null,
            )
        }

        val allowableHours =
            policy.referenceHours *
                2.0.pow((policy.criterionDb - laeqDb) / policy.exchangeRateDb)
        val dosePercent = durationHours / allowableHours * 100.0
        val projectedDosePercent = dosePercent * (policy.referenceHours / durationHours)
        val remainingExposureMs =
            ((allowableHours - durationHours).coerceAtLeast(0.0) * MILLIS_PER_HOUR).roundToLong()

        return DosimeterExposure(
            standard = standard,
            twaDb = twaDbForDose(policy, dosePercent),
            dosePercent = dosePercent.toFloat(),
            projectedDosePercent = projectedDosePercent.toFloat(),
            remainingExposureMs = remainingExposureMs,
        )
    }

    private fun policyFor(standard: DosimeterStandard): DosimeterPolicy = when (standard) {
        DosimeterStandard.NIOSH_REL ->
            DosimeterPolicy(
                criterionDb = 85.0,
                exchangeRateDb = 3.0,
                thresholdDb = 80.0,
            )

        DosimeterStandard.OSHA_PEL ->
            DosimeterPolicy(
                criterionDb = 90.0,
                exchangeRateDb = 5.0,
                thresholdDb = 90.0,
            )
    }

    private fun twaDbForDose(policy: DosimeterPolicy, dosePercent: Double): Float = if (dosePercent <= 0.0) {
        0f
    } else {
        (policy.criterionDb + policy.exchangeRateDb * log2(dosePercent / 100.0))
            .toFloat()
            .coerceAtLeast(0f)
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)

    private data class DosimeterPolicy(
        val criterionDb: Double,
        val exchangeRateDb: Double,
        val thresholdDb: Double,
        val referenceHours: Double = REFERENCE_HOURS,
    )

    private const val REFERENCE_HOURS = 8.0
    private const val MILLIS_PER_HOUR = 60.0 * 60.0 * 1000.0
}
