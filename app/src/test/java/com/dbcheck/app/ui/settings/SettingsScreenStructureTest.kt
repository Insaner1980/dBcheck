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

        assertTrue(component.contains("CalibrationProfileRow("))
        assertTrue(component.contains("CalibrationProfileEditorDialog("))
        assertTrue(component.contains("DeleteCalibrationProfileDialog("))
        assertTrue(component.contains("fun OctaveCalibrationSection("))
        assertTrue(component.contains("OctaveCalibrationBandSlider("))
        assertTrue(pages.contains("onOpenOctaveCalibration = onOpenOctaveCalibration"))
        assertTrue(pages.contains("responseTime = uiState.responseTime"))
    }

    @Test
    fun notificationPageOwnsScheduleAndPassiveMonitoringPermissionFlow() {
        val component = componentSource("NoiseNotificationsSection.kt")
        val pages = pagesSource()

        assertTrue(component.contains("NotificationScheduleControl("))
        assertTrue(component.contains("PassiveMonitoringControls("))
        assertTrue(pages.contains("fun SettingsNotificationsPage("))
        assertTrue(pages.contains("micPermissionLauncher"))
        assertTrue(pages.contains("notificationPermissionLauncher"))
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
