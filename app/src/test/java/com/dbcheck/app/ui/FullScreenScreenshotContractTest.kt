package com.dbcheck.app.ui

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

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

    @Test
    fun meterIdleBaselineShowsBothCollapsedDisclosureRowsBeforeFixedControls() {
        val baselineName = "MeterIdleLightPreview_d5e2e3ff_0.png"
        val baseline =
            projectFile(
                "src/screenshotTestDebug/reference/com/dbcheck/app/" +
                    "FullScreenScreenshotTestsKt/$baselineName",
            )
        val image = checkNotNull(ImageIO.read(baseline)) { "$baselineName could not be decoded" }

        assertEquals("$baselineName width", 945, image.width)
        assertEquals("$baselineName height", 2100, image.height)
        assertEquals(
            "$baselineName must render Live details and Sound Reference as separate full-width rows",
            2,
            fullWidthCardBands(image).size,
        )
    }

    private fun screenshotSource(): String =
        projectFile("src/screenshotTest/kotlin/com/dbcheck/app/FullScreenScreenshotTests.kt").readText()

    private fun fullWidthCardBands(image: BufferedImage): List<IntRange> {
        val sampleXs = intArrayOf(85, 340, 472, 614, 830)
        val minimumVisibleBandPixels = image.width * 24 / 360
        val qualifyingRows =
            (0 until image.height * 11 / 12).filter { y ->
                val cardColor = image.getRGB(sampleXs.first(), y)
                cardColor != image.getRGB(10, y) &&
                    sampleXs.count { x -> image.getRGB(x, y) == cardColor } >= 4
            }

        return qualifyingRows
            .fold(mutableListOf<IntRange>()) { bands, y ->
                val previous = bands.lastOrNull()
                if (previous == null || y > previous.last + 12) {
                    bands += y..y
                } else {
                    bands[bands.lastIndex] = previous.first..y
                }
                bands
            }.filter { band -> band.last - band.first + 1 >= minimumVisibleBandPixels }
    }

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
