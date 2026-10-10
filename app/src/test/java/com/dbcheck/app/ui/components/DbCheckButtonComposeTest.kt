package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h900dp-mdpi")
class DbCheckButtonComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun primaryLongLabelAt320Dp() = verifyLabel(320, 1f, DbCheckButtonStyle.Primary)

    @Test
    fun primaryLongLabelAt200PercentFont() = verifyLabel(400, 2f, DbCheckButtonStyle.Primary)

    @Test
    fun primaryLongLabelAtCombinedConstraints() = verifyLabel(320, 2f, DbCheckButtonStyle.Primary)

    @Test
    fun secondaryLongLabelAtCombinedConstraints() = verifyLabel(320, 2f, DbCheckButtonStyle.Secondary)

    private fun verifyLabel(widthDp: Int, fontScale: Float, style: DbCheckButtonStyle) {
        val label = "Start passive sample"
        var clicks = 0
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme(darkTheme = style == DbCheckButtonStyle.Secondary) {
                    Box(Modifier.requiredWidth(widthDp.dp)) {
                        DbCheckButton(
                            text = label,
                            onClick = { clicks++ },
                            modifier = Modifier.fillMaxWidth(),
                            style = style,
                            height = 48.dp,
                        )
                    }
                }
            }
        }

        val node = composeTestRule.onNodeWithText(label)
        node.assertTextFits(label)
        node.assertIsEnabled().performClick().performClick()
        assertEquals(2, clicks)
    }
}
