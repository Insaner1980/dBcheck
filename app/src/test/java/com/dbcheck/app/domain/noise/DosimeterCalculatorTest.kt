package com.dbcheck.app.domain.noise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DosimeterCalculatorTest {
    @Test
    fun nioshReferenceExposureReachesFullDoseAtEightyFiveDbaForEightHours() {
        val exposure =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.NIOSH_REL,
                laeqDb = 85f,
                durationMs = EIGHT_HOURS_MS,
            )

        assertEquals(DosimeterStandard.NIOSH_REL, exposure.standard)
        assertEquals(100f, exposure.dosePercent, 0.1f)
        assertEquals(85f, exposure.twaDb, 0.1f)
        assertEquals(100f, exposure.projectedDosePercent, 0.1f)
        assertEquals(0L, exposure.remainingExposureMs)
    }

    @Test
    fun nioshShortHighExposureMatchesExistingReportMath() {
        val exposure =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.NIOSH_REL,
                laeqDb = 88f,
                durationMs = FOUR_HOURS_MS,
            )

        assertEquals(100f, exposure.dosePercent, 0.1f)
        assertEquals(85f, exposure.twaDb, 0.1f)
        assertEquals(200f, exposure.projectedDosePercent, 0.1f)
        assertEquals(0L, exposure.remainingExposureMs)
    }

    @Test
    fun oshaReferenceExposureReachesFullDoseAtNinetyDbaForEightHours() {
        val exposure =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.OSHA_PEL,
                laeqDb = 90f,
                durationMs = EIGHT_HOURS_MS,
            )

        assertEquals(DosimeterStandard.OSHA_PEL, exposure.standard)
        assertEquals(100f, exposure.dosePercent, 0.1f)
        assertEquals(90f, exposure.twaDb, 0.1f)
        assertEquals(100f, exposure.projectedDosePercent, 0.1f)
        assertEquals(0L, exposure.remainingExposureMs)
    }

    @Test
    fun oshaUsesPelThresholdAndFiveDbExchangeRate() {
        val belowPel =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.OSHA_PEL,
                laeqDb = 88f,
                durationMs = FOUR_HOURS_MS,
            )
        val highPel =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.OSHA_PEL,
                laeqDb = 95f,
                durationMs = FOUR_HOURS_MS,
            )

        assertEquals(0f, belowPel.dosePercent, 0.1f)
        assertEquals(0f, belowPel.twaDb, 0.1f)
        assertNull(belowPel.remainingExposureMs)
        assertEquals(100f, highPel.dosePercent, 0.1f)
        assertEquals(90f, highPel.twaDb, 0.1f)
        assertEquals(0L, highPel.remainingExposureMs)
    }

    @Test
    fun projectedDoseAssumesEightHourReferenceShift() {
        val exposure =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.NIOSH_REL,
                laeqDb = 85f,
                durationMs = TWO_HOURS_MS,
            )

        assertEquals(25f, exposure.dosePercent, 0.1f)
        assertEquals(79f, exposure.twaDb, 0.1f)
        assertEquals(100f, exposure.projectedDosePercent, 0.1f)
        assertEquals(SIX_HOURS_MS, exposure.remainingExposureMs)
    }

    @Test
    fun quietOrZeroDurationReturnsZeroExposure() {
        val quiet =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.NIOSH_REL,
                laeqDb = 79f,
                durationMs = EIGHT_HOURS_MS,
            )
        val zeroDuration =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.NIOSH_REL,
                laeqDb = 85f,
                durationMs = 0L,
            )

        assertEquals(0f, quiet.dosePercent, 0.1f)
        assertEquals(0f, quiet.twaDb, 0.1f)
        assertEquals(0f, quiet.projectedDosePercent, 0.1f)
        assertNull(quiet.remainingExposureMs)
        assertEquals(0f, zeroDuration.dosePercent, 0.1f)
        assertEquals(0f, zeroDuration.twaDb, 0.1f)
        assertEquals(0f, zeroDuration.projectedDosePercent, 0.1f)
        assertNull(zeroDuration.remainingExposureMs)
    }

    @Test
    fun hardExposureClampsRemainingTimeToZero() {
        val exposure =
            DosimeterCalculator.calculate(
                standard = DosimeterStandard.NIOSH_REL,
                laeqDb = 100f,
                durationMs = ONE_HOUR_MS,
            )

        assertEquals(400f, exposure.dosePercent, 0.1f)
        assertEquals(91f, exposure.twaDb, 0.1f)
        assertEquals(3_200f, exposure.projectedDosePercent, 0.1f)
        assertEquals(0L, exposure.remainingExposureMs)
    }

    private companion object {
        const val ONE_HOUR_MS = 60 * 60 * 1_000L
        const val TWO_HOURS_MS = 2 * ONE_HOUR_MS
        const val FOUR_HOURS_MS = 4 * ONE_HOUR_MS
        const val SIX_HOURS_MS = 6 * ONE_HOUR_MS
        const val EIGHT_HOURS_MS = 8 * ONE_HOUR_MS
    }
}
