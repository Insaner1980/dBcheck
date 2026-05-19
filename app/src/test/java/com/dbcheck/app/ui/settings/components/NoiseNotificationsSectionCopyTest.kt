package com.dbcheck.app.ui.settings.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class NoiseNotificationsSectionCopyTest {
    @Test
    fun exposureDescriptionUsesConfiguredThresholdAndAverageRule() {
        val description = String.format(
            Locale.US,
            stringResourceValue("noise_notifications_exposure_description"),
            90,
        )

        assertEquals("Alert when 30 min average reaches 90 dB", description)
        assertFalse(description.contains("85"))
    }

    @Test
    fun peakDescriptionDoesNotPromiseSuddenDetection() {
        val description = stringResourceValue("noise_notifications_peak_description")

        assertEquals("Alert when peak reaches 120 dB", description)
        assertFalse(description.contains("sudden", ignoreCase = true))
    }

    @Test
    fun thresholdLabelsDoNotMarkDangerThresholdAsSafe() {
        assertEquals(
            "85 dB (default)",
            notificationThresholdValueLabel(
                notificationThreshold = 85,
                valueLabel = "85 dB",
                defaultValueLabel = "85 dB (default)",
            ),
        )
        assertEquals(
            "84 dB",
            notificationThresholdValueLabel(
                notificationThreshold = 84,
                valueLabel = "84 dB",
                defaultValueLabel = "84 dB (default)",
            ),
        )
        assertEquals("85 dB", String.format(Locale.US, stringResourceValue("notification_db_value"), 85))
    }

    @Test
    fun unitCopyUsesDbCasingAndSpacing() {
        assertEquals("LAST 7 DAYS (dB AVERAGE)", stringResourceValue("exposure_summary_last_7_days"))
        assertEquals("AVG dB/DAY", stringResourceValue("exposure_summary_avg_db_day"))
        assertEquals("PEAK dB", stringResourceValue("last_24_hours_peak_db"))
        assertEquals(
            "The test requires a room noise floor under 50 dB for precision.",
            stringResourceValue("hearing_setup_find_silence_description"),
        )
    }

    @Test
    fun frequencyAxisCopyUsesUnitSpacing() {
        assertEquals("20 Hz", stringResourceValue("unit_20_hz"))
        assertEquals("1 kHz", stringResourceValue("unit_1_khz"))
        assertEquals("20 kHz", stringResourceValue("unit_20_khz"))
    }

    @Test
    fun notificationPlaceholderDurationMatchesSharedClockFormat() {
        assertEquals("Peak 0 dB · 0:00", stringResourceValue("notification_peak_duration_placeholder"))
    }

    private fun stringResourceValue(name: String): String {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(projectFile("src/main/res/values/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.attributes?.getNamedItem("name")?.nodeValue == name) {
                return node.textContent
            }
        }
        error("String resource not found: $name")
    }
}
