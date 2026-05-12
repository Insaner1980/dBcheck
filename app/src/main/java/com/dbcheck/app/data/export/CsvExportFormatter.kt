package com.dbcheck.app.data.export

import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvEscaper {
    fun escape(value: String?): String {
        val text = value.orEmpty()
        val escaped = text.replace("\"", "\"\"")
        return if (text.requiresQuotes()) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun String.requiresQuotes(): Boolean = any { it in SPECIAL_CHARACTERS }

    private val SPECIAL_CHARACTERS = setOf(',', '"', '\n', '\r')
}

object CsvExportFormatter {
    fun buildSessionsCsv(
        sessions: List<SessionEntity>,
        locale: Locale = Locale.getDefault(),
    ): String {
        val dateFormat = csvDateFormat(locale)
        return buildString {
            appendLine(
                "session_id,start_time,end_time,session_name,session_emoji,session_tags," +
                    "min_db,avg_db,max_db,peak_db,frequency_weighting",
            )
            sessions.forEach { session ->
                appendLine(
                    listOf(
                        session.id.toString(),
                        dateFormat.format(Date(session.startTime)),
                        session.endTime?.let { dateFormat.format(Date(it)) }.orEmpty(),
                        session.name.orEmpty(),
                        session.emoji.orEmpty(),
                        session.tags.orEmpty(),
                        session.minDb.toString(),
                        session.avgDb.toString(),
                        session.maxDb.toString(),
                        session.peakDb.toString(),
                        session.frequencyWeighting,
                    ).joinToString(separator = ",") { CsvEscaper.escape(it) },
                )
            }
        }
    }

    fun buildMeasurementsCsv(
        sessions: List<SessionEntity>,
        measurementsBySessionId: Map<Long, List<MeasurementEntity>>,
        locale: Locale = Locale.getDefault(),
    ): String {
        val dateFormat = csvDateFormat(locale)
        return buildString {
            appendLine("session_id,session_name,session_emoji,session_tags,timestamp,raw_db,weighted_db")
            sessions.forEach { session ->
                measurementsBySessionId[session.id].orEmpty().forEach { measurement ->
                    appendLine(
                        listOf(
                            session.id.toString(),
                            session.name.orEmpty(),
                            session.emoji.orEmpty(),
                            session.tags.orEmpty(),
                            dateFormat.format(Date(measurement.timestamp)),
                            measurement.dbValue.toString(),
                            measurement.dbWeighted.toString(),
                        ).joinToString(separator = ",") { CsvEscaper.escape(it) },
                    )
                }
            }
        }
    }

    private fun csvDateFormat(locale: Locale): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}
