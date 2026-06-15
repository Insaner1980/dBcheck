package com.dbcheck.app.ui.meter.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MeterSessionInfoBarTest {
    @Test
    fun formatterUsesClockDurationAndCompactSampleRateLabels() {
        assertEquals("1:02:03", MeterSessionInfoFormatter.durationLabel(3_723_000L))
        assertEquals("44.1 kHz", MeterSessionInfoFormatter.sampleRateLabel(44_100))
        assertEquals("48 kHz", MeterSessionInfoFormatter.sampleRateLabel(48_000))
    }

    @Test
    fun formatterFallsBackWhenInputDeviceNameIsMissing() {
        assertEquals(
            "Default input",
            MeterSessionInfoFormatter.inputDeviceLabel(null, defaultInputLabel = "Default input"),
        )
        assertEquals(
            "USB-C microphone",
            MeterSessionInfoFormatter.inputDeviceLabel("  USB-C microphone  ", defaultInputLabel = "Default input"),
        )
    }
}
