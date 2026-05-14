package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.domain.noise.DecibelMath
import org.junit.Assert.assertEquals
import org.junit.Test

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
        assertEquals(DecibelMath.energyAverageDb(listOf(60f, 70f)) ?: 0f, averages[0].avgDb, 0.001f)
        assertEquals(70f, averages[0].maxDb, 0.001f)
        assertEquals(2, averages[0].sampleCount)
        assertEquals(1, averages[1].hour)
        assertEquals(80f, averages[1].avgDb, 0.001f)
    }

    @Test
    fun dailyAveragesUseEnergyAverageWithinEachDay() {
        val points =
            listOf(
                WeightedMeasurementPoint(timestamp = 0L, dbWeighted = 60f),
                WeightedMeasurementPoint(timestamp = 1_000L, dbWeighted = 70f),
                WeightedMeasurementPoint(timestamp = DAY_MS, dbWeighted = 80f),
            )

        val averages = MeasurementBucketAverages.daily(points)

        assertEquals(2, averages.size)
        assertEquals(0L, averages[0].dayStartMs)
        assertEquals(DecibelMath.energyAverageDb(listOf(60f, 70f)) ?: 0f, averages[0].avgDb, 0.001f)
        assertEquals(70f, averages[0].maxDb, 0.001f)
        assertEquals(2, averages[0].sampleCount)
        assertEquals(DAY_MS, averages[1].dayStartMs)
        assertEquals(80f, averages[1].avgDb, 0.001f)
    }

    private companion object {
        const val HOUR_MS = 60L * 60L * 1_000L
        const val DAY_MS = 24L * HOUR_MS
    }
}
