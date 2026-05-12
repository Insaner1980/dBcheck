package com.dbcheck.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareResultsGeneratorTest {
    @Test
    fun sessionStatsShareContentContainsActionTypeStatsAndFormattedDuration() {
        val content =
            buildSessionStatsShareContent(
                avgDb = 72.6f,
                peakDb = 91.4f,
                durationMs = 65_000L,
            )

        assertEquals("android.intent.action.SEND", content.action)
        assertEquals("text/plain", content.type)
        assertTrue(content.text.contains("72 dB avg"))
        assertTrue(content.text.contains("peak: 91 dB"))
        assertTrue(content.text.contains("1:05 session"))
    }
}
