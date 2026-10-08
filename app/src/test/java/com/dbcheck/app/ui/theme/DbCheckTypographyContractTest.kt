package com.dbcheck.app.ui.theme

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Test

class DbCheckTypographyContractTest {
    @Test
    fun displayAndDataStylesUseTabularNumerals() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/theme/Type.kt").readText()
        val styles = listOf("displayLg", "displayMd", "dataXl", "dataLg", "dataMd")

        styles.forEachIndexed { index, style ->
            val endMarker = styles.getOrNull(index + 1)?.let { "val $it" } ?: "\n)"
            val block =
                source
                    .substringAfter("val $style")
                    .substringBefore(endMarker)
            assertEquals(
                "$style must define tnum once",
                1,
                Regex("fontFeatureSettings = \"tnum\"").findAll(block).count(),
            )
        }
    }
}
