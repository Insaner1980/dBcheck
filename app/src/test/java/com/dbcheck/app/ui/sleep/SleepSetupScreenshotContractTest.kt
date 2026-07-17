package com.dbcheck.app.ui.sleep

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepSetupScreenshotContractTest {
    @Test
    fun sleepSetupScreenHasRegisteredScreenshotPreview() {
        val source = projectFile("src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt").readText()

        assertTrue(source.contains("fun SleepSetupScreenPreview()"))
        assertTrue(source.contains("SleepSetupScreen("))
        assertTrue(source.contains("SleepSetupUiState("))
    }

    @Test
    fun sleepSetupScreenShowsPreparationControlsAndCopy() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/sleep/SleepSetupScreen.kt").readText()

        assertTrue(source.contains("SleepDurationOptionsCard("))
        assertTrue(source.contains("SleepKeepAwakeCard("))
        assertTrue(source.contains("SleepSetupDurationOption"))
        assertTrue(source.contains("DbCheckChip("))
        assertTrue(source.contains("DbCheckToggle("))
        assertTrue(source.contains("R.string.sleep_setup_battery_note"))
        assertTrue(source.contains("R.string.sleep_setup_privacy_note"))
    }

    @Test
    fun sleepSetupScreenOwnsActiveRecordingControlsAndOptInKeepAwake() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/sleep/SleepSetupScreen.kt").readText()

        assertTrue(source.contains("SleepRecordingActionCard("))
        assertTrue(source.contains("onStartRecording"))
        assertTrue(source.contains("onStopRecording"))
        assertTrue(source.contains("KeepScreenOnEffect("))
        assertTrue(source.contains("uiState.isRecording && uiState.keepAwakeEnabled"))
        assertTrue(source.contains("R.string.sleep_setup_start_recording"))
        assertTrue(source.contains("R.string.sleep_setup_stop_recording"))
    }

    @Test
    fun sleepSetupScreenExposesMicrophoneDenialRecovery() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/sleep/SleepSetupScreen.kt").readText()

        assertTrue(source.contains("uiState.showMicDeniedPrompt"))
        assertTrue(source.contains("onOpenMicSettings"))
        assertTrue(source.contains("R.string.action_open_settings"))
        assertTrue(source.contains("R.string.action_try_again"))
    }
}
