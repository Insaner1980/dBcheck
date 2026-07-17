package com.dbcheck.app.data.export

import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.db.entity.SleepSessionEntity
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity
import com.dbcheck.app.projectFile
import com.dbcheck.app.withDefaultLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CsvExportFormatterTest {
    @Test
    fun sessionCsvEscapesMetadataAndIncludesTags() {
        val csv =
            CsvExportFormatter.buildSessionsCsv(
                sessions =
                    listOf(
                        SessionEntity(
                            id = 7L,
                            startTime = START_TIME,
                            endTime = START_TIME + 60_000L,
                            minDb = 61.2f,
                            avgDb = 72.3f,
                            maxDb = 84.4f,
                            peakDb = 101.5f,
                            name = "Workshop, south",
                            emoji = "🎧",
                            tags = "Work,Music",
                            frequencyWeighting = "A",
                        ),
                    ),
            )

        assertTrue(csv.startsWith("session_id,start_time_utc,end_time_utc,session_name,session_emoji,session_tags"))
        assertTrue(csv.contains("\"Workshop, south\",🎧,\"Work,Music\""))
    }

    @Test
    fun sessionCsvIncludesSleepMetadataWithFallbacks() {
        val sleepSession =
            SessionEntity(
                id = 7L,
                startTime = START_TIME,
                endTime = START_TIME + 60_000L,
                name = "Sleep",
                minDb = 45f,
                avgDb = 52f,
                maxDb = 77f,
                peakDb = 92f,
                frequencyWeighting = "A",
            )
        val regularSession =
            SessionEntity(
                id = 8L,
                startTime = START_TIME + 120_000L,
                endTime = START_TIME + 180_000L,
                name = "Workshop",
                frequencyWeighting = "Z",
            )

        val csv =
            CsvExportFormatter.buildSessionsCsv(
                sessions = listOf(sleepSession, regularSession),
                sleepSessionsBySessionId =
                    mapOf(
                        7L to
                            SleepSessionEntity(
                                sessionId = 7L,
                                targetDurationMinutes = 480,
                                keepAwakeEnabled = true,
                                createdAt = START_TIME - 1_000L,
                            ),
                    ),
            )

        assertTrue(
            csv.startsWith(
                "session_id,start_time_utc,end_time_utc,session_name,session_emoji,session_tags," +
                    "min_db,avg_db,max_db,peak_db,frequency_weighting,is_sleep_session," +
                    "sleep_target_minutes,sleep_keep_awake,sleep_created_at_utc",
            ),
        )
        assertTrue(
            csv.contains(
                "7,2023-11-14T22:13:20Z,2023-11-14T22:14:20Z,Sleep,,," +
                    "45.0,52.0,77.0,92.0,A,true,480,true,2023-11-14T22:13:19Z",
            ),
        )
        assertTrue(
            csv.contains(
                "8,2023-11-14T22:15:20Z,2023-11-14T22:16:20Z,Workshop,,,0.0,0.0,0.0,0.0,Z,false,,,",
            ),
        )
    }

    @Test
    fun measurementCsvIncludesSessionMetadataOnEveryMeasurementRow() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = START_TIME,
                endTime = START_TIME + 60_000L,
                name = "Workshop",
                emoji = "🎧",
                tags = "Work,Music",
                frequencyWeighting = "A",
            )
        val csv =
            CsvExportFormatter.buildMeasurementsCsv(
                sessions = listOf(session),
                measurementsBySessionId =
                    mapOf(
                        7L to
                            listOf(
                                MeasurementEntity(
                                    sessionId = 7L,
                                    timestamp = START_TIME + 1_000L,
                                    dbValue = 70f,
                                    dbWeighted = 68.5f,
                                    peakDb = 101.2f,
                                ),
                            ),
                    ),
            )

        assertTrue(
            csv.startsWith(
                "session_id,session_name,session_emoji,session_tags,timestamp_utc,raw_db,weighted_db,peak_db",
            ),
        )
        assertTrue(csv.contains("7,Workshop,🎧,\"Work,Music\",2023-11-14T22:13:21Z,70.0,68.5,101.2"))
    }

    @Test
    fun soundDetectionCsvExportsOnlyAggregatedDetectionEvents() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = START_TIME,
                endTime = START_TIME + 60_000L,
                name = "Workshop",
                tags = "Work",
                frequencyWeighting = "A",
            )
        val csv =
            CsvExportFormatter.buildSoundDetectionsCsv(
                sessions = listOf(session),
                detectionsBySessionId =
                    mapOf(
                        7L to
                            listOf(
                                SoundDetectionEventEntity(
                                    sessionId = 7L,
                                    timestamp = START_TIME + 2_000L,
                                    label = "=Speech, music",
                                    confidence = 0.82f,
                                ),
                            ),
                    ),
            )

        assertTrue(
            csv.startsWith(
                "session_id,session_name,session_emoji,session_tags,timestamp_utc,label,confidence",
            ),
        )
        assertTrue(csv.contains("7,Workshop,,Work,2023-11-14T22:13:22Z,\"	=Speech, music\",0.82"))
        assertFalse(csv.contains("raw_audio"))
    }

    @Test
    fun csvEscaperQuotesFieldsWithQuotesCommasAndLineBreaks() {
        assertEquals("\"a\"\"b\"", CsvEscaper.escape("a\"b"))
        assertEquals("\"a,b\"", CsvEscaper.escape("a,b"))
        assertEquals("\"a\nb\"", CsvEscaper.escape("a\nb"))
        assertEquals("plain", CsvEscaper.escape("plain"))
    }

    @Test
    fun sessionCsvNeutralizesSpreadsheetFormulaMetadata() {
        val csv =
            CsvExportFormatter.buildSessionsCsv(
                sessions =
                    listOf(
                        SessionEntity(
                            id = 7L,
                            startTime = START_TIME,
                            endTime = START_TIME + 60_000L,
                            name = "=HYPERLINK(\"https://example.com\")",
                            emoji = "@SUM(1,1)",
                            tags = "+SUM",
                            frequencyWeighting = "A",
                        ),
                    ),
            )

        assertTrue(csv.contains("\"	=HYPERLINK(\"\"https://example.com\"\")\""))
        assertTrue(csv.contains("\"	@SUM(1,1)\""))
        assertTrue(csv.contains("\"	+SUM\""))
    }

    @Test
    fun appendMeasurementCsvRowsWritesRowsToExistingAppendable() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = START_TIME,
                endTime = START_TIME + 60_000L,
                name = "Workshop",
                tags = "Work",
                frequencyWeighting = "A",
            )
        val output =
            StringBuilder().apply {
                CsvExportFormatter.appendMeasurementsCsvHeader(this)
                CsvExportFormatter.appendMeasurementCsvRows(
                    session = session,
                    measurements =
                        listOf(
                            MeasurementEntity(
                                sessionId = 7L,
                                timestamp = START_TIME + 1_000L,
                                dbValue = 70f,
                                dbWeighted = 68.5f,
                                peakDb = 101.2f,
                            ),
                        ),
                    appendable = this,
                )
            }.toString()

        assertEquals(
            "session_id,session_name,session_emoji,session_tags,timestamp_utc,raw_db,weighted_db,peak_db\n" +
                "7,Workshop,,Work,2023-11-14T22:13:21Z,70.0,68.5,101.2\n",
            output,
        )
    }

    @Test
    fun appendSoundDetectionCsvRowsWritesRowsToExistingAppendable() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = START_TIME,
                endTime = START_TIME + 60_000L,
                name = "Workshop",
                frequencyWeighting = "A",
            )
        val output =
            StringBuilder().apply {
                CsvExportFormatter.appendSoundDetectionCsvHeader(this)
                CsvExportFormatter.appendSoundDetectionCsvRows(
                    session = session,
                    detections =
                        listOf(
                            SoundDetectionEventEntity(
                                sessionId = 7L,
                                timestamp = START_TIME + 2_000L,
                                label = "Speech",
                                confidence = 0.82f,
                            ),
                        ),
                    appendable = this,
                )
            }.toString()

        assertEquals(
            "session_id,session_name,session_emoji,session_tags,timestamp_utc,label,confidence\n" +
                "7,Workshop,,,2023-11-14T22:13:22Z,Speech,0.82\n",
            output,
        )
    }

    @Test
    fun appendCsvHelpersUseDefaultLocaleWhenLocaleIsOmitted() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = START_TIME,
                endTime = START_TIME + 60_000L,
                name = "Workshop",
                frequencyWeighting = "A",
            )
        val output =
            StringBuilder().apply {
                CsvExportFormatter.appendSessionsCsv(listOf(session), this)
                CsvExportFormatter.appendMeasurementCsvRows(
                    session = session,
                    measurements =
                        listOf(
                            MeasurementEntity(
                                sessionId = 7L,
                                timestamp = START_TIME + 1_000L,
                                dbValue = 70f,
                                dbWeighted = 68.5f,
                                peakDb = 101.2f,
                            ),
                        ),
                    appendable = this,
                )
                CsvExportFormatter.appendSoundDetectionCsvRows(
                    session = session,
                    detections =
                        listOf(
                            SoundDetectionEventEntity(
                                sessionId = 7L,
                                timestamp = START_TIME + 2_000L,
                                label = "Speech",
                                confidence = 0.82f,
                            ),
                        ),
                    appendable = this,
                )
            }.toString()

        assertTrue(output.contains("2023-11-14T22:13:20Z"))
        assertTrue(output.contains("2023-11-14T22:13:21Z"))
        assertTrue(output.contains("2023-11-14T22:13:22Z"))
    }

    @Test
    fun csvDefaultsUseAsciiGregorianExportFormatAcrossDeviceLocales() {
        withDefaultLocale(Locale.forLanguageTag("th-TH")) {
            val session =
                SessionEntity(
                    id = 7L,
                    startTime = START_TIME,
                    endTime = START_TIME + 60_000L,
                    name = "Workshop",
                    frequencyWeighting = "A",
                )
            val measurement =
                MeasurementEntity(
                    sessionId = 7L,
                    timestamp = START_TIME + 1_000L,
                    dbValue = 70f,
                    dbWeighted = 68.5f,
                    peakDb = 101.2f,
                )

            val sessionsCsv = CsvExportFormatter.buildSessionsCsv(listOf(session))
            val measurementsCsv =
                CsvExportFormatter.buildMeasurementsCsv(
                    sessions = listOf(session),
                    measurementsBySessionId = mapOf(7L to listOf(measurement)),
                )

            assertTrue(sessionsCsv.contains("2023-11-14T22:13:20Z"))
            assertTrue(measurementsCsv.contains("2023-11-14T22:13:21Z"))
            assertFalse(sessionsCsv.contains("2566"))
            assertFalse(measurementsCsv.contains("2566"))
        }
    }

    @Test
    fun csvTimestampsAreSelfDescribingUtcInstants() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = START_TIME,
                endTime = START_TIME + 60_000L,
                frequencyWeighting = "A",
            )

        val csv = CsvExportFormatter.buildSessionsCsv(listOf(session))

        assertTrue(csv.contains("2023-11-14T22:13:20Z"))
        assertTrue(csv.contains("2023-11-14T22:14:20Z"))
    }

    @Test
    fun exportUseCaseFilenameTimestampUsesInvariantLocale() {
        val source = projectFile("src/main/java/com/dbcheck/app/data/export/ExportCsvUseCase.kt").readText()

        assertTrue(source.contains("SimpleDateFormat(CSV_EXPORT_TIMESTAMP_PATTERN, Locale.US)"))
        assertFalse(source.contains("CSV_EXPORT_TIMESTAMP_PATTERN, Locale.getDefault()"))
    }

    private companion object {
        const val START_TIME = 1_700_000_000_000L
    }
}
