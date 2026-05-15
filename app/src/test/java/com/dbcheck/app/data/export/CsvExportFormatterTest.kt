package com.dbcheck.app.data.export

import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import org.junit.Assert.assertEquals
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

    private companion object {
        const val START_TIME = 1_700_000_000_000L
    }
}
