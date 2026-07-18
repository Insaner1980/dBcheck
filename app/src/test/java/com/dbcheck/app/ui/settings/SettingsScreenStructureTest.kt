package com.dbcheck.app.ui.settings

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenStructureTest {
    @Test
    fun calibrationKeepsProfilesAndMovesOctaveControlsToDeepPage() {
        val component = componentSource("AudioCalibrationSection.kt")
        val pages = pagesSource()
        val calibrationPage =
            pages.requiredBlock("fun SettingsCalibrationPage(", "fun SettingsOctaveCalibrationPage(")
        val calibrationContent =
            pages.requiredBlock(
                "internal fun SettingsCalibrationContent(",
                "internal fun SettingsOctaveCalibrationContent(",
            )
        val octaveContent =
            pages.requiredBlock(
                "internal fun SettingsOctaveCalibrationContent(",
                "internal fun SettingsNotificationsContent(",
            )

        assertTrue(component.contains("CalibrationProfileRow("))
        assertTrue(component.contains("CalibrationProfileEditorDialog("))
        assertTrue(component.contains("DeleteCalibrationProfileDialog("))
        assertTrue(component.contains("fun OctaveCalibrationSection("))
        assertTrue(component.contains("OctaveCalibrationBandSlider("))
        assertTrue(calibrationPage.contains("onOpenOctaveCalibration = onOpenOctaveCalibration"))
        assertTrue(calibrationContent.contains("responseTime = uiState.responseTime"))
        assertFalse(calibrationContent.contains("OctaveCalibrationSection("))
        assertTrue(octaveContent.contains("OctaveCalibrationSection("))
    }

    @Test
    fun notificationPageOwnsScheduleAndPassiveMonitoringPermissionFlow() {
        val component = componentSource("NoiseNotificationsSection.kt")
        val pages = pagesSource()
        val notificationsPage =
            pages.requiredBlock("fun SettingsNotificationsPage(", "fun SettingsDataPrivacyPage(")
        val notificationLauncher =
            notificationsPage.requiredBlock("val notificationPermissionLauncher", "val micPermissionLauncher")
        val microphoneLauncher =
            notificationsPage.requiredBlock("val micPermissionLauncher", "val onStartProPurchase")
        val notificationContinuation =
            pages.requiredBlock(
                "private fun continuePassiveMonitoringAfterNotificationPermission(",
                "private fun SettingsDataPrivacyMessageEffects(",
            )

        assertTrue(component.contains("NotificationScheduleControl("))
        assertTrue(component.contains("PassiveMonitoringControls("))
        assertTrue(notificationLauncher.contains("viewModel.startPassiveMonitoring()"))
        assertTrue(microphoneLauncher.contains("continuePassiveMonitoringAfterNotificationPermission("))
        assertFalse(microphoneLauncher.contains("viewModel.startPassiveMonitoring()"))
        assertTrue(notificationContinuation.contains("context.hasPostNotificationsPermission()"))
        assertTrue(notificationContinuation.contains("requestPostNotificationsPermissionIfNeeded("))
        assertTrue(pages.contains("onStopPassiveMonitoring = viewModel::stopPassiveMonitoring"))
    }

    @Test
    fun dataPrivacyPageOwnsLocationRecoveryHealthExportAndLockscreen() {
        val pages = pagesSource()
        val dataExport = componentSource("DataExportSection.kt")

        assertTrue(pages.contains("fun SettingsDataPrivacyPage("))
        assertTrue(pages.contains("locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)"))
        assertTrue(pages.contains("HealthSyncSection("))
        assertTrue(pages.contains("DataExportSection("))
        assertTrue(pages.contains("LockscreenMeterSection("))
        assertTrue(dataExport.contains("onOpenLocationSettings"))
        assertFalse(pages.contains("Manifest.permission.ACCESS_FINE_LOCATION"))
        assertFalse(pages.contains("Manifest.permission.ACCESS_BACKGROUND_LOCATION"))
    }

    @Test
    fun displayPageKeepsDisplayAndFeaturePreferencesWithoutVoiceBaselineOrLockscreen() {
        val display = componentSource("DisplayAndFeaturesSection.kt")
        val page = pagesSource().substringAfter("fun SettingsDisplayPage(").substringBefore("fun SettingsProAboutPage(")

        assertTrue(display.contains("ThemeMode.entries"))
        assertTrue(display.contains("WaveformStyle.entries"))
        assertTrue(display.contains("MeterRefreshRate.entries"))
        assertTrue(display.contains("settings_feature_sound_detection_title"))
        assertTrue(display.contains("settings_feature_sleep_card_title"))
        assertFalse(display.contains("VoiceBaseline"))
        assertFalse(display.contains("LockscreenMeterSection("))
        assertFalse(page.contains("LockscreenMeterSection("))
    }

    @Test
    fun settingsDialogsAndPurchaseMessagesStillUseSharedComponents() {
        val sharedDialog = sharedComponentSource("DbCheckAlertDialog.kt")
        val pages = pagesSource()
        val settingsComponents =
            listOf("AudioCalibrationSection.kt", "DataExportSection.kt", "HealthSyncSection.kt")
                .joinToString(separator = "\n") { componentSource(it) }

        assertTrue(sharedDialog.contains("fun DbCheckAlertDialog("))
        assertTrue(settingsComponents.contains("DbCheckAlertDialog("))
        assertFalse(settingsComponents.contains("import androidx.compose.material3.AlertDialog"))
        assertTrue(pages.contains("private fun SettingsPurchaseFeedback("))
        assertTrue(pages.contains("InlineStatusRow("))
    }
}

private fun pagesSource() = projectFile("src/main/java/com/dbcheck/app/ui/settings/SettingsPages.kt").readText()

private fun componentSource(fileName: String) =
    projectFile("src/main/java/com/dbcheck/app/ui/settings/components/$fileName").readText()

private fun sharedComponentSource(fileName: String) =
    projectFile("src/main/java/com/dbcheck/app/ui/components/$fileName").readText()

private fun String.requiredBlock(startMarker: String, endMarker: String): String {
    val startIndex = indexOf(startMarker)
    require(startIndex >= 0) { "Missing start marker $startMarker" }
    val endIndex = indexOf(endMarker, startIndex + startMarker.length)
    require(endIndex >= 0) { "Missing end marker $endMarker after $startMarker" }
    return substring(startIndex, endIndex)
}
