package com.dbcheck.app.ui.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDailyWindowTest {
    @Test
    fun localDayUsesNextMidnightAcrossDaylightSavingChanges() {
        val zone = ZoneId.of("Europe/Helsinki")
        listOf("2026-03-29T12:00:00Z" to 23L, "2026-10-25T12:00:00Z" to 25L).forEach { (time, hours) ->
            val range = todayRangeMillis(zone, Instant.parse(time).toEpochMilli())
            assertEquals(hours * 3_600_000L, range.endTimeMs - range.startTimeMs)
        }
    }

    @Test
    fun dayFlowRefreshesAfterMidnightAndTimeZoneChangeWithoutDuplicateQueries() = runTest {
        var now = Instant.parse("2026-10-06T20:59:00Z").toEpochMilli()
        var zone = ZoneId.of("Europe/Helsinki")
        val ranges = mutableListOf<TimeRangeMillis>()
        backgroundScope.launch { currentDayRanges({ now }, { zone }).collect { ranges.add(it) } }
        runCurrent()
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(1, ranges.size)
        now += 120_000L
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(2, ranges.size)
        assertEquals(ranges[0].endTimeMs, ranges[1].startTimeMs)
        zone = ZoneId.of("UTC")
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(3, ranges.size)
        assertEquals(todayRangeMillis(zone, now), ranges.last())
    }
}
