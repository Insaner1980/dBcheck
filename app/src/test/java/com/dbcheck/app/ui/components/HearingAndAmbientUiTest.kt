package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackCallbacks
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackScreen
import com.dbcheck.app.ui.ambient.AmbientSoundPlaybackUiState
import com.dbcheck.app.ui.hearing.components.TinnitusPitchCard
import com.dbcheck.app.ui.hearingtest.results.HearingTestResultsContent
import com.dbcheck.app.ui.hearingtest.results.ResultsUiState
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
@Config(application = Application::class, sdk = [28], qualifiers = "en-w400dp-h900dp-mdpi")
class HearingAndAmbientUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectionsAndResultStatesRemainUsableAtCompactAndLargeFontSettings() {
        val constraints = mutableStateOf(320 to 1f)
        val screen = mutableIntStateOf(0)
        val result = mutableStateOf(ResultsUiState())
        var selectedPreset: AmbientSoundPreset? = null
        var selectedTimer: Int? = null
        var backCalls = 0
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, constraints.value.second)) {
                DbCheckTheme {
                    Box(Modifier.requiredWidth(constraints.value.first.dp)) {
                        when (screen.intValue) {
                            0 -> AmbientSoundPlaybackScreen(
                                state = AmbientSoundPlaybackUiState(
                                    preset = AmbientSoundPreset.BROWN_NOISE,
                                    timerMinutes = 30,
                                    isProUser = true,
                                ),
                                onBack = {},
                                callbacks = AmbientSoundPlaybackCallbacks(
                                    onNavigateToUpgrade = {},
                                    onPresetChange = { selectedPreset = it },
                                    onVolumeChange = {},
                                    onTimerChange = { selectedTimer = it },
                                    onPlay = {},
                                    onStop = {},
                                    onOpenNotificationSettings = {},
                                ),
                            )
                            1 -> TinnitusPitchCard(
                                profile = TinnitusPitchProfile(leftFrequencyHz = 999f, rightFrequencyHz = 1_000f),
                                isLocked = false,
                                onOpenPitchMatcher = {},
                            )
                            else -> HearingTestResultsContent(
                                state = result.value,
                                onSave = { backCalls++ },
                                onShare = {},
                            )
                        }
                    }
                }
            }
        }

        listOf(320 to 1f, 400 to 2f, 320 to 2f).forEach { constraint ->
            composeTestRule.runOnIdle {
                constraints.value = constraint
                screen.intValue = 0
            }
            composeTestRule.onNodeWithText("Brown").performScrollTo().assertIsSelected().assertTextFits("Brown")
            composeTestRule.onNodeWithText("Fan").performScrollTo().performClick()
            assertEquals(AmbientSoundPreset.FAN, selectedPreset)
            composeTestRule.onNodeWithText("30 min").performScrollTo().assertIsSelected().assertTextFits("30 min")
            composeTestRule.onNodeWithText("15 min").performScrollTo().performClick()
            assertEquals(15, selectedTimer)

            composeTestRule.runOnIdle { screen.intValue = 1 }
            val summary = "Left 999 Hz · Right 1.0 kHz"
            composeTestRule.onNodeWithText(summary).assertTextFits(summary)

            composeTestRule.runOnIdle {
                screen.intValue = 2
                result.value = ResultsUiState()
            }
            composeTestRule.onNodeWithText("Loading result…").performScrollTo().assertTextFits("Loading result…")
            composeTestRule.onNodeWithText("Back to Hearing").assertDoesNotExist()
            listOf(
                ResultsUiState(isLoading = false) to "Hearing test requires dBcheck Pro",
                ResultsUiState(isLoading = false, isProUser = true, isResultMissing = true) to "Result not found",
                ResultsUiState(isLoading = false, loadErrorMessage = "Unable to load results") to
                    "Unable to load results",
            ).forEach { (state, title) ->
                composeTestRule.runOnIdle { result.value = state }
                composeTestRule.onNodeWithText(title).performScrollTo().assertTextFits(title)
                composeTestRule.onNodeWithText("Back to Hearing").performScrollTo().assertTextFits("Back to Hearing")
                composeTestRule.onNodeWithText("Back to Hearing").performClick()
            }
        }
        assertEquals(9, backCalls)
    }
}
