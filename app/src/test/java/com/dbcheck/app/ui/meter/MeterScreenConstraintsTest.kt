package com.dbcheck.app.ui.meter

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.assertTextFits
import com.dbcheck.app.ui.meter.state.MeterUiState
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
@Config(application = Application::class, sdk = [28], qualifiers = "w800dp-h900dp-mdpi")
class MeterScreenConstraintsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expandedCardsAndStopAtCombinedConstraints() = verifyContent(320, 700, 2f)

    @Test
    fun expandedCardsAndStopInLandscape() = verifyContent(800, 360, 1f)

    private fun verifyContent(widthDp: Int, heightDp: Int, fontScale: Float) {
        var pauseLabel = ""
        var liveCollapseLabel = ""
        var liveExpandLabel = ""
        var referenceCollapseLabel = ""
        var referenceExpandLabel = ""
        var stops = 0
        composeTestRule.setContent {
            pauseLabel = stringResource(R.string.action_pause)
            liveCollapseLabel = stringResource(R.string.a11y_meter_live_details_collapse)
            liveExpandLabel = stringResource(R.string.a11y_meter_live_details_expand)
            referenceCollapseLabel = stringResource(R.string.a11y_sound_reference_collapse)
            referenceExpandLabel = stringResource(R.string.a11y_sound_reference_expand)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Box(Modifier.requiredSize(widthDp.dp, heightDp.dp)) {
                        MeterScreenContent(
                            uiState =
                                MeterUiState(
                                    currentDb = 73f,
                                    sampleCount = 3,
                                    isRecording = true,
                                    isMicPermissionGranted = true,
                                    isProUser = true,
                                ),
                            actions = MeterScreenActions({}, {}, {}, { stops++ }, {}, {}, {}, {}),
                            initialExpansionState = MeterExpansionState(true, true),
                        )
                    }
                }
            }
        }
        val readout = composeTestRule.onNodeWithText("73", useUnmergedTree = true)
        readout.performScrollTo().assertIsDisplayed().assertTextFits("73")
        composeTestRule.onNodeWithContentDescription(liveCollapseLabel)
            .performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.onNodeWithContentDescription(liveExpandLabel).assertExists()
        composeTestRule.onNodeWithContentDescription(referenceCollapseLabel)
            .performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.onNodeWithContentDescription(referenceExpandLabel).assertExists()
        composeTestRule.onNodeWithContentDescription(pauseLabel).assertIsDisplayed().performClick()
        assertEquals(1, stops)
    }
}
