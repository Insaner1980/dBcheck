package com.dbcheck.app.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.readText

class SettingsScreenStructureTest {
    @Test
    fun settingsContentRoutesDisplayAndFeatureSettingsThroughDedicatedSection() {
        val source = settingsScreenSource()
        val settingsContent = source.functionBlock("SettingsContent")
        val audioSections = source.functionBlock("SettingsAudioAndNotificationSections")

        assertTrue(settingsContent.contains("SettingsDisplayAndFeaturesSection("))
        assertFalse(settingsContent.contains("DisplayAppearanceSection("))
        assertFalse(audioSections.contains("LockscreenMeterSection("))
    }

    @Test
    fun displayAndFeaturesSectionOwnsDisplayChoicesAndLockscreenFeature() {
        val source = componentSource("DisplayAndFeaturesSection.kt")
        val lockscreenSource = componentSource("LockscreenMeterSection.kt")

        assertTrue(source.contains("fun DisplayAndFeaturesSection("))
        assertTrue(source.contains("R.string.settings_display_features_title"))
        assertTrue(source.contains("ThemeMode.entries"))
        assertTrue(source.contains("WaveformStyle.entries"))
        assertTrue(source.contains("MeterRefreshRate.entries"))
        assertTrue(source.contains("LockscreenMeterSection("))
        assertTrue(source.contains("showLockscreenMeterPublicly = state.showLockscreenMeterPublicly"))
        assertTrue(source.contains("onShowLockscreenMeterPubliclyChange = actions.onShowLockscreenMeterPubliclyChange"))
        assertTrue(lockscreenSource.contains("R.string.lockscreen_meter_public_warning"))
    }

    @Test
    fun audioCalibrationSectionOwnsCalibrationProfileControls() {
        val source = componentSource("AudioCalibrationSection.kt")
        val settingsScreen = settingsScreenSource()
        val screenshotSource = screenshotTestSource()

        assertTrue(source.contains("CalibrationProfileRow("))
        assertTrue(source.contains("CalibrationProfileEditorDialog("))
        assertTrue(source.contains("DeleteCalibrationProfileDialog("))
        assertTrue(source.contains("OctaveCalibrationControls("))
        assertTrue(source.contains("OctaveCalibrationBandSlider("))
        assertTrue(source.contains("R.string.settings_calibration_octave_reset"))
        assertTrue(settingsScreen.contains("SettingsAudioAndNotificationSections("))
        assertTrue(settingsScreen.contains("onOctaveBandOffsetChange = viewModel::updateOctaveBandOffset"))
        assertTrue(settingsScreen.contains("onResetOctaveBandOffsets = viewModel::resetOctaveBandOffsets"))
        assertTrue(screenshotSource.contains("fun AudioCalibrationProfilesPreview()"))
        assertTrue(screenshotSource.contains("previewOctaveBandOffsets"))
    }

    @Test
    fun noiseNotificationsSectionOwnsNotificationScheduleControls() {
        val source = componentSource("NoiseNotificationsSection.kt")
        val settingsScreen = settingsScreenSource()
        val screenshotSource = screenshotTestSource()

        assertTrue(source.contains("notificationSchedule: NoiseNotificationSchedule"))
        assertTrue(source.contains("NotificationScheduleControl("))
        assertTrue(source.contains("NoiseNotificationSchedule.ALL_DAYS"))
        assertTrue(source.contains("stateDescription ="))
        assertTrue(settingsScreen.contains("notificationSchedule = uiState.notificationSchedule"))
        assertTrue(settingsScreen.contains("onScheduleChange = actions.onScheduleChange"))
        assertTrue(settingsScreen.contains("NoiseNotificationUpdate.NotificationSchedule"))
        assertTrue(screenshotSource.contains("fun NoiseNotificationSchedulePreview()"))
    }

    @Test
    fun noiseNotificationsSectionOwnsPassiveMonitoringDisclosureAndControls() {
        val source = componentSource("NoiseNotificationsSection.kt")
        val settingsScreen = settingsScreenSource()

        assertTrue(source.contains("PassiveMonitoringControls("))
        assertTrue(source.contains("R.string.noise_notifications_passive_monitoring_title"))
        assertTrue(source.contains("R.string.noise_notifications_passive_monitoring_disclosure"))
        assertTrue(source.contains("passiveMonitoringActive"))
        assertTrue(source.contains("onStartPassiveMonitoring"))
        assertTrue(source.contains("onStopPassiveMonitoring"))
        assertTrue(settingsScreen.contains("passiveMonitoringActive = uiState.passiveMonitoringActive"))
        assertTrue(settingsScreen.contains("onStartPassiveMonitoring = actions.onStartPassiveMonitoring"))
        assertTrue(settingsScreen.contains("onStopPassiveMonitoring = actions.onStopPassiveMonitoring"))
    }
}

private fun settingsScreenSource(): String = Path
    .of("src", "main", "java", "com", "dbcheck", "app", "ui", "settings", "SettingsScreen.kt")
    .readText()

private fun componentSource(fileName: String): String = Path
    .of("src", "main", "java", "com", "dbcheck", "app", "ui", "settings", "components", fileName)
    .readText()

private fun screenshotTestSource(): String = Path
    .of("src", "screenshotTest", "kotlin", "com", "dbcheck", "app", "ComponentScreenshotTests.kt")
    .readText()

private fun String.functionBlock(name: String): String {
    val functionMarker =
        Regex("(private\\s+)?fun $name|(private\\s+)?@Composable\\s+fun $name").find(this)?.value
            ?: error("Function $name not found")
    return substringAfter(functionMarker)
        .substringBefore("\n\n@Composable")
        .substringBefore("\n\nprivate fun")
        .let { "$functionMarker$it" }
}
