package com.dbcheck.app.ui.meter.components

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.ui.meter.state.MeterUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class MeterUnavailableReadoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recordingWithoutSamplesShowsUnavailableInsteadOfZero() {
        composeTestRule.setContent {
            DbCheckTheme(darkTheme = false) {
                Column {
                    CircularGauge(
                        0f,
                        NoiseLevel.QUIET,
                        isRecording = true,
                        animationsEnabled = false,
                        hasSamples = false,
                    )
                    StatCard("MIN", null, animationsEnabled = false)
                    val state = MeterUiState()
                    SoundReferenceCard(
                        currentDb = null,
                        markers = state.soundReferenceMarkers,
                        nearestMarker = state.nearestSoundReferenceMarker,
                        currentPosition = state.soundReferenceCurrentPosition,
                        expanded = false,
                        onExpandedChange = {},
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithText("N/A", useUnmergedTree = true).assertCountEquals(4)
        composeTestRule.onNodeWithText("0").assertDoesNotExist()
        composeTestRule.onNodeWithText("0 dB").assertDoesNotExist()
    }
}
