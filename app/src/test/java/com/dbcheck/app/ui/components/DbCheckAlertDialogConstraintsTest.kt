package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.dbcheck.app.R
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
@Config(application = Application::class, sdk = [28], qualifiers = "w320dp-h800dp-mdpi")
class DbCheckAlertDialogConstraintsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clearHistoryActionsAtCombinedConstraints() {
        var confirmLabel = ""
        var cancelLabel = ""
        var confirmations = 0
        var cancellations = 0
        composeTestRule.setContent {
            confirmLabel = stringResource(R.string.action_clear_history)
            cancelLabel = stringResource(R.string.action_cancel)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DbCheckTheme {
                    DbCheckAlertDialog(
                        title = stringResource(R.string.settings_clear_history_dialog_title),
                        body = stringResource(R.string.settings_clear_history_dialog_message),
                        confirmText = confirmLabel,
                        dismissText = cancelLabel,
                        onConfirm = { confirmations++ },
                        onDismiss = { cancellations++ },
                        icon = Icons.Outlined.Delete,
                    )
                }
            }
        }
        composeTestRule.onNodeWithText(confirmLabel).assertIsDisplayed().assertTextFits(confirmLabel)
        composeTestRule.onNodeWithText(cancelLabel).assertIsDisplayed().assertTextFits(cancelLabel)
        composeTestRule.onNodeWithText(cancelLabel).performClick()
        composeTestRule.onNodeWithText(confirmLabel).performClick()
        assertEquals(1, cancellations)
        assertEquals(1, confirmations)
    }
}
