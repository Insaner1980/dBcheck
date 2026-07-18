package com.dbcheck.app.ui.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCardPeakPolicyTest {
    @Test
    fun trailingStatsUseCompactThemeTypographyAndSpacing() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/SessionCard.kt").readText()

        assertTrue(source.contains("Row(horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2))"))
        assertTrue(source.contains("style = typography.dataMd"))
        assertTrue(source.contains("style = typography.labelSm"))
        assertFalse(source.contains("style = typography.dataLg"))
        assertFalse(source.contains("style = typography.labelMd"))
    }

    @Test
    fun titleEmojiAndEditAffordancesKeepFlexibleAndAccessibleSpace() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/SessionCard.kt").readText()
        val spacingSource = projectFile("src/main/java/com/dbcheck/app/ui/theme/Spacing.kt").readText()
        val editButtonUsesIconCircle =
            "IconButton(onClick = editAction.onClick, modifier = Modifier.size(spacing.iconCircle))"

        assertTrue(source.contains("Text(text = state.emoji, style = typography.headlineMd)"))
        assertTrue(source.contains("modifier = Modifier.weight(1f)"))
        assertTrue(source.contains(".size(spacing.iconCircle)"))
        assertTrue(source.contains(editButtonUsesIconCircle))
        assertTrue(spacingSource.contains("val iconCircle: Dp = 48.dp"))
    }

    @Test
    fun sessionPeakWarningUsesLcPeakPolicyInsteadOfExposureBoundary() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/SessionCard.kt").readText()

        assertTrue(source.contains("NoiseAlertPolicy.PEAK_WARNING_DB"))
        assertTrue(source.contains("if (peakIsWarning) SessionStatTone.Warning else SessionStatTone.Default"))
        assertTrue(source.contains("if (peakIsWarning) SessionStatTone.Muted else SessionStatTone.Default"))
        assertTrue(source.contains("SessionStatTone.Warning -> colors.warning"))
        assertFalse(source.contains("peakDb >= NoiseLevel.ELEVATED.maxDb"))
    }
}
