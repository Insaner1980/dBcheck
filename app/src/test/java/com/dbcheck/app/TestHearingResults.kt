package com.dbcheck.app

import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
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

internal fun testRecoveryBaselineResult(id: Long = 1L): HearingTestResult {
    val thresholds = listOf(1000f to -30f, 4000f to -30f, 8000f to -30f)
    return testHearingResult(
        id = id,
        timestamp = 1_000L,
        leftEarThresholds = thresholds,
        rightEarThresholds = thresholds,
        highFreqLimit = 8000f,
        avgThreshold = -30f,
    )
}

internal fun testHearingRecoveryResult(
    id: Long = 2L,
    baselineTestId: Long = 1L,
    timestamp: Long = 2_000L,
    testedFrequencyCount: Int = 6,
    averageShiftDb: Float = 6f,
    maxShiftDb: Float = 12f,
    status: HearingRecoveryStatus = HearingRecoveryStatus.SMALL_SHIFT,
    leftEarShifts: List<Pair<Float, Float>> = emptyList(),
    rightEarShifts: List<Pair<Float, Float>> = emptyList(),
): HearingRecoveryResult = HearingRecoveryResult(
    id = id,
    baselineTestId = baselineTestId,
    timestamp = timestamp,
    testedFrequencyCount = testedFrequencyCount,
    averageShiftDb = averageShiftDb,
    maxShiftDb = maxShiftDb,
    status = status,
    leftEarShifts = leftEarShifts,
    rightEarShifts = rightEarShifts,
)
