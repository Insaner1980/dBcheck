package com.dbcheck.app.domain.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DbHistogramCalculatorTest {
    @Test
    fun emptyDataReturnsStableTenDbBucketsWithZeroPercentages() {
        val buckets = DbHistogramCalculator.calculate(emptyList())

        assertEquals(13, buckets.size)
        assertEquals(DbHistogramBucket(minDb = 0, maxDb = 10, sampleCount = 0, percent = 0), buckets.first())
        assertEquals(DbHistogramBucket(minDb = 120, maxDb = 130, sampleCount = 0, percent = 0), buckets.last())
        assertTrue(buckets.all { it.sampleCount == 0 && it.percent == 0 })
    }

    @Test
    fun bucketEdgesAreLowerInclusiveAndFinalBucketCatchesClampedExtremes() {
        val buckets =
            DbHistogramCalculator.calculate(
                listOf(-5f, 0f, 9.9f, 10f, 119.9f, 120f, 130f, 131f).mapIndexed { index, db ->
                    measurement(timestamp = index.toLong(), db = db)
                },
            )

        assertEquals(3, buckets.bucket(0).sampleCount)
        assertEquals(1, buckets.bucket(10).sampleCount)
        assertEquals(1, buckets.bucket(110).sampleCount)
        assertEquals(3, buckets.bucket(120).sampleCount)
    }

    @Test
    fun percentagesRoundToOneHundredInStableBucketOrder() {
        val buckets =
            DbHistogramCalculator.calculate(
                listOf(5f, 15f, 25f).mapIndexed { index, db ->
                    measurement(timestamp = index.toLong(), db = db)
                },
            )

        assertEquals(100, buckets.sumOf { it.percent })
        assertEquals(34, buckets.bucket(0).percent)
        assertEquals(33, buckets.bucket(10).percent)
        assertEquals(33, buckets.bucket(20).percent)
    }

    private fun List<DbHistogramBucket>.bucket(minDb: Int): DbHistogramBucket =
        first { it.minDb == minDb }

    private fun measurement(timestamp: Long, db: Float): ReportMeasurement =
        ReportMeasurement(timestamp = timestamp, dbWeighted = db)
}
