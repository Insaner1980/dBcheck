package com.dbcheck.app.ui.hearing

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class HearingComponentOwnershipTest {
    @Test
    fun hearingOwnsMovedHearingAndToolComponents() {
        movedComponentFiles.forEach { fileName ->
            val newFile = hearingComponentPath(fileName).toFile()
            val oldFile = analyticsComponentPath(fileName).toFile()

            assertTrue("Hearing must own $fileName", newFile.isFile)
            assertTrue(newFile.readText().contains("package com.dbcheck.app.ui.hearing.components"))
            assertFalse("Analytics must not retain $fileName", oldFile.exists())
        }
    }

    @Test
    fun analyticsImportsMovedComponentsFromHearingPackage() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/analytics/AnalyticsScreen.kt").readText()

        movedComponentFiles.forEach { fileName ->
            val componentName = fileName.removeSuffix(".kt")
            assertTrue(
                "Analytics must import $componentName from Hearing",
                source.contains("import com.dbcheck.app.ui.hearing.components.$componentName"),
            )
            assertFalse(source.contains("import com.dbcheck.app.ui.analytics.components.$componentName"))
        }
    }

    @Test
    fun voiceBaselineHasOneReusableHearingImplementation() {
        val mainSourceRoot = Path.of("src", "main", "java").toFile()
        val implementations =
            mainSourceRoot
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .sumOf { file -> Regex("fun\\s+VoiceBaselineCard\\s*\\(").findAll(file.readText()).count() }
        val settingsSource =
            projectFile("src/main/java/com/dbcheck/app/ui/settings/components/DisplayAndFeaturesSection.kt").readText()

        assertEquals(1, implementations)
        assertTrue(
            settingsSource.contains("import com.dbcheck.app.ui.hearing.components.VoiceBaselineCard"),
        )
        assertFalse(settingsSource.contains("private fun VoiceBaselineCard"))
    }

    @Test
    fun localizationScannerTracksMovedComponentPaths() {
        val source =
            projectFile("src/test/java/com/dbcheck/app/ui/localization/LocalizationBaselineTest.kt").readText()

        listOf("AmbientSoundCard.kt", "HearingRecoveryCard.kt", "TinnitusPitchCard.kt").forEach { fileName ->
            assertTrue(source.contains("ui/hearing/components/$fileName"))
            assertFalse(source.contains("ui/analytics/components/$fileName"))
        }
    }
}

private fun hearingComponentPath(fileName: String): Path =
    Path.of("src", "main", "java", "com", "dbcheck", "app", "ui", "hearing", "components", fileName)

private fun analyticsComponentPath(fileName: String): Path =
    Path.of("src", "main", "java", "com", "dbcheck", "app", "ui", "analytics", "components", fileName)

private val movedComponentFiles =
    listOf(
        "HearingHealthCard.kt",
        "HearingTestCta.kt",
        "HearingRecoveryCard.kt",
        "TinnitusPitchCard.kt",
        "AmbientSoundCard.kt",
    )
