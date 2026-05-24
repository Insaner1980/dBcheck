package com.dbcheck.app

import com.dbcheck.app.domain.hearingtest.HearingTestResult

internal fun testHearingResult(
    id: Long = 42L,
    timestamp: Long = 1_700_000_000_000L,
    overallScore: Int = 86,
    rating: String = "Good",
    leftEarThresholds: List<Pair<Float, Float>> = listOf(1_000f to -30f),
    rightEarThresholds: List<Pair<Float, Float>> = listOf(1_000f to -25f),
    speechClarity: Float = 84f,
    highFreqLimit: Float = 16_000f,
    avgThreshold: Float = -27.5f,
): HearingTestResult = HearingTestResult(
    id = id,
    timestamp = timestamp,
    overallScore = overallScore,
    rating = rating,
    leftEarThresholds = leftEarThresholds,
    rightEarThresholds = rightEarThresholds,
    speechClarity = speechClarity,
    highFreqLimit = highFreqLimit,
    avgThreshold = avgThreshold,
)
