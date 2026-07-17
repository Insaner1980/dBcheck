package com.dbcheck.app.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class SessionTimeZoneOffsetsTest {
    @Test
    fun resolverUsesZoneRulesAtTheRecordedInstant() {
        val helsinki = ZoneId.of("Europe/Helsinki")

        assertEquals(7_200, SessionTimeZoneOffsetResolver.offsetSecondsAt(WINTER_INSTANT_MS, helsinki))
        assertEquals(10_800, SessionTimeZoneOffsetResolver.offsetSecondsAt(SUMMER_INSTANT_MS, helsinki))
    }

    @Test
    fun timestampOffsetUsesBoundaryOffsetsAndFallsBackToUtcWhenOffsetChangesMidSession() {
        val offsets = SessionTimeZoneOffsets(startUtcOffsetSeconds = 7_200, endUtcOffsetSeconds = 10_800)

        assertEquals(7_200, offsets.offsetForTimestamp(START_MS, START_MS, END_MS))
        assertNull(offsets.offsetForTimestamp(START_MS + 500L, START_MS, END_MS))
        assertEquals(10_800, offsets.offsetForTimestamp(END_MS, START_MS, END_MS))
    }

    private companion object {
        const val WINTER_INSTANT_MS = 1_704_067_200_000L
        const val SUMMER_INSTANT_MS = 1_719_792_000_000L
        const val START_MS = 1_000L
        const val END_MS = 2_000L
    }
}
