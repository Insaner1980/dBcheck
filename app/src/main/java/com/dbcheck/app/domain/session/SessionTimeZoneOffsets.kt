package com.dbcheck.app.domain.session

import java.time.Instant
import java.time.ZoneId

data class SessionTimeZoneOffsets(val startUtcOffsetSeconds: Int? = null, val endUtcOffsetSeconds: Int? = null) {
    fun offsetForTimestamp(timestampMs: Long, startTimeMs: Long, endTimeMs: Long): Int? = when {
        timestampMs <= startTimeMs -> startUtcOffsetSeconds
        timestampMs >= endTimeMs -> endUtcOffsetSeconds
        startUtcOffsetSeconds == endUtcOffsetSeconds -> startUtcOffsetSeconds
        else -> null
    }
}

object SessionTimeZoneOffsetResolver {
    fun offsetSecondsAt(timestampMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int =
        zoneId.rules.getOffset(Instant.ofEpochMilli(timestampMs)).totalSeconds
}
