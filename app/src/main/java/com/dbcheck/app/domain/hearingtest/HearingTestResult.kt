package com.dbcheck.app.domain.hearingtest

data class HearingTestResult(
    val id: Long = 0,
    val timestamp: Long,
    val overallScore: Int,
    val rating: String,
    val leftEarThresholds: List<Pair<Float, Float>>,
    val rightEarThresholds: List<Pair<Float, Float>>,
    val speechClarity: Float,
    val highFreqLimit: Float,
    val avgThreshold: Float,
)

enum class Ear {
    LEFT,
    RIGHT,
}

data class TestKey(val ear: Ear, val frequencyHz: Float)

val TEST_FREQUENCIES = listOf(250f, 500f, 1000f, 2000f, 4000f, 8000f)

object HearingTestThresholdCodec {
    fun serializeEarData(thresholds: Map<TestKey, Float>, ear: Ear): String = thresholds
            .filterKeys { it.ear == ear }
            .toList()
            .sortedBy { (key, _) -> key.frequencyHz }
            .joinToString(separator = ",") { (key, threshold) -> "${key.frequencyHz}:$threshold" }

    fun parseEarData(data: String): List<Pair<Float, Float>> = data
            .split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size != 2) {
                    return@mapNotNull null
                }
                val frequency = parts[0].toFloatOrNull() ?: return@mapNotNull null
                val threshold = parts[1].toFloatOrNull() ?: return@mapNotNull null
                frequency to threshold
            }.sortedBy { it.first }
}

object HearingTestResultCalculator {
    fun build(thresholds: Map<TestKey, Float>, timestamp: Long): HearingTestResult {
        val allThresholds = thresholds.values.toList()
        val avgThreshold = if (allThresholds.isNotEmpty()) allThresholds.average().toFloat() else 0f
        val normalizedThreshold = (-avgThreshold).coerceIn(MIN_NORMALIZED_THRESHOLD, MAX_NORMALIZED_THRESHOLD)
        val overallScore = ((normalizedThreshold / MAX_NORMALIZED_THRESHOLD) * SCORE_MAX).toInt()

        return HearingTestResult(
            timestamp = timestamp,
            overallScore = overallScore,
            rating = ratingFor(overallScore),
            leftEarThresholds = thresholds.toEarThresholds(Ear.LEFT),
            rightEarThresholds = thresholds.toEarThresholds(Ear.RIGHT),
            speechClarity = (overallScore.toFloat() / SCORE_MAX * SPEECH_CLARITY_MAX).coerceIn(0f, SCORE_MAX),
            highFreqLimit = DEFAULT_HIGH_FREQUENCY_LIMIT_HZ,
            avgThreshold = avgThreshold,
        )
    }

    private fun Map<TestKey, Float>.toEarThresholds(ear: Ear): List<Pair<Float, Float>> = filterKeys { it.ear == ear }
            .map { (key, threshold) -> key.frequencyHz to threshold }
            .sortedBy { it.first }

    private fun ratingFor(overallScore: Int): String = when {
            overallScore >= EXCELLENT_SCORE -> "Excellent"
            overallScore >= GOOD_SCORE -> "Good"
            overallScore >= FAIR_SCORE -> "Fair"
            else -> "Poor"
        }

    private const val MIN_NORMALIZED_THRESHOLD = 0f
    private const val MAX_NORMALIZED_THRESHOLD = 60f
    private const val SCORE_MAX = 100f
    private const val SPEECH_CLARITY_MAX = 98f
    private const val DEFAULT_HIGH_FREQUENCY_LIMIT_HZ = 17_400f
    private const val EXCELLENT_SCORE = 90
    private const val GOOD_SCORE = 75
    private const val FAIR_SCORE = 50
}
