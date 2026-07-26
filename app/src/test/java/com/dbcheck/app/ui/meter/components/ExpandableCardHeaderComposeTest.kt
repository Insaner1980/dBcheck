package com.dbcheck.app.ui.meter.components

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ExpandableCardHeaderComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun headerIsAccessibleAndTogglesStateInLightTheme() {
        verifyExpandableHeader(darkTheme = false)
    }

    @Test
    fun headerIsAccessibleAndTogglesStateInDarkTheme() {
        verifyExpandableHeader(darkTheme = true)
    }

    private fun verifyExpandableHeader(darkTheme: Boolean) {
        val headerText = "Expandable header"
        val collapsedLabel = "Collapsed"
        val expandedLabel = "Expanded"
        val expandedContent = "Expanded content"

        composeTestRule.setContent {
            DbCheckTheme(darkTheme = darkTheme) {
                var expanded by remember { mutableStateOf(false) }
                Column(
                    modifier =
                        Modifier.expandableCardHeader(
                            spacing = DbCheckTheme.spacing,
                            stateLabel = if (expanded) expandedLabel else collapsedLabel,
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        ),
                ) {
                    Text(headerText)
                    if (expanded) {
                        Text(expandedContent)
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithText(headerText)
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, collapsedLabel))
            .performClick()

        composeTestRule.onNodeWithText(expandedContent).assertExists()
        composeTestRule
            .onNodeWithText(headerText)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expandedLabel))
    }
}
