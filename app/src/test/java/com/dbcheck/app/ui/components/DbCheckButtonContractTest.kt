package com.dbcheck.app.ui.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DbCheckButtonContractTest {
    @Test
    fun primaryButtonResolvesEnabledPressedAndDisabledColorsBeforeDrawing() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/DbCheckButton.kt").readText()

        assertTrue(source.contains("enabled = enabled"))
        assertTrue(source.contains("enabled: Boolean"))
        assertTrue(source.contains("isPressed: Boolean"))
        assertTrue(source.contains("!enabled -> colors.material.surfaceContainerHighest"))
        assertTrue(source.contains("isPressed -> colors.accentDim"))
        assertTrue(source.contains("else -> colors.accent"))
        assertTrue(
            source.contains(
                "contentColor = if (enabled) colors.onAccent else colors.material.onSurfaceVariant",
            ),
        )
        assertFalse(source.contains("signatureGradient"))
    }

    @Test
    fun nonPrimaryButtonsNeverUseAccentForIdleOrPressedContainers() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/DbCheckButton.kt").readText()
        val secondary = source.substringAfter("private fun secondaryButtonVisuals").substringBefore(
            "private fun tertiaryButtonVisuals",
        )
        val tertiary =
            source.substringAfter("private fun tertiaryButtonVisuals").substringBefore(
                "private fun Modifier.dbCheckButtonModifier",
            )

        assertFalse(secondary.contains("colors.accent"))
        assertFalse(tertiary.contains("colors.accent"))
    }
}
