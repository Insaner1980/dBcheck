package com.dbcheck.app.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.hearing.HearingTestUiState
import com.dbcheck.app.ui.hearing.HearingUiState
import com.dbcheck.app.ui.history.state.HistoryUiState
import com.dbcheck.app.ui.meter.state.MeterUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h800dp")
class UiSnapshotRecompositionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun equalSnapshotsSkipAndChangedSnapshotsUpdateAt320Dp() {
        verifySnapshots(widthDp = 320, fontScale = 1f)
    }

    @Test
    fun equalSnapshotsSkipAndChangedSnapshotsUpdateAt200PercentFont() {
        verifySnapshots(widthDp = 360, fontScale = 2f)
    }

    @Test
    fun equalSnapshotsSkipAndChangedSnapshotsUpdateAt320DpAnd200PercentFont() {
        verifySnapshots(widthDp = 320, fontScale = 2f)
    }

    private fun verifySnapshots(widthDp: Int, fontScale: Float) {
        val unrelatedRedraw = mutableIntStateOf(0)
        val amplitude = mutableFloatStateOf(0.25f)
        val level = mutableFloatStateOf(60f)
        val label = mutableStateOf("first")
        var compositionCount = 0
        val onCompose: () -> Unit = { compositionCount++ }

        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Column(Modifier.width(widthDp.dp)) {
                        Text("Redraw ${unrelatedRedraw.intValue}")
                        SnapshotReadout(
                            analytics =
                                AnalyticsUiState.Success(
                                    spectralAnalysis =
                                        SpectralAnalysisUiState.Live(
                                            bands = listOf(SpectralBandUiState(amplitude.floatValue)),
                                            dominantFrequencyHz = 1_000f,
                                            bandwidth = SpectralBandwidth.NARROW,
                                        ),
                                ),
                            meter = MeterUiState(currentDb = level.floatValue),
                            hearing =
                                HearingUiState(
                                    latestHearingTest = HearingTestUiState.Result(1L, 1L, 80, label.value, 10f),
                                ),
                            history = HistoryUiState.Success(searchQuery = label.value),
                            onCompose = onCompose,
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Analytics 0.25").assertIsDisplayed()
        composeTestRule.onNodeWithText("Meter 60.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hearing first").assertIsDisplayed()
        composeTestRule.onNodeWithText("History first").assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(1, compositionCount) }

        composeTestRule.runOnIdle { unrelatedRedraw.intValue++ }
        composeTestRule.onNodeWithText("Redraw 1").assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(1, compositionCount) }

        composeTestRule.runOnIdle {
            amplitude.floatValue = 0.75f
            level.floatValue = 75f
            label.value = "second"
        }

        composeTestRule.onNodeWithText("Analytics 0.75").assertIsDisplayed()
        composeTestRule.onNodeWithText("Meter 75.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hearing second").assertIsDisplayed()
        composeTestRule.onNodeWithText("History second").assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(2, compositionCount) }
    }

    @Composable
    private fun SnapshotReadout(
        analytics: AnalyticsUiState,
        meter: MeterUiState,
        hearing: HearingUiState,
        history: HistoryUiState,
        onCompose: () -> Unit,
    ) {
        SideEffect(onCompose)
        val spectral = (analytics as AnalyticsUiState.Success).spectralAnalysis as SpectralAnalysisUiState.Live
        Column {
            Text("Analytics ${spectral.bands.single().normalizedAmplitude}")
            Text("Meter ${meter.currentDb}")
            Text("Hearing ${(hearing.latestHearingTest as HearingTestUiState.Result).rating}")
            Text("History ${(history as HistoryUiState.Success).searchQuery}")
        }
    }
}
