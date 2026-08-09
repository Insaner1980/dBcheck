package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DbCheckPresentationComponentsComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lockedCtaRendersGroupedContentAndInvokesPrimaryAction() {
        var clickCount = 0

        composeTestRule.setContent {
            DbCheckTheme {
                DbCheckLockedCtaCard(
                    content =
                        DbCheckLockedCtaContent(
                            title = "Sleep setup",
                            subtitle = "Prepare your measurement",
                            buttonText = "Open setup",
                        ),
                    onClick = { clickCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Sleep setup").assertExists()
        composeTestRule.onNodeWithText("Prepare your measurement").assertExists()
        composeTestRule.onNodeWithText("Open setup").performClick()

        assertEquals(1, clickCount)
    }

    @Test
    fun emptyStateRendersGroupedContentAndInvokesAction() {
        var clickCount = 0

        composeTestRule.setContent {
            DbCheckTheme {
                EmptyState(
                    content =
                        EmptyStateContent(
                            icon = Icons.Outlined.History,
                            title = "No measurements",
                            description = "Start measuring to build history",
                        ),
                    ctaText = "Start measuring",
                    onCtaClick = { clickCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText("No measurements").assertExists()
        composeTestRule.onNodeWithText("Start measuring to build history").assertExists()
        composeTestRule.onNodeWithText("Start measuring").performClick()

        assertEquals(1, clickCount)
    }

    @Test
    fun sliderUsesGroupedLabelsInTextAndStateDescription() {
        composeTestRule.setContent {
            DbCheckTheme {
                DbCheckSlider(
                    value = 0.5f,
                    onValueChange = {},
                    labels =
                        DbCheckSliderLabels(
                            value = "50 dB",
                            min = "0 dB",
                            max = "100 dB",
                        ),
                )
            }
        }

        composeTestRule.onNodeWithText("50 dB").assertExists()
        composeTestRule.onNodeWithText("0 dB").assertExists()
        composeTestRule.onNodeWithText("100 dB").assertExists()
        composeTestRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "50 dB, 0 dB – 100 dB",
                ),
            ).assertExists()
    }
}
