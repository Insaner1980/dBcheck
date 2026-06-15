package com.dbcheck.app.data.export

import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
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
                locale = Locale.US,
            )

        assertTrue(csv.startsWith("session_id,start_time,end_time,session_name,session_emoji,session_tags"))
        assertTrue(csv.contains("\"Workshop, south\",🎧,\"Work,Music\""))
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
                locale = Locale.US,
            )

        assertTrue(
            csv.startsWith(
                "session_id,session_name,session_emoji,session_tags,timestamp,raw_db,weighted_db,peak_db",
            ),
        )
        assertTrue(csv.contains("7,Workshop,🎧,\"Work,Music\",2023-11-14 22:13:21,70.0,68.5,101.2"))
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
                locale = Locale.US,
            )

        assertTrue(
            csv.startsWith(
                "session_id,session_name,session_emoji,session_tags,timestamp,label,confidence",
            ),
        )
        assertTrue(csv.contains("7,Workshop,,Work,2023-11-14 22:13:22,\"	=Speech, music\",0.82"))
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
                locale = Locale.US,
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
                    locale = Locale.US,
                )
            }.toString()

        assertEquals(
            "session_id,session_name,session_emoji,session_tags,timestamp,raw_db,weighted_db,peak_db\n" +
                "7,Workshop,,Work,2023-11-14 22:13:21,70.0,68.5,101.2\n",
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
                    locale = Locale.US,
                )
            }.toString()

        assertEquals(
            "session_id,session_name,session_emoji,session_tags,timestamp,label,confidence\n" +
                "7,Workshop,,,2023-11-14 22:13:22,Speech,0.82\n",
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

        assertTrue(output.contains("2023-11-14 22:13:20"))
        assertTrue(output.contains("2023-11-14 22:13:21"))
        assertTrue(output.contains("2023-11-14 22:13:22"))
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

            assertTrue(sessionsCsv.contains("2023-11-14 22:13:20"))
            assertTrue(measurementsCsv.contains("2023-11-14 22:13:21"))
            assertFalse(sessionsCsv.contains("2566"))
            assertFalse(measurementsCsv.contains("2566"))
        }
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
