package com.dbcheck.app.domain.audio

internal class ResponseTimedDecibelReadingProcessor(responseTime: ResponseTime = ResponseTime.FAST) {
    private var currentResponseTime = responseTime
    private var instantSmoother = ResponseTimeSmoother(responseTime)
    private var weightedSmoother = ResponseTimeSmoother(responseTime)
    private var aWeightedSmoother = ResponseTimeSmoother(responseTime)

    fun setResponseTime(responseTime: ResponseTime) {
        if (currentResponseTime == responseTime) return

        currentResponseTime = responseTime
        reset()
    }

    fun reset() {
        instantSmoother = ResponseTimeSmoother(currentResponseTime)
        weightedSmoother = ResponseTimeSmoother(currentResponseTime)
        aWeightedSmoother = ResponseTimeSmoother(currentResponseTime)
    }

    fun process(reading: DecibelReading): DecibelReading = reading.copy(
        instantDb =
            instantSmoother
                .smooth(ResponseTimeSample(db = reading.instantDb, timestampMs = reading.timestamp))
                .db,
        weightedDb =
            weightedSmoother
                .smooth(ResponseTimeSample(db = reading.weightedDb, timestampMs = reading.timestamp))
                .db,
        aWeightedDb =
            aWeightedSmoother
                .smooth(ResponseTimeSample(db = reading.aWeightedDb, timestampMs = reading.timestamp))
                .db,
    )
}
