package com.dbcheck.app.data.export

import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.db.entity.SleepSessionEntity
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity
import java.time.Instant
import java.time.format.DateTimeFormatter

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
    fun buildSessionsCsv(
        sessions: List<SessionEntity>,
        sleepSessionsBySessionId: Map<Long, SleepSessionEntity> = emptyMap(),
    ): String = buildString {
            appendSessionsCsv(
                sessions = sessions,
                appendable = this,
                sleepSessionsBySessionId = sleepSessionsBySessionId,
            )
        }

    fun appendSessionsCsv(
        sessions: List<SessionEntity>,
        appendable: Appendable,
        sleepSessionsBySessionId: Map<Long, SleepSessionEntity> = emptyMap(),
    ) {
        appendable.appendLine(
            "session_id,start_time_utc,end_time_utc,session_name,session_emoji,session_tags," +
                "min_db,avg_db,max_db,peak_db,frequency_weighting,is_sleep_session," +
                "sleep_target_minutes,sleep_keep_awake,sleep_created_at_utc",
        )
        sessions.forEach { session ->
            appendable.appendLine(
                sessionCsvRow(
                    session = session,
                    sleepSession = sleepSessionsBySessionId[session.id],
                ),
            )
        }
    }

    fun buildMeasurementsCsv(
        sessions: List<SessionEntity>,
        measurementsBySessionId: Map<Long, List<MeasurementEntity>>,
    ): String = buildString {
        appendMeasurementsCsvHeader(this)
        sessions.forEach { session ->
            appendMeasurementCsvRows(
                session = session,
                measurements = measurementsBySessionId[session.id].orEmpty(),
                appendable = this,
            )
        }
    }

    fun buildSoundDetectionsCsv(
        sessions: List<SessionEntity>,
        detectionsBySessionId: Map<Long, List<SoundDetectionEventEntity>>,
    ): String = buildString {
        appendSoundDetectionCsvHeader(this)
        sessions.forEach { session ->
            appendSoundDetectionCsvRows(
                session = session,
                detections = detectionsBySessionId[session.id].orEmpty(),
                appendable = this,
            )
        }
    }

    fun appendMeasurementsCsvHeader(appendable: Appendable) {
        appendable.appendLine(
            "session_id,session_name,session_emoji,session_tags,timestamp_utc,raw_db,weighted_db,peak_db",
        )
    }

    fun appendMeasurementCsvRows(
        session: SessionEntity,
        measurements: List<MeasurementEntity>,
        appendable: Appendable,
    ) {
        measurements.forEach { measurement ->
            appendable.appendLine(measurementCsvRow(session, measurement))
        }
    }

    fun appendSoundDetectionCsvHeader(appendable: Appendable) {
        appendable.appendLine("session_id,session_name,session_emoji,session_tags,timestamp_utc,label,confidence")
    }

    fun appendSoundDetectionCsvRows(
        session: SessionEntity,
        detections: List<SoundDetectionEventEntity>,
        appendable: Appendable,
    ) {
        detections.forEach { detection ->
            appendable.appendLine(soundDetectionCsvRow(session, detection))
        }
    }

    private fun sessionCsvRow(session: SessionEntity, sleepSession: SleepSessionEntity?): String = listOf(
        CsvEscaper.escape(session.id.toString()),
        CsvEscaper.escape(csvTimestamp(session.startTime)),
        CsvEscaper.escape(session.endTime?.let(::csvTimestamp).orEmpty()),
    ).plus(sessionMetadataColumns(session))
        .plus(
            listOf(
                CsvEscaper.escape(session.minDb.toString()),
                CsvEscaper.escape(session.avgDb.toString()),
                CsvEscaper.escape(session.maxDb.toString()),
                CsvEscaper.escape(session.peakDb.toString()),
                CsvEscaper.escape(session.frequencyWeighting),
            ),
        ).plus(sleepSessionColumns(sleepSession))
        .joinToString(separator = ",")

    private fun measurementCsvRow(session: SessionEntity, measurement: MeasurementEntity): String = listOf(
        CsvEscaper.escape(session.id.toString()),
    ).plus(sessionMetadataColumns(session))
        .plus(
            listOf(
                CsvEscaper.escape(csvTimestamp(measurement.timestamp)),
                CsvEscaper.escape(measurement.dbValue.toString()),
                CsvEscaper.escape(measurement.dbWeighted.toString()),
                CsvEscaper.escape(measurement.peakDb.toString()),
            ),
        ).joinToString(separator = ",")

    private fun soundDetectionCsvRow(session: SessionEntity, detection: SoundDetectionEventEntity): String = listOf(
        CsvEscaper.escape(session.id.toString()),
    ).plus(sessionMetadataColumns(session))
        .plus(
            listOf(
                CsvEscaper.escape(csvTimestamp(detection.timestamp)),
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

private fun sleepSessionColumns(sleepSession: SleepSessionEntity?): List<String> = if (sleepSession == null) {
        listOf(
            CsvEscaper.escape(false.toString()),
            CsvEscaper.escape(""),
            CsvEscaper.escape(""),
            CsvEscaper.escape(""),
        )
    } else {
        listOf(
            CsvEscaper.escape(true.toString()),
            CsvEscaper.escape(sleepSession.targetDurationMinutes.toString()),
            CsvEscaper.escape(sleepSession.keepAwakeEnabled.toString()),
            CsvEscaper.escape(csvTimestamp(sleepSession.createdAt)),
        )
    }

private fun csvTimestamp(timestampMs: Long): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestampMs))
