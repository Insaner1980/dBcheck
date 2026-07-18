package com.dbcheck.app.ui

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenScreenshotContractTest {
    @Test
    fun fullScreenMatrixHasExactLightAndDarkPairsAt360By800() {
        val source = screenshotSource()

        assertEquals(34, matrixPreviewNames.size)
        assertEquals(
            (matrixPreviewNames + largeFontPreviewNames).toSet(),
            previewNames(source).toSet(),
        )
        matrixPreviewNames.forEach { name ->
            val annotation = previewAnnotation(source, name)
            assertTrue("$name width", annotation.contains("widthDp = 360"))
            assertTrue("$name height", annotation.contains("heightDp = 800"))
            assertEquals("$name dark mode", name.contains("Dark"), annotation.contains("UI_MODE_NIGHT_YES"))
            assertFalse("$name font scale", annotation.contains("fontScale"))
        }
    }

    @Test
    fun largeFontMatrixHasExactFiveStatesAtOnePointFiveScale() {
        val source = screenshotSource()

        assertEquals(largeFontPreviewNames.toSet(), previewNames(source).intersect(largeFontPreviewNames.toSet()))
        largeFontPreviewNames.forEach { name ->
            val annotation = previewAnnotation(source, name)
            assertTrue("$name width", annotation.contains("widthDp = 360"))
            assertTrue("$name height", annotation.contains("heightDp = 800"))
            assertTrue("$name font scale", annotation.contains("fontScale = 1.5f"))
        }
    }

    @Test
    fun previewsUseProductionPresentationEntryPointsAndAppShell() {
        val source = screenshotSource()

        listOf(
            "MeterScreenContent(",
            "AnalyticsScreenContent(",
            "HearingScreenContent(",
            "HistoryScreenContent(",
            "SettingsHomePage(",
            "SettingsCalibrationContent(",
            "SettingsOctaveCalibrationContent(",
            "SettingsNotificationsContent(",
            "SettingsDataPrivacyContent(",
            "SettingsDisplayContent(",
            "SettingsProAboutContent(",
            "BottomNavBar(",
            "DbCheckTheme {",
        ).forEach { productionEntry ->
            assertTrue("Missing production entry $productionEntry", source.contains(productionEntry))
        }
        assertFalse(source.contains("hiltViewModel"))
        assertFalse(source.contains("rememberNavController"))
    }

    @Test
    fun compactMeterAndBottomNavigationScaleInsideTheCapturedViewport() {
        val meterSource = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val historySource = projectFile("src/main/java/com/dbcheck/app/ui/history/HistoryScreen.kt").readText()
        val historyContent = historySource.substringAfter("private fun HistorySuccessContent(")
        val navigationSource = projectFile("src/main/java/com/dbcheck/app/ui/components/BottomNavBar.kt").readText()

        assertTrue(meterSource.contains("val useCompactGauge = maxHeight < 720.dp"))
        assertTrue(historySource.contains("modifier = Modifier.weight(1f)"))
        assertTrue(historyContent.contains("modifier: Modifier = Modifier"))
        assertTrue(navigationSource.contains(".heightIn(min = 64.dp)"))
        assertTrue(navigationSource.contains("Modifier.heightIn(min = 14.dp)"))
        assertFalse(navigationSource.contains("Modifier.height(14.dp)"))
    }

    private fun screenshotSource(): String =
        projectFile("src/screenshotTest/kotlin/com/dbcheck/app/FullScreenScreenshotTests.kt").readText()

    private fun previewNames(source: String): List<String> =
        Regex("@PreviewTest\\s+@Preview\\([\\s\\S]*?\\)\\s+@Composable\\s+fun\\s+(\\w+)\\(")
            .findAll(source)
            .map { it.groupValues[1] }
            .toList()

    private fun previewAnnotation(source: String, name: String): String {
        val functionIndex = source.indexOf("fun $name(")
        check(functionIndex >= 0) { "Missing preview $name" }
        return source
            .substring(0, functionIndex)
            .substringAfterLast("@PreviewTest")
            .substringAfter("@Preview(")
            .substringBefore(")")
    }

    private companion object {
        val matrixPreviewNames =
            listOf(
                "MeterIdleLightPreview",
                "MeterIdleDarkPreview",
                "MeterRecordingLightPreview",
                "MeterRecordingDarkPreview",
                "MeterDosimeterLightPreview",
                "MeterDosimeterDarkPreview",
                "TrendsOverviewLightPreview",
                "TrendsOverviewDarkPreview",
                "TrendsSpectralLightPreview",
                "TrendsSpectralDarkPreview",
                "TrendsEnvironmentLightPreview",
                "TrendsEnvironmentDarkPreview",
                "HearingFreeLightPreview",
                "HearingFreeDarkPreview",
                "HearingProLightPreview",
                "HearingProDarkPreview",
                "HistoryEmptyLightPreview",
                "HistoryEmptyDarkPreview",
                "HistorySessionsLightPreview",
                "HistorySessionsDarkPreview",
                "SettingsHubLightPreview",
                "SettingsHubDarkPreview",
                "SettingsCalibrationLightPreview",
                "SettingsCalibrationDarkPreview",
                "SettingsOctaveCalibrationLightPreview",
                "SettingsOctaveCalibrationDarkPreview",
                "SettingsNotificationsLightPreview",
                "SettingsNotificationsDarkPreview",
                "SettingsDataPrivacyLightPreview",
                "SettingsDataPrivacyDarkPreview",
                "SettingsDisplayLightPreview",
                "SettingsDisplayDarkPreview",
                "SettingsProAboutLightPreview",
                "SettingsProAboutDarkPreview",
            )

        val largeFontPreviewNames =
            listOf(
                "MeterIdleLargeFontPreview",
                "HearingProLargeFontPreview",
                "HistorySessionsLargeFontPreview",
                "SettingsNotificationsLargeFontPreview",
                "SettingsDataPrivacyLargeFontPreview",
            )
    }
}
