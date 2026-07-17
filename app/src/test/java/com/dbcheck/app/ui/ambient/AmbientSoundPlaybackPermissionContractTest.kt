package com.dbcheck.app.ui.ambient

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientSoundPlaybackPermissionContractTest {
    @Test
    fun notificationDenialExposesSystemSettingsRecovery() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/ambient/AmbientSoundPlaybackScreen.kt").readText()

        assertTrue(source.contains("state.notificationPermissionDenied"))
        assertTrue(source.contains("onOpenNotificationSettings"))
        assertTrue(source.contains("R.string.action_open_settings"))
    }
}
