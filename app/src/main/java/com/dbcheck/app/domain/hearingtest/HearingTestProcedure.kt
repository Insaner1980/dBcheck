package com.dbcheck.app.domain.hearingtest

data class HearingTestProgress(
    val currentPhase: Int,
    val totalPhases: Int,
    val currentEar: Ear,
    val currentFrequency: Float,
    val amplitudeDb: Float,
)

sealed interface HearingTestStepResult {
    data class Continue(val progress: HearingTestProgress) : HearingTestStepResult

    data class Completed(val thresholds: Map<TestKey, Float>) : HearingTestStepResult
}

class HearingTestProcedure(
    frequencies: List<Float> = TEST_FREQUENCIES,
    ears: List<Ear> = listOf(Ear.LEFT, Ear.RIGHT),
) {
    private val testSequence: List<Pair<Ear, Float>> =
        buildList {
            ears.forEach { ear ->
                frequencies.forEach { frequency -> add(ear to frequency) }
            }
        }
    private val thresholds = linkedMapOf<TestKey, Float>()
    private var currentIndex = 0
    private var currentAmplitudeDb = STARTING_AMPLITUDE_DB
    private var ascendingHeardCount = 0
    private var hasAscendedSinceLastHeard = false

    fun start(): HearingTestProgress {
        currentIndex = 0
        thresholds.clear()
        resetForNewFrequency()
        return currentProgress()
    }

    fun onHeard(): HearingTestStepResult {
        if (hasAscendedSinceLastHeard) {
            ascendingHeardCount++
            hasAscendedSinceLastHeard = false
        }

        return if (ascendingHeardCount >= REQUIRED_ASCENDING_HEARD_COUNT) {
            recordThreshold(currentAmplitudeDb)
            advance()
        } else {
            currentAmplitudeDb -= HEARD_DESCEND_DB
            if (currentAmplitudeDb < FLOOR_AMPLITUDE_DB) {
                recordThreshold(FLOOR_AMPLITUDE_DB)
                advance()
            } else {
                HearingTestStepResult.Continue(currentProgress())
            }
        }
    }

    fun onNotHeard(): HearingTestStepResult {
        currentAmplitudeDb += NOT_HEARD_ASCEND_DB
        hasAscendedSinceLastHeard = true
        return if (currentAmplitudeDb > MAX_AMPLITUDE_DB) {
            recordThreshold(MAX_AMPLITUDE_DB)
            advance()
        } else {
            HearingTestStepResult.Continue(currentProgress())
        }
    }

    private fun recordThreshold(thresholdDb: Float) {
        val (ear, frequency) = testSequence[currentIndex]
        thresholds[TestKey(ear, frequency)] = thresholdDb
    }

    private fun advance(): HearingTestStepResult {
        currentIndex++
        return if (currentIndex >= testSequence.size) {
            HearingTestStepResult.Completed(thresholds.toMap())
        } else {
            resetForNewFrequency()
            HearingTestStepResult.Continue(currentProgress())
        }
    }

    private fun resetForNewFrequency() {
        currentAmplitudeDb = STARTING_AMPLITUDE_DB
        ascendingHeardCount = 0
        hasAscendedSinceLastHeard = false
    }

    private fun currentProgress(): HearingTestProgress {
        val (ear, frequency) = testSequence[currentIndex]
        return HearingTestProgress(
            currentPhase = currentIndex + 1,
            totalPhases = testSequence.size,
            currentEar = ear,
            currentFrequency = frequency,
            amplitudeDb = currentAmplitudeDb,
        )
    }

    private companion object {
        const val STARTING_AMPLITUDE_DB = -30f
        const val HEARD_DESCEND_DB = 10f
        const val NOT_HEARD_ASCEND_DB = 5f
        const val FLOOR_AMPLITUDE_DB = -60f
        const val MAX_AMPLITUDE_DB = 0f
        const val REQUIRED_ASCENDING_HEARD_COUNT = 2
    }
}
