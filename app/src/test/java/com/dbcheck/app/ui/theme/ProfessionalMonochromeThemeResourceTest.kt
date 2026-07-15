package com.dbcheck.app.ui.theme

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalMonochromeThemeResourceTest {
    @Test
    fun themeAndLauncherResourcesDoNotUseOldNeonBrandColors() {
        val checkedFiles =
            listOf(
                "src/main/java/com/dbcheck/app/ui/theme/Color.kt",
                "src/main/java/com/dbcheck/app/ui/theme/Theme.kt",
                "src/main/res/drawable/ic_launcher_foreground.xml",
                "src/main/res/values/colors.xml",
                "src/main/java/com/dbcheck/app/util/ExternalBrand.kt",
                "src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt",
                "src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt",
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
                val text = projectFile(relativePath).readText()
                oldBrandColors
                    .filter { color -> text.contains(color, ignoreCase = true) }
                    .map { color -> "$relativePath contains $color" }
            }

        assertTrue(matches.joinToString(separator = "\n"), matches.isEmpty())
    }
}
