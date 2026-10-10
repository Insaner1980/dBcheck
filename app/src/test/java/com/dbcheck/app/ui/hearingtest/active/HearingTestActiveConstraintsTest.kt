package com.dbcheck.app.ui.hearingtest.active

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.hearingtest.HearingTestMode
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h900dp-mdpi")
class HearingTestActiveConstraintsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun responsesAt320Dp() = verifyControls(320, 1f, false)

    @Test
    fun responsesAt200PercentFont() = verifyControls(400, 2f, false)

    @Test
    fun responsesAtCombinedConstraints() = verifyControls(320, 2f, false)

    @Test
    fun retryAt320Dp() = verifyControls(320, 1f, true)

    @Test
    fun retryAt200PercentFont() = verifyControls(400, 2f, true)

    @Test
    fun retryAtCombinedConstraints() = verifyControls(320, 2f, true)

    private fun verifyControls(widthDp: Int, fontScale: Float, failed: Boolean) {
        var heard = 0
        var missed = 0
        var retried = 0
        var hearText = ""
        var missText = ""
        var retryText = ""
        composeTestRule.setContent {
            hearText = stringResource(R.string.action_i_hear_it)
            missText = stringResource(R.string.action_i_do_not_hear_it)
            retryText = stringResource(R.string.action_try_again)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme(darkTheme = false) {
                    Box(Modifier.requiredSize(widthDp.dp, 800.dp)) {
                        HearingTestActiveContent(
                            state = ActiveTestState(
                                currentFrequency = 8000f,
                                errorMessage = "Tone could not start".takeIf { failed },
                            ),
                            mode = HearingTestMode.FULL,
                            onBack = {},
                            onRetrySave = {},
                            onHearTone = { heard++ },
                            onMissTone = { missed++ },
                            onRetryTone = { retried++ },
                        )
                    }
                }
            }
        }

        if (failed) {
            val retry = composeTestRule.onNodeWithText(retryText)
            if (fontScale > 1f) retry.performScrollTo()
            retry.assertIsDisplayed().performClick()
            composeTestRule.onNodeWithText(hearText).assertIsNotEnabled()
            composeTestRule.onNodeWithText(missText).assertIsNotEnabled()
            assertEquals(1, retried)
        } else {
            val hear = composeTestRule.onNodeWithText(hearText)
            if (fontScale > 1f) hear.performScrollTo()
            hear.assertIsDisplayed().performClick()
            val miss = composeTestRule.onNodeWithText(missText)
            if (fontScale > 1f) miss.performScrollTo()
            miss.assertIsDisplayed().performClick()
            assertEquals(1, heard)
            assertEquals(1, missed)
        }
    }
}
