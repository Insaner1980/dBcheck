package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.ui.analytics.components.SpectralModeChipRow
import com.dbcheck.app.ui.analytics.state.SpectralMode
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h900dp-mdpi")
class DbCheckChipComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun validLongTagAt320Dp() = verifyTag(320, 1f)

    @Test
    fun validLongTagAt200PercentFont() = verifyTag(400, 2f)

    @Test
    fun validLongTagAtCombinedConstraints() = verifyTag(320, 2f)

    @Test
    fun spectralLabelsStayWholeAt320Dp() = verifySpectralLabels(320, 1f)

    @Test
    fun spectralLabelsStayWholeAt200PercentFont() = verifySpectralLabels(400, 2f)

    @Test
    fun spectralLabelsStayWholeAtCombinedConstraints() = verifySpectralLabels(320, 2f)

    private fun verifySpectralLabels(widthDp: Int, fontScale: Float) {
        var selected: SpectralMode? = null
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Box(Modifier.requiredWidth(widthDp.dp).padding(horizontal = 24.dp)) {
                        SpectralModeChipRow(SpectralMode.BARS, { selected = it })
                    }
                }
            }
        }
        val node = composeTestRule.onNodeWithText("Spectrogram")
        node.assertTextFits("Spectrogram")
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(1, results.single().lineCount)
        node.performClick()
        assertEquals(SpectralMode.SPECTROGRAM, selected)
    }

    private fun verifyTag(widthDp: Int, fontScale: Float) {
        val tag = "W".repeat(24)
        assertEquals(listOf(tag), SessionMetadata.normalizeTags(listOf(tag)))
        var clicks = 0
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Box(Modifier.requiredWidth(widthDp.dp).padding(horizontal = 20.dp)) {
                        DbCheckChip(text = tag, selected = true, onClick = { clicks++ })
                    }
                }
            }
        }
        val node = composeTestRule.onNodeWithText(tag)
        node.assertTextFits(tag)
        node.performClick()
        assertEquals(1, clicks)
    }
}
