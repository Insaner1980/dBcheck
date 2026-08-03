package com.dbcheck.app.ui.meter.components

import com.dbcheck.app.projectFile
import com.dbcheck.app.withDefaultLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DosimeterGaugeCardTest {
    @Test
    fun doseProgressFractionClampsToGaugeRange() {
        assertEquals(0f, DosimeterGaugeFormatter.doseProgressFraction(-12f), 0.001f)
        assertEquals(0f, DosimeterGaugeFormatter.doseProgressFraction(0f), 0.001f)
        assertEquals(0.72f, DosimeterGaugeFormatter.doseProgressFraction(72f), 0.001f)
        assertEquals(1f, DosimeterGaugeFormatter.doseProgressFraction(100f), 0.001f)
        assertEquals(1f, DosimeterGaugeFormatter.doseProgressFraction(168f), 0.001f)
    }

    @Test
    fun doseRiskLevelSeparatesLowNearLimitAndOverLimit() {
        assertEquals(DosimeterGaugeRiskLevel.LOW, DosimeterGaugeFormatter.riskLevel(79.9f))
        assertEquals(DosimeterGaugeRiskLevel.NEAR_LIMIT, DosimeterGaugeFormatter.riskLevel(80f))
        assertEquals(DosimeterGaugeRiskLevel.NEAR_LIMIT, DosimeterGaugeFormatter.riskLevel(99.9f))
        assertEquals(DosimeterGaugeRiskLevel.OVER_LIMIT, DosimeterGaugeFormatter.riskLevel(100f))
    }

    @Test
    fun valuesUseExistingClockAndDecimalFormatting() {
        withDefaultLocale(Locale.US) {
            assertEquals("42%", DosimeterGaugeFormatter.percent(42.4f))
            assertEquals("88.7 dB", DosimeterGaugeFormatter.decibel(88.66f))
            assertEquals("2:00:00", DosimeterGaugeFormatter.remainingTime(7_200_000L, "N/A"))
            assertEquals("N/A", DosimeterGaugeFormatter.remainingTime(null, "N/A"))
        }
    }

    @Test
    fun unavailableStateUsesMeterStructureWithoutInventedValuesOrIdleParagraph() {
        val source =
            projectFile(
                "src/main/java/com/dbcheck/app/ui/meter/components/DosimeterGaugeCard.kt",
            ).readText()
        val unavailableCard =
            source
                .substringAfter("private fun DosimeterUnavailableCard")
                .substringBefore("private fun DosimeterMessageCard")

        assertTrue(unavailableCard.contains("DosimeterHeader(standard = standard)"))
        assertTrue(unavailableCard.contains("DosimeterGauge("))
        assertTrue(unavailableCard.contains("dosePercent = null"))
        assertTrue(unavailableCard.contains("R.string.report_metric_twa"))
        assertTrue(unavailableCard.contains("R.string.meter_dosimeter_remaining"))
        assertTrue(unavailableCard.contains("R.string.value_unknown_em_dash"))
        assertFalse(unavailableCard.contains("meter_dosimeter_unavailable_description"))
    }

    @Test
    fun standardBadgeUsesNeutralSurfaceRole() {
        val source =
            projectFile(
                "src/main/java/com/dbcheck/app/ui/meter/components/DosimeterGaugeCard.kt",
            ).readText()
        val badge =
            source
                .substringAfter("private fun DosimeterStandardBadge")
                .substringBefore("private fun DosimeterGauge")

        assertTrue(badge.contains("material.surfaceContainerHigh"))
        assertTrue(badge.contains("material.onSurfaceVariant"))
        assertFalse(badge.contains(".accent"))
        assertFalse(badge.contains(".primary"))
    }
}
