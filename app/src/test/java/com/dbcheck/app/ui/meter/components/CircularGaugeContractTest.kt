package com.dbcheck.app.ui.meter.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CircularGaugeContractTest {
    @Test
    fun recordingStateControlsReadoutInsteadOfDecibelValue() {
        val source =
            projectFile(
                "src/main/java/com/dbcheck/app/ui/meter/components/CircularGauge.kt",
            ).readText()
        assertTrue(source.contains("isRecording: Boolean"))
        assertTrue(source.contains("if (isRecording)"))
        assertTrue(source.contains("R.string.meter_idle_instruction"))
        assertTrue(source.contains("UiNumberFormatter.integer(currentDb)"))
        assertFalse(source.contains("currentDb == 0"))
        assertFalse(source.contains("currentDb > 0"))
    }

    @Test
    fun scaleAndReadoutUseSharedTokensAndTypography() {
        val source =
            projectFile(
                "src/main/java/com/dbcheck/app/ui/meter/components/CircularGauge.kt",
            ).readText()
        val spacing = projectFile("src/main/java/com/dbcheck/app/ui/theme/Spacing.kt").readText()

        assertTrue(source.contains("rememberTextMeasurer()"))
        assertTrue(source.contains("gaugeAngleForDb"))
        assertTrue(source.contains("SoundLevelDisplayScale.positionForDb"))
        assertTrue(source.contains("spacing.meterGaugeLabelOffset"))
        assertTrue(source.contains("colors.signatureGradient"))
        assertTrue(source.contains("colors.noiseLevels.colorFor(noiseLevel)"))
        assertTrue(source.contains("style = typography.displayLg"))
        assertTrue(source.contains("style = typography.dataMd"))
        assertTrue(source.contains("style = typography.labelLg"))
        assertTrue(source.contains("gaugeInnerContentDiameter("))
        assertTrue(source.contains("spacing.meterGaugeStrokeWidth"))
        assertTrue(spacing.contains("val meterGaugeLabelOffset: Dp = 16.dp"))
        assertFalse(source.contains("10.dp.toPx()"))
        assertFalse(source.contains("IDLE_COPY_WIDTH_FRACTION"))
    }
}
