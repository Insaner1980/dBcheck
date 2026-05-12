package com.dbcheck.app.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HealthConnectHeartRateMapperTest {
    @Test
    fun filterForSessionKeepsSamplesInsideSessionWindow() {
        val start = Instant.parse("2026-05-09T09:00:00Z")
        val end = Instant.parse("2026-05-09T09:30:00Z")
        val samples =
            listOf(
                HeartRateSample(time = start.minusSeconds(1), beatsPerMinute = 66),
                HeartRateSample(time = start, beatsPerMinute = 70),
                HeartRateSample(time = start.plusSeconds(300), beatsPerMinute = 74),
                HeartRateSample(time = end, beatsPerMinute = 80),
            )

        val filtered = HealthConnectHeartRateMapper.filterForSession(samples, start, end)

        assertEquals(listOf(70L, 74L), filtered.map { it.beatsPerMinute })
    }

    @Test
    fun filterForSessionSortsSamplesByTime() {
        val start = Instant.parse("2026-05-09T09:00:00Z")
        val end = Instant.parse("2026-05-09T09:30:00Z")
        val late = HeartRateSample(time = start.plusSeconds(120), beatsPerMinute = 73)
        val early = HeartRateSample(time = start.plusSeconds(30), beatsPerMinute = 71)

        val filtered = HealthConnectHeartRateMapper.filterForSession(listOf(late, early), start, end)

        assertEquals(listOf(71L, 73L), filtered.map { it.beatsPerMinute })
    }
}
