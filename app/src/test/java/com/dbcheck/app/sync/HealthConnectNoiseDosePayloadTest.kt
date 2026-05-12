package com.dbcheck.app.sync

import com.dbcheck.app.domain.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HealthConnectNoiseDosePayloadTest {
    @Test
    fun fromSessionBuildsStableNoiseDosePayload() {
        val payload =
            HealthConnectNoiseDosePayload.fromSession(
                session =
                    session(
                        id = 7L,
                        startTime = START_TIME_MS,
                        endTime = END_TIME_MS,
                        avgDb = 61.4f,
                        maxDb = 96.4f,
                        peakDb = 103.8f,
                    ),
                laeqDb = 88.2f,
            )

        assertNotNull(payload)
        payload ?: return
        assertEquals("noise_dose_2023-11-14_session_7", payload.clientRecordId)
        assertEquals(Instant.ofEpochMilli(START_TIME_MS), payload.startTime)
        assertEquals(Instant.ofEpochMilli(END_TIME_MS), payload.endTime)
        assertEquals(22, payload.durationMinutes)
        assertEquals("dBcheck noise exposure", payload.title)
        assertTrue(payload.notes.contains("LAeq 88.2 dB"))
        assertTrue(payload.notes.contains("Max 96.4 dB"))
        assertTrue(payload.notes.contains("Peak 103.8 dB"))
    }

    @Test
    fun fromSessionSkipsActiveSessionWithoutEndTime() {
        val payload =
            HealthConnectNoiseDosePayload.fromSession(
                session = session(id = 8L, startTime = START_TIME_MS, endTime = null),
                laeqDb = 72f,
            )

        assertNull(payload)
    }

    private fun session(
        id: Long,
        startTime: Long,
        endTime: Long?,
        avgDb: Float = 72f,
        maxDb: Float = 78f,
        peakDb: Float = 84f,
    ) = Session(
        id = id,
        startTime = startTime,
        endTime = endTime,
        minDb = 55f,
        avgDb = avgDb,
        maxDb = maxDb,
        peakDb = peakDb,
        name = null,
        emoji = null,
        tags = emptyList(),
        isActive = endTime == null,
        frequencyWeighting = "A",
    )

    private companion object {
        const val START_TIME_MS = 1_700_000_000_000L
        const val END_TIME_MS = START_TIME_MS + 22 * 60 * 1_000L
    }
}
