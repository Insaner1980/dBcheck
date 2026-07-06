package com.dbcheck.app.domain.noise

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZonedDateTime

class NoiseNotificationScheduleTest {
    @Test
    fun crossingMidnightScheduleTreatsMorningAsPreviousActiveDay() {
        val schedule =
            NoiseNotificationSchedule(
                activeDays = setOf(DayOfWeek.MONDAY),
                startMinuteOfDay = 22 * MINUTES_PER_HOUR,
                endMinuteOfDay = 6 * MINUTES_PER_HOUR,
            )

        assertFalse(schedule.isActiveAt(time("2026-06-22T21:59:00Z")))
        assertTrue(schedule.isActiveAt(time("2026-06-22T23:00:00Z")))
        assertTrue(schedule.isActiveAt(time("2026-06-23T02:00:00Z")))
        assertFalse(schedule.isActiveAt(time("2026-06-23T06:00:00Z")))
        assertFalse(schedule.isActiveAt(time("2026-06-23T23:00:00Z")))
    }

    @Test
    fun sameDayScheduleUsesCurrentDayAndExclusiveEndMinute() {
        val schedule =
            NoiseNotificationSchedule(
                activeDays = setOf(DayOfWeek.TUESDAY),
                startMinuteOfDay = 8 * MINUTES_PER_HOUR,
                endMinuteOfDay = 17 * MINUTES_PER_HOUR,
            )

        assertFalse(schedule.isActiveAt(time("2026-06-23T07:59:00Z")))
        assertTrue(schedule.isActiveAt(time("2026-06-23T08:00:00Z")))
        assertTrue(schedule.isActiveAt(time("2026-06-23T16:59:00Z")))
        assertFalse(schedule.isActiveAt(time("2026-06-23T17:00:00Z")))
        assertFalse(schedule.isActiveAt(time("2026-06-24T09:00:00Z")))
    }

    @Test
    fun matchingStartAndEndMeansFullSelectedDay() {
        val schedule =
            NoiseNotificationSchedule(
                activeDays = setOf(DayOfWeek.WEDNESDAY),
                startMinuteOfDay = 0,
                endMinuteOfDay = 0,
            )

        assertTrue(schedule.isActiveAt(time("2026-06-24T00:00:00Z")))
        assertTrue(schedule.isActiveAt(time("2026-06-24T23:59:00Z")))
        assertFalse(schedule.isActiveAt(time("2026-06-25T00:00:00Z")))
    }

    @Test
    fun emptyActiveDaysAreNeverActive() {
        val schedule =
            NoiseNotificationSchedule(
                activeDays = emptySet(),
                startMinuteOfDay = 0,
                endMinuteOfDay = 0,
            )

        assertFalse(schedule.isActiveAt(time("2026-06-24T12:00:00Z")))
    }

    private fun time(value: String): ZonedDateTime = ZonedDateTime.parse(value)

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
