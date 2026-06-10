package com.dbcheck.app.domain.audio

import com.dbcheck.app.domain.noise.DecibelMath
import kotlin.math.exp

internal fun expectedResponseTimeStepDb(
    startDb: Float,
    stepDb: Float,
    responseTime: ResponseTime,
    intervalMs: Double = RESPONSE_TIME_STEP_INTERVAL_MS,
): Float {
    val startEnergy = DecibelMath.energyFromDb(startDb)
    val stepEnergy = DecibelMath.energyFromDb(stepDb)
    val alpha = 1.0 - exp(-intervalMs / responseTime.timeConstantMs.toDouble())
    val smoothedEnergy = startEnergy + alpha * (stepEnergy - startEnergy)
    return DecibelMath.energyAverageDb(smoothedEnergy, weight = 1.0) ?: 0f
}

private const val RESPONSE_TIME_STEP_INTERVAL_MS = 100.0
