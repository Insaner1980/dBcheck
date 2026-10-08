package com.dbcheck.app.ui.meter.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChartBufferTest {
    @Test
    fun trimsPointsByTimestampWindowNotListSize() {
        val buffer = LiveChartBuffer(windowMs = 30_000L)

        repeat(401) { index ->
            buffer.add(timestampMs = index * 100L, db = index.toFloat())
        }

        val points = buffer.snapshot()
        assertEquals(301, points.size)
        assertEquals(10_000L, points.first().timestampMs)
        assertEquals(40_000L, points.last().timestampMs)
    }

    @Test
    fun publishedPointsStayUnchangedWhenBufferIsUpdatedOrCleared() {
        val buffer = LiveChartBuffer(windowMs = 1_000L)
        val first = buffer.add(timestampMs = 1_000L, db = 62f)

        buffer.add(timestampMs = 3_000L, db = 80f)
        buffer.clear()

        assertEquals(listOf(LiveChartPointUiState(timestampMs = 1_000L, db = 62f)), first)
    }

    @Test
    fun clearRemovesBufferedPoints() {
        val buffer = LiveChartBuffer(windowMs = 30_000L)
        buffer.add(timestampMs = 1_000L, db = 62f)

        buffer.clear()

        assertTrue(buffer.snapshot().isEmpty())
    }
}
