package com.dbcheck.app.domain.hearingtest

data class HearingRecoveryResult(
    val id: Long = 0,
    val baselineTestId: Long,
    val timestamp: Long,
    val testedFrequencyCount: Int,
    val averageShiftDb: Float,
    val maxShiftDb: Float,
    val status: HearingRecoveryStatus,
    val leftEarShifts: List<Pair<Float, Float>>,
    val rightEarShifts: List<Pair<Float, Float>>,
)

enum class HearingRecoveryStatus {
    STABLE,
    SMALL_SHIFT,
    ELEVATED_SHIFT,
}

object HearingRecoveryCalculator {
    fun build(baseline: HearingTestResult, thresholds: Map<TestKey, Float>, timestamp: Long): HearingRecoveryResult {
        val leftShifts = thresholdShifts(baseline.leftEarThresholds, thresholds, Ear.LEFT)
        val rightShifts = thresholdShifts(baseline.rightEarThresholds, thresholds, Ear.RIGHT)
        val allShifts = leftShifts.map { it.second } + rightShifts.map { it.second }
        val averageShift = if (allShifts.isEmpty()) 0f else allShifts.average().toFloat()
        val maxShift = allShifts.maxOrNull() ?: 0f

        return HearingRecoveryResult(
            baselineTestId = baseline.id,
            timestamp = timestamp,
            testedFrequencyCount = allShifts.size,
            averageShiftDb = averageShift,
            maxShiftDb = maxShift,
            status = recoveryStatus(maxShift),
            leftEarShifts = leftShifts,
            rightEarShifts = rightShifts,
        )
    }

    private fun thresholdShifts(
        baselineThresholds: List<Pair<Float, Float>>,
        thresholds: Map<TestKey, Float>,
        ear: Ear,
    ): List<Pair<Float, Float>> {
        val baselineByFrequency = baselineThresholds.toMap()
        return HearingTestPolicy.RECOVERY_CHECK_FREQUENCIES.mapNotNull { frequency ->
            val baselineThreshold = baselineByFrequency[frequency] ?: return@mapNotNull null
            val checkThreshold = thresholds[TestKey(ear, frequency)] ?: return@mapNotNull null
            frequency to checkThreshold - baselineThreshold
        }
    }

    private fun recoveryStatus(maxShiftDb: Float): HearingRecoveryStatus = when {
        maxShiftDb >= ELEVATED_SHIFT_DB -> HearingRecoveryStatus.ELEVATED_SHIFT
        maxShiftDb >= SMALL_SHIFT_DB -> HearingRecoveryStatus.SMALL_SHIFT
        else -> HearingRecoveryStatus.STABLE
    }

    private const val SMALL_SHIFT_DB = 10f
    private const val ELEVATED_SHIFT_DB = 20f
}
