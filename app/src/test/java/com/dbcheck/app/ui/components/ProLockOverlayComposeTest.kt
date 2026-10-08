package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.history.components.SessionNamingSheet
import com.dbcheck.app.ui.history.HistoryScreenContent
import com.dbcheck.app.ui.history.state.HistoryUiState
import com.dbcheck.app.ui.history.state.HourlyExposureUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProLockOverlayComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "fi-w360dp-h800dp")
    fun historyMissingTrendsDisabledButtonAndFinnishUnitsAt320Dp() = verifyHistoryAndButton(320, 1f)

    @Test
    @Config(qualifiers = "fi-w360dp-h800dp")
    fun historyMissingTrendsDisabledButtonAndFinnishUnitsAt200PercentFont() = verifyHistoryAndButton(360, 2f)

    @Test
    @Config(qualifiers = "fi-w320dp-h800dp")
    fun historyMissingTrendsDisabledButtonAndFinnishUnitsAt320DpAnd200PercentFont() = verifyHistoryAndButton(320, 2f)

    private fun verifyHistoryAndButton(widthDp: Int, fontScale: Float) {
        var disabledClicks = 0
        var averageText = ""
        var relativeText = ""
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    averageText = stringResource(R.string.last_24_hours_average_without_trend, 70)
                    relativeText = stringResource(R.string.hearing_relative_db_value, "20")
                    Column(Modifier.width(widthDp.dp).height(720.dp)) {
                        Box(Modifier.weight(1f)) {
                            HistoryScreenContent(
                                state = HistoryUiState.Success(
                                    last24HoursData = listOf(HourlyExposureUiState(12, 70f, 80f, 0L)),
                                    last24HoursAvg = 70f,
                                    last24HoursMax = 80f,
                                    last24HoursWindowEndMs = 86_400_000L,
                                ),
                            )
                        }
                        Text(relativeText)
                        DbCheckButton("Disabled", { disabledClicks++ }, enabled = false)
                    }
                }
            }
        }
        composeTestRule.onNodeWithText(averageText).assertIsDisplayed()
        composeTestRule.onNodeWithText("80").assertIsDisplayed()
        composeTestRule.onNodeWithText(relativeText).assertIsDisplayed()
        assertTrue(relativeText.contains("suhteellinen"))
        val button = composeTestRule.onNodeWithText("Disabled").assertIsNotEnabled().assertIsDisplayed()
        button.performTouchInput { click() }
        assertEquals(0, disabledClicks)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun customAndPreviouslyLocalizedTagsCanBeRemovedAt320DpAnd200PercentFont() {
        var savedTags: List<String>? = null
        var saveText = ""
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DbCheckTheme {
                    saveText = stringResource(R.string.action_save)
                    SessionNamingSheet(
                        currentName = "Session",
                        currentEmoji = "",
                        currentTags = listOf("Custom", "Työ"),
                        onDismiss = {},
                        onSave = { _, _, tags -> savedTags = tags },
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Custom").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Custom").assertDoesNotExist()
        composeTestRule.onNodeWithText("Työ").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Työ").assertDoesNotExist()
        composeTestRule.onNodeWithText(saveText).performScrollTo().performClick()
        assertEquals(emptyList<String>(), savedTags)
    }

    @Test
    fun lockedPreviewAt320DpBlocksPreviewAndKeepsUpgradeClickable() = verifyOverlay(320, 1f)

    @Test
    fun lockedPreviewAt200PercentFontKeepsUpgradeClickable() = verifyOverlay(360, 2f)

    @Test
    fun lockedPreviewAt320DpAnd200PercentFontKeepsUpgradeClickable() = verifyOverlay(320, 2f)

    private fun verifyOverlay(widthDp: Int, fontScale: Float) {
        var previewClicks = 0
        var upgradeClicks = 0
        var upgradeText = ""
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    upgradeText = stringResource(R.string.action_upgrade)
                    Box(Modifier.size(widthDp.dp, 360.dp).testTag("host")) {
                        ProLockOverlay(isLocked = true, onUpgradeClick = { upgradeClicks++ }) {
                            Box(Modifier.size(widthDp.dp, 360.dp).clickable { previewClicks++ }) {
                                Text("Private preview")
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Private preview").assertDoesNotExist()
        composeTestRule.onNodeWithTag("host").performTouchInput { click(Offset(8f, 8f)) }
        assertEquals(0, previewClicks)
        val upgrade = composeTestRule.onNodeWithText(upgradeText)
        assertTrue(upgrade.fetchSemanticsNode().boundsInRoot.height >= 48f)
        upgrade.performTouchInput { click() }
        assertEquals(1, upgradeClicks)
        assertEquals(0, previewClicks)
    }
}
