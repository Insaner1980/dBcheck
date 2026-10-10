package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h900dp-mdpi")
class SessionCardConstraintsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun identityAt320Dp() = verifyCard(320, 1f)

    @Test
    fun identityAt200PercentFont() = verifyCard(400, 2f)

    @Test
    fun identityAtCombinedConstraints() = verifyCard(320, 2f)

    private fun verifyCard(widthDp: Int, fontScale: Float) {
        val title = "Warehouse calibration run"
        var editLabel = ""
        var sleepLabel = ""
        var opens = 0
        var edits = 0
        composeTestRule.setContent {
            editLabel = stringResource(R.string.session_unlock_naming_content_description)
            sleepLabel = stringResource(R.string.session_badge_sleep)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Box(Modifier.requiredWidth(widthDp.dp).padding(20.dp)) {
                        SessionCard(
                            state =
                                SessionCardState(
                                    "dB", title, "Oct 10 · 12:34", 94f, 68f, listOf("workshop", "calibration"), true,
                                ),
                            editAction = SessionCardEditAction(true, { edits++ }),
                            onClick = { opens++ },
                        )
                    }
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(title, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(results)) }
        val layout = results.single()
        assertTrue("The session title disappeared", layout.getLineEnd(0, visibleEnd = true) > 0)
        assertTrue("The session title has no visible width", layout.getLineRight(0) > 0f)
        composeTestRule.onNodeWithText(sleepLabel, useUnmergedTree = true).assertTextFits(sleepLabel)
        composeTestRule.onNodeWithText("94", useUnmergedTree = true).assertTextFits("94")
        composeTestRule.onNodeWithText("68", useUnmergedTree = true).assertTextFits("68")
        composeTestRule.onNodeWithText(title).performClick()
        composeTestRule.onNodeWithContentDescription(editLabel).performClick()
        assertEquals(1, opens)
        assertEquals(1, edits)
    }
}
