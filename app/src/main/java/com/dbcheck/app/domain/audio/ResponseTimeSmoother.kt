package com.dbcheck.app.domain.audio

import com.dbcheck.app.domain.noise.DecibelMath
import kotlin.math.exp

data class ResponseTimeSample(val db: Float, val timestampMs: Long)

class ResponseTimeSmoother(private val responseTime: ResponseTime) {
    private var previousTimestampMs: Long? = null
    private var smoothedEnergy: Double? = null

    fun smooth(samples: List<ResponseTimeSample>): List<ResponseTimeSample> = samples.map(::smooth)

    fun smooth(sample: ResponseTimeSample): ResponseTimeSample {
        val previousTimestamp = previousTimestampMs
        val previousEnergy = smoothedEnergy
        val currentEnergy = DecibelMath.energyFromDb(sample.db)
        val nextEnergy =
            if (previousTimestamp == null || previousEnergy == null) {
                currentEnergy
            } else {
                require(sample.timestampMs >= previousTimestamp) {
                    "Response time samples must be ordered by timestamp."
                }
                val elapsedMs = (sample.timestampMs - previousTimestamp).toDouble()
                val alpha = 1.0 - exp(-elapsedMs / responseTime.timeConstantMs.toDouble())
                previousEnergy + alpha * (currentEnergy - previousEnergy)
            }

        previousTimestampMs = sample.timestampMs
        smoothedEnergy = nextEnergy
        return sample.copy(db = DecibelMath.energyAverageDb(nextEnergy, weight = 1.0) ?: sample.db)
    }

    fun reset() {
        previousTimestampMs = null
        smoothedEnergy = null
    }
}
