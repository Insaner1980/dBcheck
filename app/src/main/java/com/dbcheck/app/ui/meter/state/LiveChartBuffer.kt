package com.dbcheck.app.ui.meter.state

internal const val METER_LIVE_CHART_WINDOW_MS = 30_000L

data class LiveChartPointUiState(val timestampMs: Long, val db: Float)

internal class LiveChartBuffer(private val windowMs: Long = METER_LIVE_CHART_WINDOW_MS) {
    private val points = ArrayDeque<LiveChartPointUiState>()

    init {
        require(windowMs > 0L) { "Live chart window must be positive" }
    }

    fun add(timestampMs: Long, db: Float): List<LiveChartPointUiState> {
        points.addLast(LiveChartPointUiState(timestampMs = timestampMs, db = db))
        trimToWindow(latestTimestampMs = timestampMs)
        return snapshot()
    }

    fun clear() {
        points.clear()
    }

    fun snapshot(): List<LiveChartPointUiState> = points.toList()

    private fun trimToWindow(latestTimestampMs: Long) {
        val cutoffTimestampMs = latestTimestampMs - windowMs
        while (points.firstOrNull()?.timestampMs?.let { it < cutoffTimestampMs } == true) {
            points.removeFirst()
        }
    }
}
