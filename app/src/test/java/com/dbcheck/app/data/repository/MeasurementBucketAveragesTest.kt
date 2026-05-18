package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.domain.noise.DecibelMath
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import kotlin.math.log10

class MeasurementBucketAveragesTest {
    @Test
    fun hourlyAveragesUseEnergyAverageWithinEachHour() {
        val points =
            listOf(
                WeightedMeasurementPoint(timestamp = 0L, dbWeighted = 60f),
                WeightedMeasurementPoint(timestamp = 1_000L, dbWeighted = 70f),
                WeightedMeasurementPoint(timestamp = HOUR_MS, dbWeighted = 80f),
            )

        val averages = MeasurementBucketAverages.hourly(points)

        assertEquals(2, averages.size)
        assertEquals(0, averages[0].hour)
        assertTwoBucketEnergyMetrics(averages[0].avgDb, averages[0].maxDb, averages[0].sampleCount, averages[1].avgDb)
        assertEquals(1, averages[1].hour)
    }

    @Test
    fun dailyAveragesUseEnergyAverageWithinEachDay() {
        val points =
            listOf(
                WeightedMeasurementPoint(timestamp = 0L, dbWeighted = 60f),
                WeightedMeasurementPoint(timestamp = 1_000L, dbWeighted = 70f),
                WeightedMeasurementPoint(timestamp = DAY_MS, dbWeighted = 80f),
            )

        val averages = MeasurementBucketAverages.daily(points, ZoneId.of("UTC"))

        assertEquals(2, averages.size)
        assertEquals(0L, averages[0].dayStartMs)
        assertTwoBucketEnergyMetrics(averages[0].avgDb, averages[0].maxDb, averages[0].sampleCount, averages[1].avgDb)
        assertEquals(DAY_MS, averages[1].dayStartMs)
    }

    @Test
    fun dailyAveragesUseLocalDayStartForSystemZone() {
        val originalTimeZone = TimeZone.getDefault()
        val zoneId = ZoneId.of("Europe/Helsinki")
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
            val localMidnightSample =
                LocalDate
                    .of(2026, 5, 14)
                    .atTime(0, 30)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()

            val averages =
                MeasurementBucketAverages.daily(
                    listOf(WeightedMeasurementPoint(timestamp = localMidnightSample, dbWeighted = 65f)),
                )

            assertEquals(
                LocalDate
                    .of(2026, 5, 14)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli(),
                averages.single().dayStartMs,
            )
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun hourlyAveragesWeightForcedRowsByElapsedTime() {
        val points =
            listOf(
                WeightedMeasurementPoint(timestamp = 0L, dbWeighted = 60f),
                WeightedMeasurementPoint(timestamp = 100L, dbWeighted = 120f),
                WeightedMeasurementPoint(timestamp = 1_000L, dbWeighted = 60f),
            )

        val averages = MeasurementBucketAverages.hourly(points)

        assertEquals(
            weightedEnergyAverage(
                60f to 100L,
                120f to 100L,
                60f to 900L,
            ),
            averages.single().avgDb,
            0.001f,
        )
        assertEquals(3, averages.single().sampleCount)
    }

    @Test
    fun hourlyAveragesStayChronologicalAcrossMidnight() {
        val zoneId = ZoneId.of("UTC")
        val points =
            listOf(
                WeightedMeasurementPoint(timestamp = hourStart("2026-05-14T23:00:00Z") + 30_000L, dbWeighted = 70f),
                WeightedMeasurementPoint(timestamp = hourStart("2026-05-15T00:00:00Z") + 30_000L, dbWeighted = 80f),
            )

        val averages = MeasurementBucketAverages.hourly(points, zoneId)

        assertEquals(listOf(23, 0), averages.map { it.hour })
        assertEquals(70f, averages[0].avgDb, 0.001f)
        assertEquals(80f, averages[1].avgDb, 0.001f)
    }

    private fun weightedEnergyAverage(vararg weightedValues: Pair<Float, Long>): Float {
        val totalWeight = weightedValues.sumOf { it.second }.toDouble()
        val totalEnergy = weightedValues.sumOf { (db, weight) -> DecibelMath.energyFromDb(db) * weight }
        return (10.0 * log10(totalEnergy / totalWeight)).toFloat()
    }

    private fun assertTwoBucketEnergyMetrics(
        actualFirstAvgDb: Float,
        actualMaxDb: Float,
        actualSampleCount: Int,
        actualSecondAvgDb: Float,
    ) {
        assertEquals(DecibelMath.energyAverageDb(listOf(60f, 70f)) ?: 0f, actualFirstAvgDb, 0.001f)
        assertEquals(70f, actualMaxDb, 0.001f)
        assertEquals(2, actualSampleCount)
        assertEquals(80f, actualSecondAvgDb, 0.001f)
    }

    private fun hourStart(instant: String): Long = java.time.Instant.parse(instant).toEpochMilli()

    private companion object {
        const val HOUR_MS = 60L * 60L * 1_000L
        const val DAY_MS = 24L * HOUR_MS
    }
}
