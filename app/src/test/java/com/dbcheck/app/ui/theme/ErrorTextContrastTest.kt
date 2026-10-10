package com.dbcheck.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ErrorTextContrastTest {
    @Test
    fun warningAndSuccessTextMeetMinimumContrastOnTheirCardSurfaces() {
        listOf(
            LightWarning to LightSurfaceContainer,
            LightWarning to LightSurfaceContainerHigh,
            LightSuccess to LightSurfaceContainer,
            DarkWarning to DarkSurfaceContainer,
            DarkWarning to DarkSurfaceContainerHigh,
            DarkSuccess to DarkSurfaceContainer,
        ).forEach { (foreground, background) ->
            val contrast = contrastRatio(foreground, background)
            assertTrue("Status text contrast is $contrast on $background", contrast >= 4.5f)
        }
    }

    @Test
    fun errorTextMeetsMinimumContrastOnItsLightAndDarkSurfaces() {
        listOf(
            LightError to LightBackground,
            LightError to LightSurface,
            LightError to LightSurfaceContainer,
            LightError to LightSurfaceContainerHigh,
            DarkError to DarkBackground,
            DarkError to DarkSurface,
            DarkError to DarkSurfaceContainer,
            DarkError to DarkSurfaceContainerHigh,
        ).forEach { (foreground, background) ->
            val contrast = contrastRatio(foreground, background)
            assertTrue("Error text contrast is $contrast on $background", contrast >= 4.5f)
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val foregroundLuminance = foreground.luminance()
        val backgroundLuminance = background.luminance()
        return (max(foregroundLuminance, backgroundLuminance) + 0.05f) /
            (min(foregroundLuminance, backgroundLuminance) + 0.05f)
    }
}
