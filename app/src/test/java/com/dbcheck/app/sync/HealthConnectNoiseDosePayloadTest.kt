package com.dbcheck.app.sync

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.dbcheck.app.domain.audio.WeightingType
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
                text = TEXT,
            )

        assertNotNull(payload)
        payload ?: return
        assertEquals("noise_dose_2023-11-14_session_7", payload.clientRecordId)
        assertEquals(END_TIME_MS, payload.clientRecordVersion)
        assertEquals(Instant.ofEpochMilli(START_TIME_MS), payload.startTime)
        assertEquals(Instant.ofEpochMilli(END_TIME_MS), payload.endTime)
        assertEquals(22, payload.durationMinutes)
        assertEquals("dBcheck noise exposure", payload.title)
        assertTrue(payload.notes.contains("LAeq 88.2 dB"))
        assertTrue(payload.notes.contains("Max 96.4 dB"))
        assertTrue(payload.notes.contains("Peak 103.8 dB"))
    }

    @Test
    fun fromSessionFormatsWeightingForHealthConnectNotes() {
        val payload =
            HealthConnectNoiseDosePayload.fromSession(
                session =
                    session(
                        id = 7L,
                        startTime = START_TIME_MS,
                        endTime = END_TIME_MS,
                        frequencyWeighting = WeightingType.ITUR468.name,
                    ),
                laeqDb = 88.2f,
                text = TEXT,
            )

        assertNotNull(payload)
        payload ?: return
        assertTrue(payload.notes.contains("Weighting ITU-R 468"))
    }

    @Test
    fun toExerciseSessionRecordBuildsCurrentHealthConnectPayloadShape() {
        val payload =
            requireNotNull(
                HealthConnectNoiseDosePayload.fromSession(
                    session = session(id = 7L, startTime = START_TIME_MS, endTime = END_TIME_MS),
                    laeqDb = 88.2f,
                    text = TEXT,
                ),
            )

        val record =
            payload.toExerciseSessionRecord(
                device =
                    Device(
                        type = Device.TYPE_PHONE,
                        manufacturer = "dbcheck-test",
                        model = "unit",
                    ),
            )

        assertEquals(Instant.ofEpochMilli(START_TIME_MS), record.startTime)
        assertEquals(null, record.startZoneOffset)
        assertEquals(Instant.ofEpochMilli(END_TIME_MS), record.endTime)
        assertEquals(null, record.endZoneOffset)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, record.exerciseType)
        assertEquals("dBcheck noise exposure", record.title)
        assertEquals(payload.notes, record.notes)
        assertEquals("noise_dose_2023-11-14_session_7", record.metadata.clientRecordId)
        assertEquals(END_TIME_MS, record.metadata.clientRecordVersion)
        assertEquals(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED, record.metadata.recordingMethod)
    }

    @Test
    fun fromSessionSkipsActiveSessionWithoutEndTime() {
        val payload =
            HealthConnectNoiseDosePayload.fromSession(
                session = session(id = 8L, startTime = START_TIME_MS, endTime = null),
                laeqDb = 72f,
                text = TEXT,
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
        frequencyWeighting: String = WeightingType.A.name,
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
        frequencyWeighting = frequencyWeighting,
    )

    private companion object {
        const val START_TIME_MS = 1_700_000_000_000L
        const val END_TIME_MS = START_TIME_MS + 22 * 60 * 1_000L
        val TEXT =
            HealthConnectNoiseDoseText(
                title = "dBcheck noise exposure",
                laeqLabel = "LAeq",
                maxLabel = "Max",
                peakLabel = "Peak",
                weightingLabel = "Weighting",
                aWeightLabel = "A-Weight",
                bWeightLabel = "B-Weight",
                cWeightLabel = "C-Weight",
                zWeightLabel = "Z-Weight",
                ituR468Label = "ITU-R 468",
            )
    }
}
