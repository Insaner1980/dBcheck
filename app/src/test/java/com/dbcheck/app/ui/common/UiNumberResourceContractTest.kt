package com.dbcheck.app.ui.common

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiNumberResourceContractTest {
    @Test
    fun uiMeasurementResourcesAcceptPreformattedStringsInEnglishAndFinnish() {
        val defaultStrings = projectFile("src/main/res/values/strings.xml").readText()
        val finnishStrings = projectFile("src/main/res/values-fi/strings.xml").readText()

        UI_NUMBER_RESOURCE_NAMES.forEach { name ->
            val defaultValue = defaultStrings.stringValue(name)
            val finnishValue = finnishStrings.stringValue(name)

            assertFalse("$name must not reformat UI numbers with %f", defaultValue.containsFloatPlaceholder())
            assertFalse("$name must not reformat Finnish UI numbers with %f", finnishValue.containsFloatPlaceholder())
            assertTrue("$name must accept a preformatted string", defaultValue.containsStringPlaceholder())
            assertEquals(
                "Placeholder contract differs between default and Finnish resources for $name",
                defaultValue.placeholders(),
                finnishValue.placeholders(),
            )
        }
    }
}

private fun String.stringValue(name: String): String =
    requireNotNull(Regex("""<string name="$name">(.*?)</string>""").find(this)?.groupValues?.get(1)) {
        "Missing string resource: $name"
    }

private fun String.containsFloatPlaceholder(): Boolean = FLOAT_PLACEHOLDER.containsMatchIn(this)

private fun String.containsStringPlaceholder(): Boolean = STRING_PLACEHOLDER.containsMatchIn(this)

private fun String.placeholders(): Set<String> = PLACEHOLDER.findAll(this).map { it.value }.toSet()

private val UI_NUMBER_RESOURCE_NAMES =
    setOf(
        "hearing_hub_latest_test_result",
        "hearing_recovery_shift_db",
        "tinnitus_pitch_frequency_hz",
        "tinnitus_pitch_frequency_khz",
        "settings_calibration_offset_db",
        "settings_calibration_frequency_hz",
        "settings_calibration_frequency_khz",
    )

private val FLOAT_PLACEHOLDER = Regex("""%\d+\${'$'}[+.]?\d*f""")
private val STRING_PLACEHOLDER = Regex("""%\d+\${'$'}s""")
private val PLACEHOLDER = Regex("""%\d+\${'$'}[sdf]""")
