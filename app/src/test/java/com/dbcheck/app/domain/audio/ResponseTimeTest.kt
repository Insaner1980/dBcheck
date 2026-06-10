package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponseTimeTest {
    @Test
    fun responseTimesExposeConfiguredTimeConstants() {
        assertEquals(200L, ResponseTime.FAST.timeConstantMs)
        assertEquals(500L, ResponseTime.SLOW.timeConstantMs)
        assertEquals(50L, ResponseTime.IMPULSE.timeConstantMs)
    }

    @Test
    fun responseTimeFallsBackToFastForUnknownPreference() {
        assertEquals(ResponseTime.FAST, ResponseTime.fromPreference(null))
        assertEquals(ResponseTime.FAST, ResponseTime.fromPreference("fast"))
        assertEquals(ResponseTime.FAST, ResponseTime.fromPreference("unexpected"))
    }
}
