package com.dbcheck.app.domain.noise

import java.time.DayOfWeek
import java.time.ZonedDateTime

data class NoiseNotificationSchedule(
    val activeDays: Set<DayOfWeek> = ALL_DAYS,
    val startMinuteOfDay: Int = FULL_DAY_MINUTE,
    val endMinuteOfDay: Int = FULL_DAY_MINUTE,
) {
    init {
        require(startMinuteOfDay in MIN_MINUTE_OF_DAY..MAX_MINUTE_OF_DAY) {
            "startMinuteOfDay must be within a day"
        }
        require(endMinuteOfDay in MIN_MINUTE_OF_DAY..MAX_MINUTE_OF_DAY) {
            "endMinuteOfDay must be within a day"
        }
    }

    val isFullDay: Boolean
        get() = startMinuteOfDay == endMinuteOfDay

    fun isActiveAt(timestamp: ZonedDateTime): Boolean = activeDays.isNotEmpty() && isInsideActiveWindow(timestamp)

    private fun isInsideActiveWindow(timestamp: ZonedDateTime): Boolean {
        val minuteOfDay = timestamp.hour * MINUTES_PER_HOUR + timestamp.minute
        return when {
            isFullDay -> timestamp.dayOfWeek in activeDays

            startMinuteOfDay < endMinuteOfDay ->
                timestamp.dayOfWeek in activeDays &&
                    minuteOfDay >= startMinuteOfDay &&
                    minuteOfDay < endMinuteOfDay

            else -> {
                val previousDay = timestamp.dayOfWeek.minus(1)
                (timestamp.dayOfWeek in activeDays && minuteOfDay >= startMinuteOfDay) ||
                    (previousDay in activeDays && minuteOfDay < endMinuteOfDay)
            }
        }
    }

    companion object {
        const val MIN_MINUTE_OF_DAY = 0
        const val MAX_MINUTE_OF_DAY = 23 * 60 + 59
        const val FULL_DAY_MINUTE = 0

        val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.values().toSet()

        fun normalized(
            activeDays: Set<DayOfWeek>?,
            startMinuteOfDay: Int?,
            endMinuteOfDay: Int?,
        ): NoiseNotificationSchedule = NoiseNotificationSchedule(
                activeDays = activeDays ?: ALL_DAYS,
                startMinuteOfDay = startMinuteOfDay?.coerceIn(MIN_MINUTE_OF_DAY, MAX_MINUTE_OF_DAY)
                    ?: FULL_DAY_MINUTE,
                endMinuteOfDay = endMinuteOfDay?.coerceIn(MIN_MINUTE_OF_DAY, MAX_MINUTE_OF_DAY)
                    ?: FULL_DAY_MINUTE,
            )

        fun activeDaysFromPreference(value: String?): Set<DayOfWeek> = when {
                value == null -> ALL_DAYS

                value.isBlank() -> emptySet()

                else ->
                    value
                        .split(ACTIVE_DAY_SEPARATOR)
                        .mapNotNull { token -> token.trim().toIntOrNull()?.toDayOfWeekOrNull() }
                        .toSet()
                        .ifEmpty { ALL_DAYS }
            }

        fun activeDaysPreferenceValue(activeDays: Set<DayOfWeek>): String = activeDays
                .sortedBy { day -> day.value }
                .joinToString(ACTIVE_DAY_SEPARATOR.toString()) { day -> day.value.toString() }

        private const val MINUTES_PER_HOUR = 60
        private const val ACTIVE_DAY_SEPARATOR = ','
    }
}

private fun Int.toDayOfWeekOrNull(): DayOfWeek? = runCatching { DayOfWeek.of(this) }.getOrNull()
