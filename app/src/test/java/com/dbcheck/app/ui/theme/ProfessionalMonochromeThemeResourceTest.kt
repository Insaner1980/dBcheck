package com.dbcheck.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class ProfessionalMonochromeThemeResourceTest {
    @Test
    fun themeAndLauncherResourcesDoNotUseOldNeonBrandColors() {
        val projectRoot = findProjectRoot()
        val checkedFiles =
            listOf(
                "app/src/main/java/com/dbcheck/app/ui/theme/Color.kt",
                "app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt",
                "app/src/main/res/drawable/ic_launcher_foreground.xml",
                "app/src/main/res/values/colors.xml",
                "app/src/main/java/com/dbcheck/app/util/ExternalBrand.kt",
                "app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt",
                "app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt",
            )

        val oldBrandColors =
            listOf(
                "C5FE00",
                "B9EF00",
                "DFEC60",
                "4ADE80",
                "FBBF24",
                "F87171",
                "466906",
                "5A8A0A",
                "D4F5A0",
                "954B00",
                "2D7A2D",
                "CC7700",
            )

        val matches =
            checkedFiles.flatMap { relativePath ->
                val text = projectRoot.resolve(relativePath).readText()
                oldBrandColors
                    .filter { color -> text.contains(color, ignoreCase = true) }
                    .map { color -> "$relativePath contains $color" }
            }

        assertTrue(matches.joinToString(separator = "\n"), matches.isEmpty())
    }

    private fun findProjectRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (current.parent != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current
            }
            current = current.parent
        }
        error("settings.gradle.kts not found from ${Path.of("").toAbsolutePath()}")
    }
}
