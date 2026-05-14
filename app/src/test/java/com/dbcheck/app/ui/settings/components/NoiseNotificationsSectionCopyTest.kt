package com.dbcheck.app.ui.settings.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NoiseNotificationsSectionCopyTest {
    @Test
    fun exposureDescriptionUsesConfiguredThresholdAndAverageRule() {
        val description = exposureAlertDescription(notificationThreshold = 90)

        assertEquals("Alert when 30 min average reaches 90 dB", description)
        assertFalse(description.contains("85"))
    }

    @Test
    fun peakDescriptionDoesNotPromiseSuddenDetection() {
        assertEquals("Alert when peak reaches 120 dB", PEAK_WARNING_DESCRIPTION)
        assertFalse(PEAK_WARNING_DESCRIPTION.contains("sudden", ignoreCase = true))
    }

    @Test
    fun thresholdLabelsDoNotMarkDangerThresholdAsSafe() {
        assertEquals("85 dB (default)", notificationThresholdValueLabel(notificationThreshold = 85))
        assertEquals("84 dB", notificationThresholdValueLabel(notificationThreshold = 84))
        assertEquals("85 dB", notificationThresholdReferenceLabel())
    }
}
