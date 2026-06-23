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

        assertTrue(source.contains("fun DisplayAndFeaturesSection("))
        assertTrue(source.contains("R.string.settings_display_features_title"))
        assertTrue(source.contains("ThemeMode.entries"))
        assertTrue(source.contains("WaveformStyle.entries"))
        assertTrue(source.contains("MeterRefreshRate.entries"))
        assertTrue(source.contains("LockscreenMeterSection("))
    }

    @Test
    fun audioCalibrationSectionOwnsCalibrationProfileControls() {
        val source = componentSource("AudioCalibrationSection.kt")
        val settingsScreen = settingsScreenSource()
        val screenshotSource =
            Path.of("src", "screenshotTest", "kotlin", "com", "dbcheck", "app", "ComponentScreenshotTests.kt")
                .readText()

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
}

private fun settingsScreenSource(): String = Path
    .of("src", "main", "java", "com", "dbcheck", "app", "ui", "settings", "SettingsScreen.kt")
    .readText()

private fun componentSource(fileName: String): String = Path
    .of("src", "main", "java", "com", "dbcheck", "app", "ui", "settings", "components", fileName)
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
