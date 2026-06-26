package com.dbcheck.app.sync

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.testSessionReportData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HealthConnectNoiseDosePayloadTest {
    @Test
    fun fromReportBuildsStableNoiseDosePayload() {
        val payload =
            HealthConnectNoiseDosePayload.fromReport(
                report =
                    report(
                        sessionId = 7L,
                        startTime = START_TIME_MS,
                        endTime = END_TIME_MS,
                        laeqDb = 88.2f,
                        maxDb = 96.4f,
                        lcPeakDb = 103.8f,
                    ),
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
        assertTrue(payload.notes.contains("LCpeak 103.8 dB"))
        assertFalse(payload.notes.contains("Peak 103.8 dB"))
    }

    @Test
    fun fromReportFormatsWeightingForHealthConnectNotes() {
        val payload =
            HealthConnectNoiseDosePayload.fromReport(
                report =
                    report(
                        sessionId = 7L,
                        startTime = START_TIME_MS,
                        endTime = END_TIME_MS,
                        laeqDb = 88.2f,
                        frequencyWeighting = WeightingType.ITUR468.name,
                        equivalentLevelLabel = "Leq (ITU-R 468)",
                    ),
                text = TEXT,
            )

        assertNotNull(payload)
        payload ?: return
        assertTrue(payload.notes.contains("Leq (ITU-R 468) 88.2 dB"))
        assertFalse(payload.notes.contains("LAeq"))
        assertTrue(payload.notes.contains("Weighting ITU-R 468"))
    }

    @Test
    fun fromReportFormatsEverySupportedWeightingForHealthConnectNotes() {
        val expectedLabels =
            mapOf(
                WeightingType.B.name to "B-Weight",
                WeightingType.C.name to "C-Weight",
                WeightingType.Z.name to "Z-Weight",
            )

        expectedLabels.forEach { (weighting, expectedLabel) ->
            val payload =
                requireNotNull(
                    HealthConnectNoiseDosePayload.fromReport(
                        report =
                            report(
                                sessionId = 7L,
                                startTime = START_TIME_MS,
                                endTime = END_TIME_MS,
                                frequencyWeighting = weighting,
                            ),
                        text = TEXT,
                    ),
                )

            assertTrue(payload.notes.contains("Weighting $expectedLabel"))
        }
    }

    @Test
    fun fromReportKeepsUnknownWeightingInHealthConnectNotes() {
        val payload =
            requireNotNull(
                HealthConnectNoiseDosePayload.fromReport(
                    report =
                        report(
                            sessionId = 7L,
                            startTime = START_TIME_MS,
                            endTime = END_TIME_MS,
                            frequencyWeighting = "Custom",
                        ),
                    text = TEXT,
                ),
            )

        assertTrue(payload.notes.contains("Weighting Custom"))
    }

    @Test
    fun toExerciseSessionRecordBuildsCurrentHealthConnectPayloadShape() {
        val payload =
            requireNotNull(
                HealthConnectNoiseDosePayload.fromReport(
                    report = report(sessionId = 7L, startTime = START_TIME_MS, endTime = END_TIME_MS, laeqDb = 88.2f),
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
    fun fromReportSkipsZeroDurationReport() {
        val payload =
            HealthConnectNoiseDosePayload.fromReport(
                report = report(sessionId = 8L, startTime = START_TIME_MS, endTime = START_TIME_MS),
                text = TEXT,
            )

        assertNull(payload)
    }

    private fun report(
        sessionId: Long,
        startTime: Long,
        endTime: Long,
        laeqDb: Float = 72f,
        maxDb: Float = 78f,
        lcPeakDb: Float = 84f,
        frequencyWeighting: String = WeightingType.A.name,
        equivalentLevelLabel: String = "LAeq",
    ) = testSessionReportData(
        sessionId = sessionId,
        startTime = startTime,
        endTime = endTime,
        generatedAtMs = endTime,
        durationMs = (endTime - startTime).coerceAtLeast(0L),
        weighting = frequencyWeighting,
        equivalentLevelLabel = equivalentLevelLabel,
        minDb = 55f,
        maxDb = maxDb,
        laeqDb = laeqDb,
        lcPeakDb = lcPeakDb,
        aWeightedExposureMetricsAvailable = frequencyWeighting == WeightingType.A.name,
    )

    private companion object {
        const val START_TIME_MS = 1_700_000_000_000L
        const val END_TIME_MS = START_TIME_MS + 22 * 60 * 1_000L
        val TEXT =
            HealthConnectNoiseDoseText(
                title = "dBcheck noise exposure",
                maxLabel = "Max",
                peakLabel = "LCpeak",
                weightingLabel = "Weighting",
                aWeightLabel = "A-Weight",
                bWeightLabel = "B-Weight",
                cWeightLabel = "C-Weight",
                zWeightLabel = "Z-Weight",
                ituR468Label = "ITU-R 468",
            )
    }
}
