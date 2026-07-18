package com.dbcheck.app.ui.localization

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationBaselineTest {
    @Test
    fun finnishLaunchResourcesExistForInitialLocaleBaseline() {
        val defaultStrings = projectFile("src/main/res/values/strings.xml")
        val resDir = requireNotNull(defaultStrings.parentFile?.parentFile)
        val finnishStrings = File(resDir, "values-fi/strings.xml")

        assertTrue("Osa 94 launch locale baseline requires values-fi/strings.xml", finnishStrings.isFile)

        val defaultResources = defaultStrings.readStringResources()
        val finnishResources = finnishStrings.readStringResources()

        REQUIRED_FINNISH_BASELINE_STRINGS.forEach { name ->
            assertTrue("Missing Finnish string resource: $name", finnishResources.containsKey(name))
            assertEquals(
                "Placeholder mismatch for $name",
                defaultResources.getValue(name).placeholders(),
                finnishResources.getValue(name).placeholders(),
            )
        }
    }

    @Test
    fun finnishLaunchResourcesKeepPluralPlaceholderContracts() {
        val defaultStrings = projectFile("src/main/res/values/strings.xml")
        val resDir = requireNotNull(defaultStrings.parentFile?.parentFile)
        val finnishStrings = File(resDir, "values-fi/strings.xml")

        assertTrue("Osa 94 launch locale baseline requires values-fi/strings.xml", finnishStrings.isFile)

        val defaultPlurals = defaultStrings.readPluralResources()
        val finnishPlurals = finnishStrings.readPluralResources()

        REQUIRED_FINNISH_BASELINE_PLURALS.forEach { name ->
            assertTrue("Missing Finnish plural resource: $name", finnishPlurals.containsKey(name))
            assertEquals(defaultPlurals.getValue(name).keys, finnishPlurals.getValue(name).keys)
            defaultPlurals.getValue(name).forEach { (quantity, value) ->
                assertEquals(
                    "Placeholder mismatch for $name/$quantity",
                    value.placeholders(),
                    finnishPlurals.getValue(name).getValue(quantity).placeholders(),
                )
            }
        }
    }

    @Test
    fun newUiSurfacesDoNotInlineUserFacingComposeText() {
        val offenders =
            LOCALIZATION_SCANNED_SOURCE_FILES
                .flatMap { relativePath ->
                    projectFile(relativePath)
                        .readLines()
                        .mapIndexedNotNull { index, line ->
                            val trimmed = line.trim()
                            if (USER_FACING_LITERAL_PATTERNS.any { it.containsMatchIn(trimmed) }) {
                                "$relativePath:${index + 1}: $trimmed"
                            } else {
                                null
                            }
                        }
                }

        assertTrue(
            "User-facing Compose text must use stringResource in new UI surfaces:\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }
}

private val REQUIRED_FINNISH_BASELINE_STRINGS =
    setOf(
        "action_cancel",
        "action_open_settings",
        "action_play",
        "action_try_again",
        "action_upgrade",
        "a11y_back",
        "a11y_not_selected",
        "a11y_selected",
        "ambient_sound_card_description",
        "ambient_sound_card_title",
        "ambient_sound_description",
        "ambient_sound_notification_required",
        "ambient_sound_open",
        "ambient_sound_phase",
        "ambient_sound_play",
        "ambient_sound_preset",
        "ambient_sound_preset_brown_noise",
        "ambient_sound_preset_fan",
        "ambient_sound_preset_pink_noise",
        "ambient_sound_preset_white_noise",
        "ambient_sound_pro_required",
        "ambient_sound_stop",
        "ambient_sound_timer",
        "ambient_sound_timer_minutes",
        "ambient_sound_timer_none",
        "ambient_sound_title",
        "ambient_sound_volume",
        "ambient_sound_volume_value",
        "hearing_recovery_average_shift",
        "hearing_recovery_description",
        "hearing_recovery_locked_preview",
        "hearing_recovery_max_shift",
        "hearing_recovery_missing_baseline",
        "hearing_recovery_result_elevated_shift",
        "hearing_recovery_result_small_shift",
        "hearing_recovery_result_stable",
        "hearing_recovery_setup_compare_description",
        "hearing_recovery_setup_compare_title",
        "hearing_recovery_setup_description",
        "hearing_recovery_setup_phase",
        "hearing_recovery_setup_title",
        "hearing_recovery_shift_db",
        "hearing_recovery_start_baseline",
        "hearing_recovery_start_short_check",
        "hearing_recovery_title",
        "hearing_setup_use_headphones_description",
        "hearing_setup_use_headphones_title",
        "meter_idle_instruction",
        "meter_live_details",
        "a11y_meter_live_details_collapse",
        "a11y_meter_live_details_collapsed",
        "a11y_meter_live_details_expand",
        "a11y_meter_live_details_expanded",
        "notification_action_stop",
        "notification_ambient_sound_channel_name",
        "notification_ambient_sound_text",
        "notification_ambient_sound_text_timer",
        "notification_ambient_sound_title",
        "settings_audio_response_time",
        "settings_page_calibration",
        "settings_page_data_privacy",
        "settings_page_display",
        "settings_page_notifications",
        "settings_page_octave_calibration",
        "settings_page_pro_about",
        "tinnitus_pitch_card_description",
        "tinnitus_pitch_card_title",
        "tinnitus_pitch_current_frequency",
        "tinnitus_pitch_description",
        "tinnitus_pitch_frequency_hz",
        "tinnitus_pitch_frequency_khz",
        "tinnitus_pitch_left_ear",
        "tinnitus_pitch_no_saved_profile",
        "tinnitus_pitch_open",
        "tinnitus_pitch_phase",
        "tinnitus_pitch_preview",
        "tinnitus_pitch_right_ear",
        "tinnitus_pitch_save",
        "tinnitus_pitch_saved_summary",
        "tinnitus_pitch_title",
        "value_unavailable",
    )

private val REQUIRED_FINNISH_BASELINE_PLURALS =
    setOf(
        "a11y_meter_live_chart_active",
        "a11y_meter_live_chart_paused",
    )

private val LOCALIZATION_SCANNED_SOURCE_FILES =
    listOf(
        "src/main/java/com/dbcheck/app/ui/ambient/AmbientSoundPlaybackScreen.kt",
        "src/main/java/com/dbcheck/app/ui/hearing/components/AmbientSoundCard.kt",
        "src/main/java/com/dbcheck/app/ui/hearing/components/HearingRecoveryCard.kt",
        "src/main/java/com/dbcheck/app/ui/hearing/components/TinnitusPitchCard.kt",
        "src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt",
        "src/main/java/com/dbcheck/app/ui/meter/components/LiveActivityCard.kt",
        "src/main/java/com/dbcheck/app/ui/meter/components/SoundReferenceCard.kt",
        "src/main/java/com/dbcheck/app/ui/settings/SettingsPages.kt",
        "src/main/java/com/dbcheck/app/ui/settings/components/AudioCalibrationSection.kt",
        "src/main/java/com/dbcheck/app/ui/settings/components/NoiseNotificationsSection.kt",
        "src/main/java/com/dbcheck/app/ui/tinnitus/TinnitusPitchMatcherScreen.kt",
        "src/main/java/com/dbcheck/app/ui/hearingtest/setup/HearingRecoverySetupScreen.kt",
    )

private val USER_FACING_LITERAL_PATTERNS =
    listOf(
        Regex("""Text\(\s*"[^"]+""""),
        Regex("""DbCheckButton\(\s*text\s*=\s*"[^"]+""""),
        Regex("""contentDescription\s*=\s*"[^"]+""""),
        Regex("""String\.format\([^"]*"[^"]*[A-Za-z][^"]*""""),
    )

private fun File.readStringResources(): Map<String, String> {
    val document = parseXml()
    val nodes = document.documentElement.getElementsByTagName("string")
    return (0 until nodes.length)
        .map { nodes.item(it) as Element }
        .associate { element -> element.getAttribute("name") to element.textContent }
}

private fun File.readPluralResources(): Map<String, Map<String, String>> {
    val document = parseXml()
    val nodes = document.documentElement.getElementsByTagName("plurals")
    return (0 until nodes.length)
        .map { nodes.item(it) as Element }
        .associate { plurals ->
            val items = plurals.getElementsByTagName("item")
            plurals.getAttribute("name") to
                (0 until items.length)
                    .map { items.item(it) as Element }
                    .associate { item -> item.getAttribute("quantity") to item.textContent }
        }
}

private fun File.parseXml() = DocumentBuilderFactory
    .newInstance()
    .newDocumentBuilder()
    .parse(this)

private fun String.placeholders(): Set<String> = Regex("""%(\d+\$)?(\.\d+)?[dsf]""")
        .findAll(this)
        .map { it.value }
        .toSet()
