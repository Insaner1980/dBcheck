package com.dbcheck.app.domain.analytics

import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

internal data class HistoricalExposureInterval(val startMs: Long, val endMs: Long, val db: Float) {
    val durationMs: Long get() = endMs - startMs
}

/**
 * A-weighted persistence estimates, not a reconstruction of live accumulated energy.
 * Hold an observation until the next distinct timestamp in the same session. The sampler
 * saves the first reading and flushes the last on stop: neither endpoint justifies extra time.
 * Duplicate timestamps use the last row in DAO timestamp/id order and add no duration.
 * Session gaps and time outside the query/session bounds never contribute coverage.
 */
internal object HistoricalExposureIntervals {
    fun from(points: List<WeightedExposureMeasurement>): List<HistoricalExposureInterval> = points
        .groupBy { it.sessionId }.values.flatMap { session ->
            session.groupBy { it.timestamp }.toSortedMap().values.map { it.last() }
                .zipWithNext().mapNotNull { (previous, next) ->
                    val start = maxOf(previous.timestamp, previous.coverageStartMs, next.coverageStartMs)
                    val end = minOf(next.timestamp, previous.coverageEndMs, next.coverageEndMs)
                    if (previous.frequencyWeighting == "A" && next.frequencyWeighting == "A" && end > start) {
                        HistoricalExposureInterval(start, end, previous.dbWeighted)
                    } else {
                        null
                    }
                }
        }

    fun average(intervals: List<HistoricalExposureInterval>): Float? = DecibelMath.energyAverageDb(
        totalEnergy = intervals.sumOf { DecibelMath.energyFromDb(it.db) * it.durationMs },
        weight = intervals.sumOf { it.durationMs }.toDouble(),
    )

    fun buckets(
        points: List<WeightedExposureMeasurement>,
        zoneId: ZoneId,
        hourly: Boolean,
    ): Map<Long, List<HistoricalExposureInterval>> {
        val buckets = sortedMapOf<Long, MutableList<HistoricalExposureInterval>>()
        from(points).forEach { interval ->
            var start = interval.startMs
            while (start < interval.endMs) {
                val local = Instant.ofEpochMilli(start).atZone(zoneId)
                val bucketStart = bucketStart(start, zoneId, hourly)
                var nextStart = if (hourly) {
                    local.plusHours(1).withMinute(0).withSecond(0).withNano(0)
                } else {
                    bucketStart.plusDays(1)
                }
                // A partial-hour offset rollback can resolve the truncated hour before this interval.
                while (nextStart.toInstant().toEpochMilli() <= start) {
                    nextStart = nextStart.plusHours(1)
                }
                val end = minOf(interval.endMs, nextStart.toInstant().toEpochMilli())
                buckets.getOrPut(bucketStart.toInstant().toEpochMilli()) { mutableListOf() }
                    .add(interval.copy(startMs = start, endMs = end))
                start = end
            }
        }
        return buckets
    }

    fun bucketStart(timestampMs: Long, zoneId: ZoneId, hourly: Boolean): ZonedDateTime {
        val local = Instant.ofEpochMilli(timestampMs).atZone(zoneId)
        return if (hourly) local.withMinute(0).withSecond(0).withNano(0)
            else local.toLocalDate().atStartOfDay(zoneId)
    }

    // These counts are milliseconds for historical exposure; live environment counts remain observations.
    fun zoneDurations(intervals: List<HistoricalExposureInterval>): EnvironmentExposureMixCounts =
        intervals.fold(EnvironmentExposureMixCounts()) { counts, interval ->
            val duration = interval.durationMs
            val total = counts.totalCount + duration
            when (NoiseLevel.fromDb(interval.db)) {
                NoiseLevel.QUIET -> counts.copy(quietCount = counts.quietCount + duration, totalCount = total)
                NoiseLevel.NORMAL -> counts.copy(moderateCount = counts.moderateCount + duration, totalCount = total)
                NoiseLevel.ELEVATED -> counts.copy(loudCount = counts.loudCount + duration, totalCount = total)
                NoiseLevel.DANGEROUS -> counts.copy(criticalCount = counts.criticalCount + duration, totalCount = total)
            }
        }
}
