package com.dbcheck.app.ui.theme

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DbCheckColorRoleContractTest {
    @Test
    fun themeDefinesTheApprovedAccentAndLevelPalettes() {
        val colors = projectFile("src/main/java/com/dbcheck/app/ui/theme/Color.kt").readText()
        val expectedColors =
            listOf(
                "DarkAccent = Color(0xFF9CBFA3)",
                "DarkAccentDim = Color(0xFF6E8F76)",
                "DarkOnAccent = Color(0xFF08120C)",
                "DarkAccentContainer = Color(0xFF1B2A20)",
                "DarkOnAccentContainer = Color(0xFFC9E0CE)",
                "DarkLevelQuiet = Color(0xFF7E9C86)",
                "DarkLevelNormal = Color(0xFF9CBFA3)",
                "DarkLevelElevated = Color(0xFFD6A94F)",
                "DarkLevelDangerous = Color(0xFFE07A7A)",
                "LightAccent = Color(0xFF2F5D43)",
                "LightAccentDim = Color(0xFF4C7A5E)",
                "LightOnAccent = Color(0xFFFFFFFF)",
                "LightAccentContainer = Color(0xFFDCEADF)",
                "LightOnAccentContainer = Color(0xFF17301F)",
                "LightLevelQuiet = Color(0xFF607460)",
                "LightLevelNormal = Color(0xFF3F7350)",
                "LightLevelElevated = Color(0xFF8A6C2D)",
                "LightLevelDangerous = Color(0xFFA95353)",
            )

        expectedColors.forEach { expected ->
            assertTrue("Missing approved color token: $expected", colors.contains(expected))
        }
    }

    @Test
    fun materialPrimaryRolesAndNoiseLevelsUseSeparateThemeHolders() {
        val theme = projectFile("src/main/java/com/dbcheck/app/ui/theme/Theme.kt").readText()

        listOf(
            "val accent: Color",
            "val accentDim: Color",
            "val onAccent: Color",
            "val accentContainer: Color",
            "val onAccentContainer: Color",
            "val noiseLevels: NoiseLevelColors",
            "fun colorFor(level: NoiseLevel)",
            "fun contentColorFor(level: NoiseLevel)",
            "primary = DarkAccent",
            "onPrimary = DarkOnAccent",
            "primaryContainer = DarkAccentContainer",
            "onPrimaryContainer = DarkOnAccentContainer",
            "primary = LightAccent",
            "onPrimary = LightOnAccent",
            "primaryContainer = LightAccentContainer",
            "onPrimaryContainer = LightOnAccentContainer",
        ).forEach { contract ->
            assertTrue("Missing theme color-role contract: $contract", theme.contains(contract))
        }
        assertFalse(theme.contains("val primaryDim: Color"))
    }

    @Test
    fun signatureGradientHasOnlyItsThemeDefinitionAndCircularGaugeConsumer() {
        val uiRoot = projectFile("src/main/java/com/dbcheck/app/ui/theme/Theme.kt").parentFile.parentFile
        val hits =
            uiRoot
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    file
                        .readLines()
                        .mapIndexedNotNull { index, line ->
                            if ("signatureGradient" in line) {
                                "${file.relativeTo(uiRoot).invariantSeparatorsPath}:${index + 1}"
                            } else {
                                null
                            }
                        }
                        .asSequence()
                }.toList()

        val hitFiles = hits.map { it.substringBefore(":") }.toSet()
        assertEquals(
            hits.joinToString(separator = "\n"),
            setOf("theme/Theme.kt", "meter/components/CircularGauge.kt"),
            hitFiles,
        )
    }

    @Test
    fun prohibitedPresentationRolesDoNotUseMaterialPrimary() {
        val prohibitedFiles =
            listOf(
                "ui/components/DbCheckTopAppBar.kt",
                "ui/components/DbCheckSetupScaffold.kt",
                "ui/components/InlineStatusRow.kt",
                "ui/components/SessionCard.kt",
                "ui/settings/components/SettingsRows.kt",
                "ui/analytics/components/MonthlyTrendChart.kt",
                "ui/analytics/components/SpectralAnalysisCard.kt",
                "ui/analytics/components/WeeklyBarChart.kt",
                "ui/history/components/Last24HoursChart.kt",
                "ui/hearing/components/HearingHealthPresentation.kt",
            )

        prohibitedFiles.forEach { relativePath ->
            val source = projectFile("src/main/java/com/dbcheck/app/$relativePath").readText()
            assertFalse("$relativePath leaks accent through material.primary", source.contains("material.primary"))
        }
    }

    @Test
    fun subduedCardsUseTheDbCheckSurfaceRoleInsteadOfTheMaterialDefault() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/DbCheckCard.kt").readText()
        val subduedBranch =
            source
                .substringAfter("DbCheckCardEmphasis.Subdued ->")
                .substringBefore("DbCheckCardEmphasis.Default ->")

        assertTrue(subduedBranch.contains("colors.surfaceContainerLowest"))
        assertFalse(subduedBranch.contains("colors.material.surfaceContainerLowest"))
    }
}
