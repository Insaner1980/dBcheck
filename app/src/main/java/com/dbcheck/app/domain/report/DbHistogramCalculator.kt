package com.dbcheck.app.domain.report

data class DbHistogramBucket(
    val minDb: Int,
    val maxDb: Int,
    val sampleCount: Int,
    val percent: Int,
)

object DbHistogramCalculator {
    fun calculate(measurements: List<ReportMeasurement>): List<DbHistogramBucket> {
        val counts = IntArray(BUCKET_COUNT)
        measurements.forEach { measurement ->
            counts[measurement.dbWeighted.bucketIndex()] += 1
        }
        val percentages = roundedPercentages(counts)

        return List(BUCKET_COUNT) { index ->
            DbHistogramBucket(
                minDb = index * BUCKET_WIDTH_DB,
                maxDb = (index + 1) * BUCKET_WIDTH_DB,
                sampleCount = counts[index],
                percent = percentages[index],
            )
        }
    }

    private fun roundedPercentages(counts: IntArray): IntArray {
        val total = counts.sum()
        if (total <= 0) return IntArray(counts.size)

        val roundedBuckets =
            counts.mapIndexed { index, count ->
                val rawPercent = count.toDouble() * PERCENT_TOTAL / total.toDouble()
                RoundedBucketPercent(
                    index = index,
                    percent = rawPercent.toInt(),
                    remainder = rawPercent - rawPercent.toInt(),
                )
            }
        val missingPercent = PERCENT_TOTAL - roundedBuckets.sumOf { it.percent }
        val incrementedIndexes =
            roundedBuckets
                .sortedWith(compareByDescending<RoundedBucketPercent> { it.remainder }.thenBy { it.index })
                .take(missingPercent)
                .map { it.index }
                .toSet()

        return IntArray(counts.size) { index ->
            roundedBuckets[index].percent + if (index in incrementedIndexes) 1 else 0
        }
    }

    private fun Float.bucketIndex(): Int {
        val normalizedDb =
            if (isNaN()) {
                MIN_DB_FLOAT
            } else {
                coerceIn(MIN_DB_FLOAT, MAX_DB_FLOAT)
            }
        return (normalizedDb / BUCKET_WIDTH_DB).toInt().coerceIn(0, BUCKET_COUNT - 1)
    }

    private data class RoundedBucketPercent(
        val index: Int,
        val percent: Int,
        val remainder: Double,
    )

    private const val MIN_DB_FLOAT = 0f
    private const val MAX_DB_FLOAT = 130f
    private const val BUCKET_WIDTH_DB = 10
    private const val BUCKET_COUNT = 13
    private const val PERCENT_TOTAL = 100
}
