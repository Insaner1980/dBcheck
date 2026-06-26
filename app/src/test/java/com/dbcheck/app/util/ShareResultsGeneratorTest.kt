package com.dbcheck.app.util

import android.content.Intent
import com.dbcheck.app.R
import com.dbcheck.app.testSessionReportData
import com.dbcheck.app.testStringContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareResultsGeneratorTest {
    @After
    fun tearDown() {
        unmockkConstructor(Intent::class)
    }

    @Test
    fun sessionStatsShareContentContainsActionTypeStatsAndFormattedDuration() {
        val content =
            buildSessionStatsShareContent(
                text = "I measured 72.4 dB LCeq (LCpeak: 91.2 dB) in my 1:05 session with dBcheck",
            )

        assertEquals("android.intent.action.SEND", content.action)
        assertEquals("text/plain", content.type)
        assertTrue(content.text.contains("72.4 dB LCeq"))
        assertTrue(content.text.contains("LCpeak: 91.2 dB"))
        assertTrue(content.text.contains("1:05 session"))
    }

    @Test
    fun shareSessionStatsFormatsEquivalentLevelAndLcPeakWithOneDecimal() = runTest {
        val context = testStringContext()
        mockShareIntentConstruction()
        every {
            context.getString(
                R.string.share_meter_results_text,
                "72.4",
                "LCeq",
                "91.2",
                "1:05",
            )
        } returns "I measured 72.4 dB LCeq (LCpeak: 91.2 dB) in my 1:05 session with dBcheck"
        val generator = ShareResultsGenerator(context, UnconfinedTestDispatcher())

        generator.shareSessionStats(
            avgDb = 72.4f,
            peakDb = 91.2f,
            durationMs = 65_000L,
            equivalentLevelLabel = "LCeq",
        )

        verify {
            context.getString(R.string.share_meter_results_text, "72.4", "LCeq", "91.2", "1:05")
            anyConstructed<Intent>().setType("text/plain")
            anyConstructed<Intent>().putExtra(
                Intent.EXTRA_TEXT,
                "I measured 72.4 dB LCeq (LCpeak: 91.2 dB) in my 1:05 session with dBcheck",
            )
        }
    }

    @Test
    fun shareTextEllipsizesToAvailableWidth() {
        val text = "Very long session name that cannot fit"

        val ellipsized =
            ellipsizeShareText(
                text = text,
                maxWidth = 16f,
                measureText = { value -> value.length.toFloat() },
            )

        assertEquals("Very long ses...", ellipsized)
        assertTrue(ellipsized.length <= 16)
    }

    @Test
    fun sessionReportShareFileNameUsesBoundedSlugAndSessionId() {
        val report =
            sessionReportData(
                sessionId = 42L,
                sessionName =
                    "Workshop main hall with a very long descriptive name " +
                        "that would otherwise produce an oversized file name",
            )

        val fileName = buildSessionReportShareFileName(report)

        assertTrue(fileName.startsWith("dBcheck_session_report_42_"))
        assertTrue(fileName.endsWith(".png"))
        assertTrue(fileName.length <= 82)
    }

    @Test
    fun sessionReportShareTextUsesEquivalentLevelLabelFromReport() {
        val report =
            sessionReportData(
                sessionName = "Workshop",
                weighting = "C",
                equivalentLevelLabel = "LCeq",
            )

        val text =
            buildSessionReportShareText(
                template = "dBcheck session report for %1\$s: %2\$s dB %3\$s",
                report = report,
            )

        assertEquals("dBcheck session report for Workshop: 72.6 dB LCeq", text)
    }

    private fun sessionReportData(
        sessionId: Long = 1L,
        sessionName: String = "Session",
        weighting: String = "A",
        equivalentLevelLabel: String = "LAeq",
    ) = testSessionReportData(
            sessionId = sessionId,
            sessionName = sessionName,
            sessionCustomName = sessionName,
            startTime = 1_700_000_000_000L,
            endTime = 1_700_000_065_000L,
            generatedAtMs = 1_700_000_065_000L,
            durationMs = 65_000L,
            weighting = weighting,
            equivalentLevelLabel = equivalentLevelLabel,
            minDb = 50f,
            maxDb = 80f,
            laeqDb = 72.6f,
            lcPeakDb = 91.4f,
            twaDb = 65f,
            dosePercent = 12f,
        )

    private fun mockShareIntentConstruction() {
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setType(any()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().putExtra(Intent.EXTRA_TEXT, any<String>()) } returns mockk(relaxed = true)
    }
}
