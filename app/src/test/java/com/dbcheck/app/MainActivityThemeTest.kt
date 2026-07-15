package com.dbcheck.app

import android.app.UiModeManager
import com.dbcheck.app.data.local.preferences.model.ThemeMode
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityThemeTest {
    @Test
    fun unresolvedPreferencesKeepStartupThemeLoading() {
        assertEquals(
            StartupThemeState.Loading,
            resolveStartupThemeState(
                prefs = null,
                systemDarkTheme = false,
            ),
        )
    }

    @Test
    fun resolvedPreferencesUseSavedThemeMode() {
        assertEquals(
            StartupThemeState.Resolved(darkTheme = true),
            resolveStartupThemeState(
                prefs = UserPreferences(themeMode = "dark"),
                systemDarkTheme = false,
            ),
        )
    }

    @Test
    fun systemThemeModeUsesCurrentSystemTheme() {
        assertEquals(
            StartupThemeState.Resolved(darkTheme = true),
            resolveStartupThemeState(
                prefs = UserPreferences(themeMode = "system"),
                systemDarkTheme = true,
            ),
        )
    }

    @Test
    fun healthConnectPermissionDisclosureActionsAreRecognized() {
        assertEquals(
            true,
            isHealthConnectPermissionDisclosureAction(ACTION_SHOW_HEALTH_CONNECT_PERMISSIONS_RATIONALE),
        )
    }

    @Test
    fun applicationNightModeMatchesStoredThemeMode() {
        assertEquals(UiModeManager.MODE_NIGHT_YES, ThemeMode.DARK.applicationNightMode())
        assertEquals(UiModeManager.MODE_NIGHT_NO, ThemeMode.LIGHT.applicationNightMode())
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, ThemeMode.SYSTEM.applicationNightMode())
    }

    @Test
    fun startupThemesDisableSystemSelectedPreview() {
        val themeFiles =
            listOf(
                "src/main/res/values/themes.xml",
                "src/main/res/values-night/themes.xml",
            )

        themeFiles.forEach { relativePath ->
            val source = projectFile(relativePath).readText()
            assertTrue(
                "$relativePath must disable the system-selected startup preview",
                source.contains("<item name=\"android:windowDisablePreview\">true</item>"),
            )
        }
    }

    @Test
    fun normalLaunchActionsDoNotUseHealthConnectDisclosure() {
        assertEquals(false, isHealthConnectPermissionDisclosureAction(null))
        assertEquals(false, isHealthConnectPermissionDisclosureAction("android.intent.action.MAIN"))
        assertEquals(false, isHealthConnectPermissionDisclosureAction(ACTION_VIEW_HEALTH_PERMISSION_USAGE))
    }
}
