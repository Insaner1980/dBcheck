package com.dbcheck.app.ui.settings.components

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.ui.settings.state.AudioInputDeviceUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w800dp-h1800dp-mdpi")
class SettingsSelectionSemanticsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun toggleHasItsVisibleNameAndInvokesOneAction() {
        var calls = 0
        composeTestRule.setContent {
            val checked = remember { mutableStateOf(false) }
            DbCheckTheme {
                SettingsToggleDescriptionRow(
                    title = "Show waveform",
                    subtitle = "Display the waveform during measurement",
                    checked = checked.value,
                    onCheckedChange = {
                        calls++
                        checked.value = it
                    },
                )
            }
        }
        printTrees()
        composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertCountEquals(1)
        composeTestRule.onNodeWithContentDescription("Show waveform").assertIsOff().performClick().assertIsOn()
        assertEquals(1, calls)
    }

    @Test
    fun audioInputHasOneNamedRadioAction() {
        val selected = mutableListOf<Int>()
        composeTestRule.setContent {
            DbCheckTheme {
                AudioCalibrationSection(
                    state = AudioCalibrationSectionState(
                        sensitivityOffset = 0f,
                        frequencyWeighting = "A",
                        responseTime = ResponseTime.FAST,
                        isProUser = true,
                        profiles = emptyList(),
                        selectedProfileId = null,
                        profileErrorMessage = null,
                        audioInputDevices =
                            listOf(AudioInputDeviceUiState(42, "External microphone", AudioInputDeviceType.USB, true)),
                        selectedAudioInputDeviceId = null,
                    ),
                    actions = AudioCalibrationSectionActions(
                        onSensitivityChange = {},
                        onWeightingChange = {},
                        onResponseTimeChange = {},
                        onSelectAudioInputDevice = { selected += it },
                        onCreateProfile = {},
                        onSelectProfile = {},
                        onRenameProfile = { _, _ -> },
                        onDeleteProfile = {},
                        onOpenOctaveCalibration = {},
                        onUpgradeClick = {},
                    ),
                )
            }
        }
        printTrees()
        composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertCountEquals(1)
        composeTestRule.onNodeWithText("External microphone").performClick()
        assertEquals(listOf(42), selected)
    }

    private fun printTrees() {
        println(composeTestRule.onRoot().printToString())
        println(composeTestRule.onRoot(useUnmergedTree = true).printToString())
    }
}
