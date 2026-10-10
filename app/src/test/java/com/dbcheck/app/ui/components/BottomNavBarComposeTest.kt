package com.dbcheck.app.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.navigation.BottomNavDestination
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h900dp-mdpi")
class BottomNavBarComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tabsAt320Dp() = verifyTabs(320, 1f)

    @Test
    fun tabsAt200PercentFont() = verifyTabs(400, 2f)

    @Test
    fun tabsAtCombinedConstraints() = verifyTabs(320, 2f)

    private fun verifyTabs(widthDp: Int, fontScale: Float) {
        var items = emptyList<BottomNavItem>()
        val clicked = mutableListOf<String>()
        composeTestRule.setContent {
            items = BottomNavDestination.entries.map {
                BottomNavItem(stringResource(it.labelRes), it.selectedIcon, it.unselectedIcon, it.screen.route)
            }
            val route = remember { mutableStateOf(items.first().route) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Box(Modifier.requiredWidth(widthDp.dp)) {
                        BottomNavBar(items, route.value, {
                            clicked += it.route
                            route.value = it.route
                        })
                    }
                }
            }
        }
        items.forEach { item ->
            composeTestRule.onNodeWithText(item.label, useUnmergedTree = true).assertTextFits(item.label)
            val tab = composeTestRule.onNodeWithContentDescription(item.label)
            val node = tab.fetchSemanticsNode()
            assertEquals(Role.Tab, node.config[SemanticsProperties.Role])
            assertTrue(node.boundsInRoot.width >= 48f)
            assertTrue(node.boundsInRoot.height >= 48f)
            tab.performClick().assertIsSelected()
        }
        assertEquals(items.map { it.route }, clicked)
    }
}
