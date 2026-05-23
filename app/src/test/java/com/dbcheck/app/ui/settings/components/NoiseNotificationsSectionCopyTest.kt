package com.dbcheck.app.ui.settings.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class NoiseNotificationsSectionCopyTest {
    @Test
    fun exposureDescriptionUsesConfiguredThresholdAndAverageRule() {
        val description = String.format(
            Locale.US,
            stringResourceValue("noise_notifications_exposure_description"),
            90,
        )

        assertEquals("Alert when 30 min average reaches 90 dB", description)
        assertFalse(description.contains("85"))
    }

    @Test
    fun peakDescriptionDoesNotPromiseSuddenDetection() {
        val description = stringResourceValue("noise_notifications_peak_description")

        assertEquals("Alert when peak reaches 120 dB", description)
        assertFalse(description.contains("sudden", ignoreCase = true))
    }

    @Test
    fun thresholdLabelsDoNotMarkDangerThresholdAsSafe() {
        assertEquals(
            "85 dB (default)",
            notificationThresholdValueLabel(
                notificationThreshold = 85,
                valueLabel = "85 dB",
                defaultValueLabel = "85 dB (default)",
            ),
        )
        assertEquals(
            "84 dB",
            notificationThresholdValueLabel(
                notificationThreshold = 84,
                valueLabel = "84 dB",
                defaultValueLabel = "84 dB (default)",
            ),
        )
        assertEquals("85 dB", String.format(Locale.US, stringResourceValue("notification_db_value"), 85))
    }

    @Test
    fun unitCopyUsesDbCasingAndSpacing() {
        assertEquals("LAST 7 DAYS (dB AVERAGE)", stringResourceValue("exposure_summary_last_7_days"))
        assertEquals("AVG dB/DAY", stringResourceValue("exposure_summary_avg_db_day"))
        assertEquals("PEAK dB", stringResourceValue("last_24_hours_peak_db"))
        assertEquals(
            "Use a room with a noise floor under 50 dB.",
            stringResourceValue("hearing_setup_find_silence_description"),
        )
    }

    @Test
    fun frequencyAxisCopyUsesUnitSpacing() {
        assertEquals("20 Hz", stringResourceValue("unit_20_hz"))
        assertEquals("1 kHz", stringResourceValue("unit_1_khz"))
        assertEquals("20 kHz", stringResourceValue("unit_20_khz"))
    }

    @Test
    fun notificationPlaceholderDurationMatchesSharedClockFormat() {
        assertEquals("Peak 0 dB · 0:00", stringResourceValue("notification_peak_duration_placeholder"))
    }

    @Test
    fun hearingHealthSafeCopyFramesExposureInsteadOfHearingStatus() {
        val safeCopy = stringResourceValue("hearing_health_safe")

        assertEquals("Weekly noise exposure is in the lower range.", safeCopy)
        assertFalse(safeCopy.contains("your hearing", ignoreCase = true))
    }

    @Test
    fun hearingResultSummaryFramesPersonalTrackingResult() {
        val summaryCopy = stringResourceValue("hearing_results_summary_range")

        assertEquals("PERSONAL TRACKING RESULT: %1\$s RANGE", summaryCopy)
        assertFalse(summaryCopy.contains("your hearing", ignoreCase = true))
    }

    @Test
    fun hearingSetupCopyAvoidsAccuracyClaims() {
        val setupCopy = stringResourceValue("hearing_setup_description")
        val silenceCopy = stringResourceValue("hearing_setup_find_silence_description")

        assertEquals("Use a quiet, consistent environment for personal tracking.", setupCopy)
        assertFalse(setupCopy.contains("accurate", ignoreCase = true))
        assertFalse(silenceCopy.contains("precision", ignoreCase = true))
    }

    @Test
    fun hearingShareCopyCarriesClinicalDisclaimer() {
        val shareCopy = stringResourceValue("share_hearing_results_text")

        assertTrue(shareCopy.contains("relative hearing thresholds for personal tracking"))
        assertTrue(shareCopy.contains("For clinical diagnosis, consult an audiologist."))
    }

    @Test
    fun hearingShareCardDrawsClinicalDisclaimer() {
        val source = projectFile("src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt").readText()

        assertTrue(source.contains("R.string.hearing_results_disclaimer"))
    }

    @Test
    fun hearingResultCopyAvoidsClinicalAudiogramTerminology() {
        assertFalse(stringResourceValue("a11y_audiogram_chart_empty").contains("audiogram", ignoreCase = true))
        assertFalse(stringResourceValue("a11y_audiogram_chart_with_data").contains("audiogram", ignoreCase = true))
        assertFalse(stringResourceValue("a11y_audiogram_ear_empty").contains("threshold", ignoreCase = true))
        assertFalse(stringResourceValue("a11y_audiogram_ear_thresholds").contains("threshold", ignoreCase = true))
        assertEquals("Avg. Relative Level", stringResourceValue("hearing_results_avg_threshold"))
        assertTrue(stringResourceValue("hearing_results_estimated_note").contains("not calibrated dB HL"))
    }

    @Test
    fun hearingResultMetricsAvoidUnsupportedInterpretationClaims() {
        val strings = projectFile("src/main/res/values/strings.xml").readText()
        val resultsSource =
            projectFile("src/main/java/com/dbcheck/app/ui/hearingtest/results/HearingTestResultsScreen.kt")
                .readText()

        assertFalse(strings.contains(">Speech Clarity*<"))
        assertFalse(strings.contains(">High Freq. Limit*<"))
        assertFalse(resultsSource.contains("state.speechClarity"))
        assertFalse(resultsSource.contains("state.highFreqLimit"))
        assertTrue(resultsSource.contains("R.string.hearing_results_tested_range"))
    }

    @Test
    fun hearingSpecsAvoidDbHlClassificationClaims() {
        val completeSpec = repoFile("dBcheck_complete_spec_v2.md").readText()
        val designSpec = repoFile("dBcheck_design_spec.md").readText()

        assertFalse(completeSpec.contains("dB HL"))
        assertFalse(completeSpec.contains("Produces audiogram"))
        assertFalse(completeSpec.contains("Results classified per WHO criteria"))
        assertFalse(designSpec.contains("Hearing test + audiogram"))
    }

    @Test
    fun measurementAccuracyCopyDoesNotOverstateLaeqOrClassInstrumentStatus() {
        val footerCopy = stringResourceValue("report_generated_footer")
        val healthConnectCopy = stringResourceValue("health_connect_disclosure_noise_body")

        assertEquals(
            "Generated by dBcheck v%1\$s - Not a calibrated Class 1/2 sound level meter",
            footerCopy,
        )
        assertFalse(footerCopy.contains("unless used with verified external microphone", ignoreCase = true))
        assertFalse(healthConnectCopy.contains("session LAeq"))
        assertEquals("30-DAY WEIGHTED dB TREND", stringResourceValue("monthly_trend_title"))
        assertEquals("AVG dB", stringResourceValue("monthly_trend_laeq"))
        assertEquals("12mo avg dB", stringResourceValue("yearly_report_12mo_laeq"))
        assertEquals("Adjust device mic offset", stringResourceValue("settings_audio_sensitivity_helper"))
        assertFalse(stringResourceValue("a11y_monthly_trend_chart_empty").contains("LAeq"))
    }

    private fun stringResourceValue(name: String): String {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(projectFile("src/main/res/values/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.attributes?.getNamedItem("name")?.nodeValue == name) {
                return node.textContent
            }
        }
        error("String resource not found: $name")
    }

    private fun repoFile(path: String): File = listOf(
            File(path),
            File("..", path),
        ).first(File::isFile)
}
