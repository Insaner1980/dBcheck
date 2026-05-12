package com.dbcheck.app

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import org.junit.Assert.assertEquals
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
        assertEquals(
            true,
            isHealthConnectPermissionDisclosureAction(ACTION_VIEW_HEALTH_PERMISSION_USAGE),
        )
    }

    @Test
    fun normalLaunchActionsDoNotUseHealthConnectDisclosure() {
        assertEquals(false, isHealthConnectPermissionDisclosureAction(null))
        assertEquals(false, isHealthConnectPermissionDisclosureAction("android.intent.action.MAIN"))
    }
}
