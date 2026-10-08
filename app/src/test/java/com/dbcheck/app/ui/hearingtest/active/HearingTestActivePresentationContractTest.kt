package com.dbcheck.app.ui.hearingtest.active

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HearingTestActivePresentationContractTest {
    @Test
    fun tonePulseExistsOnlyInsidePlayingToneBranchAndUsesOneTokenizedTransition() {
        val source =
            projectFile(
                "src/main/java/com/dbcheck/app/ui/hearingtest/active/HearingTestActiveScreen.kt",
            ).readText()
        val motion = projectFile("src/main/java/com/dbcheck/app/ui/theme/Motion.kt").readText()

        val toneBranch =
            source
                .substringAfter("if (state.isPlayingTone) {")
                .substringBefore("}")
        assertTrue(toneBranch.contains("HearingTonePulseRing()"))
        assertEquals(1, Regex(Regex.escape("rememberInfiniteTransition(")).findAll(source).count())
        assertTrue(source.contains("durationMillis = DbCheckMotion.Breathing"))
        assertTrue(source.contains("colors.material.onSurfaceVariant.copy("))
        assertTrue(source.contains("DbCheckMotion.HearingTonePulseBaseScale"))
        assertTrue(source.contains("DbCheckMotion.HearingTonePulseScaleRange"))
        assertTrue(source.contains("DbCheckMotion.HearingTonePulseBaseAlpha"))
        assertTrue(source.contains("DbCheckMotion.HearingTonePulseAlphaRange"))
        assertTrue(motion.contains("const val HearingTonePulseBaseScale = 0.94f"))
        assertTrue(motion.contains("const val HearingTonePulseScaleRange = 0.06f"))
        assertTrue(motion.contains("const val HearingTonePulseBaseAlpha = 0.44f"))
        assertTrue(motion.contains("const val HearingTonePulseAlphaRange = 0.40f"))
        val pulseSource = source.substringAfter("private fun HearingTonePulseRing()")
        assertFalse(pulseSource.contains("colors.accent"))
    }

    @Test
    fun progressIsNeutralStandardIndicatorWithoutTrackStop() {
        val source =
            projectFile(
                "src/main/java/com/dbcheck/app/ui/hearingtest/active/HearingTestActiveScreen.kt",
            ).readText()
        val progressSource =
            source
                .substringAfter("LinearProgressIndicator(")
                .substringBefore("Spacer(Modifier.height(spacing.space4))")

        assertTrue(progressSource.contains("state.currentPhase.toFloat() / state.totalPhases"))
        assertTrue(progressSource.contains("color = colors.material.onSurface"))
        assertTrue(progressSource.contains("drawStopIndicator = {}"))
    }

    @Test
    fun screenshotPreviewsCoverToneOffOnDarkAndLargeFontAtFirstPhase() {
        val source = projectFile("src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt").readText()

        assertTrue(source.contains("fun HearingActiveToneOffPreview()"))
        assertTrue(source.contains("fun HearingActiveToneOnDarkPreview()"))
        assertTrue(source.contains("fun HearingActiveToneOnLargeFontPreview()"))
        assertTrue(source.contains("ActiveTestState(currentPhase = 1, isPlayingTone = false)"))
        assertTrue(source.contains("ActiveTestState(currentPhase = 1, isPlayingTone = true)"))
        assertTrue(source.contains("state.copy(currentEar = Ear.LEFT, totalPhases = 12)"))
    }
}
