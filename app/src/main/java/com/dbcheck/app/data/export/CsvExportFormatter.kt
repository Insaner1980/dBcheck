package com.dbcheck.app.data.export

import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvEscaper {
    fun escape(value: String?, neutralizeSpreadsheetFormula: Boolean = false): String {
        val text = value.orEmpty()
        val csvText =
            if (neutralizeSpreadsheetFormula && text.startsWithSpreadsheetFormula()) {
                "$FORMULA_NEUTRALIZER$text"
            } else {
                text
            }
        val escaped = csvText.replace("\"", "\"\"")
        return if (csvText.requiresQuotes() || csvText != text) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun String.requiresQuotes(): Boolean = any { it in SPECIAL_CHARACTERS }

    private fun String.startsWithSpreadsheetFormula(): Boolean = firstOrNull() in FORMULA_PREFIXES

    private val SPECIAL_CHARACTERS = setOf(',', '"', '\n', '\r')
    private val FORMULA_PREFIXES = setOf('=', '+', '-', '@')
    private const val FORMULA_NEUTRALIZER = '\t'
}

object CsvExportFormatter {
    fun buildSessionsCsv(sessions: List<SessionEntity>, locale: Locale = Locale.US): String = buildString {
            appendSessionsCsv(
                sessions = sessions,
                appendable = this,
                locale = locale,
            )
        }

    fun appendSessionsCsv(sessions: List<SessionEntity>, appendable: Appendable, locale: Locale = Locale.US) {
        val dateFormat = csvDateFormat(locale)
        appendable.appendLine(
            "session_id,start_time,end_time,session_name,session_emoji,session_tags," +
                "min_db,avg_db,max_db,peak_db,frequency_weighting",
        )
        sessions.forEach { session ->
            appendable.appendLine(sessionCsvRow(session, dateFormat))
        }
    }

    fun buildMeasurementsCsv(
        sessions: List<SessionEntity>,
        measurementsBySessionId: Map<Long, List<MeasurementEntity>>,
        locale: Locale = Locale.US,
    ): String = buildString {
        appendMeasurementsCsvHeader(this)
        sessions.forEach { session ->
            appendMeasurementCsvRows(
                session = session,
                measurements = measurementsBySessionId[session.id].orEmpty(),
                appendable = this,
                locale = locale,
            )
        }
    }

    fun buildSoundDetectionsCsv(
        sessions: List<SessionEntity>,
        detectionsBySessionId: Map<Long, List<SoundDetectionEventEntity>>,
        locale: Locale = Locale.US,
    ): String = buildString {
        appendSoundDetectionCsvHeader(this)
        sessions.forEach { session ->
            appendSoundDetectionCsvRows(
                session = session,
                detections = detectionsBySessionId[session.id].orEmpty(),
                appendable = this,
                locale = locale,
            )
        }
    }

    fun appendMeasurementsCsvHeader(appendable: Appendable) {
        appendable.appendLine("session_id,session_name,session_emoji,session_tags,timestamp,raw_db,weighted_db,peak_db")
    }

    fun appendMeasurementCsvRows(
        session: SessionEntity,
        measurements: List<MeasurementEntity>,
        appendable: Appendable,
        locale: Locale = Locale.US,
    ) {
        val dateFormat = csvDateFormat(locale)
        measurements.forEach { measurement ->
            appendable.appendLine(measurementCsvRow(session, measurement, dateFormat))
        }
    }

    fun appendSoundDetectionCsvHeader(appendable: Appendable) {
        appendable.appendLine("session_id,session_name,session_emoji,session_tags,timestamp,label,confidence")
    }

    fun appendSoundDetectionCsvRows(
        session: SessionEntity,
        detections: List<SoundDetectionEventEntity>,
        appendable: Appendable,
        locale: Locale = Locale.US,
    ) {
        val dateFormat = csvDateFormat(locale)
        detections.forEach { detection ->
            appendable.appendLine(soundDetectionCsvRow(session, detection, dateFormat))
        }
    }

    private fun sessionCsvRow(session: SessionEntity, dateFormat: SimpleDateFormat): String = listOf(
        CsvEscaper.escape(session.id.toString()),
        CsvEscaper.escape(dateFormat.format(Date(session.startTime))),
        CsvEscaper.escape(session.endTime?.let { dateFormat.format(Date(it)) }.orEmpty()),
    ).plus(sessionMetadataColumns(session))
        .plus(
            listOf(
                CsvEscaper.escape(session.minDb.toString()),
                CsvEscaper.escape(session.avgDb.toString()),
                CsvEscaper.escape(session.maxDb.toString()),
                CsvEscaper.escape(session.peakDb.toString()),
                CsvEscaper.escape(session.frequencyWeighting),
            ),
        ).joinToString(separator = ",")

    private fun measurementCsvRow(
        session: SessionEntity,
        measurement: MeasurementEntity,
        dateFormat: SimpleDateFormat,
    ): String = listOf(
        CsvEscaper.escape(session.id.toString()),
    ).plus(sessionMetadataColumns(session))
        .plus(
            listOf(
                CsvEscaper.escape(dateFormat.format(Date(measurement.timestamp))),
                CsvEscaper.escape(measurement.dbValue.toString()),
                CsvEscaper.escape(measurement.dbWeighted.toString()),
                CsvEscaper.escape(measurement.peakDb.toString()),
            ),
        ).joinToString(separator = ",")

    private fun soundDetectionCsvRow(
        session: SessionEntity,
        detection: SoundDetectionEventEntity,
        dateFormat: SimpleDateFormat,
    ): String = listOf(
        CsvEscaper.escape(session.id.toString()),
    ).plus(sessionMetadataColumns(session))
        .plus(
            listOf(
                CsvEscaper.escape(dateFormat.format(Date(detection.timestamp))),
                CsvEscaper.escape(detection.label, neutralizeSpreadsheetFormula = true),
                CsvEscaper.escape(detection.confidence.toString()),
            ),
        ).joinToString(separator = ",")
}

private fun sessionMetadataColumns(session: SessionEntity): List<String> = listOf(
    CsvEscaper.escape(session.name.orEmpty(), neutralizeSpreadsheetFormula = true),
    CsvEscaper.escape(session.emoji.orEmpty(), neutralizeSpreadsheetFormula = true),
    CsvEscaper.escape(session.tags.orEmpty(), neutralizeSpreadsheetFormula = true),
)

private fun csvDateFormat(locale: Locale): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
