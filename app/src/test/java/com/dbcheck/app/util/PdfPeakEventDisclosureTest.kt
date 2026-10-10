package com.dbcheck.app.util

import android.graphics.Canvas
import android.graphics.Paint
import com.dbcheck.app.domain.report.PeakEvent
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.testSessionReportData
import com.dbcheck.app.testStringContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PdfPeakEventDisclosureTest {
    @Test
    fun cappedEventsDiscloseTotalAndKeepAllVisibleTextAboveFooter() {
        val text = renderEvents(12)

        assertTrue(text.any { it.first == "Peak events shown: 8 of 12." })
        assertEquals(8, text.count { it.first.matches(Regex("\\d+\\. .* dB")) })
        assertTrue(text.all { it.second < 780f })
    }

    @Test
    fun completeEventListsDoNotClaimTruncation() {
        listOf(0, 1, 8).forEach { count ->
            val text = renderEvents(count)
            assertEquals(count, text.count { it.first.matches(Regex("\\d+\\. .* dB")) })
            assertTrue(text.none { it.first.startsWith("Peak events shown:") })
        }
    }

    private fun renderEvents(count: Int): List<Pair<String, Float>> {
        val context = testStringContext()
        every { context.getString(any(), 8, 12) } returns "Peak events shown: 8 of 12."
        val canvas = mockk<Canvas>(relaxed = true)
        val text = mutableListOf<Pair<String, Float>>()
        every { canvas.drawText(any<String>(), any<Float>(), any<Float>(), any<Paint>()) } answers {
            text += firstArg<String>() to thirdArg<Float>()
        }
        val styleClass = Class.forName("com.dbcheck.app.util.PdfReportStyle")
        val style = mockkClass(styleClass.kotlin, relaxed = true)
        val report = testSessionReportData(peakEvents = List(count) { PeakEvent(0L, 1L, 0L, 90f) })
        val method = ExportPdfReportUseCase::class.java.getDeclaredMethod(
            "drawPeakEvents", Canvas::class.java, SessionReportData::class.java, List::class.java, styleClass,
        ).apply { isAccessible = true }
        method.invoke(
            ExportPdfReportUseCase(context, UnconfinedTestDispatcher()),
            canvas,
            report,
            report.peakEvents.take(8),
            style,
        )
        return text
    }
}
