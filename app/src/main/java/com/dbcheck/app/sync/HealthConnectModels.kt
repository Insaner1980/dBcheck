package com.dbcheck.app.sync

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.report.SessionReportData
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class HealthConnectNoiseDosePayload(
    val clientRecordId: String,
    val clientRecordVersion: Long,
    val startTime: Instant,
    val endTime: Instant,
    val startUtcOffsetSeconds: Int?,
    val endUtcOffsetSeconds: Int?,
    val durationMinutes: Int,
    val title: String,
    val notes: String,
) {
    companion object {
        fun fromReport(report: SessionReportData, text: HealthConnectNoiseDoseText): HealthConnectNoiseDosePayload? =
            if (report.durationMs <= 0L || report.endTime <= report.startTime) {
                null
            } else {
                val startTime = Instant.ofEpochMilli(report.startTime)
                val endTime = Instant.ofEpochMilli(report.endTime)
                val date = DATE_FORMATTER.format(startTime.atOffset(ZoneOffset.UTC))
                val durationMinutes =
                    (report.durationMs / MILLIS_PER_MINUTE)
                        .roundToInt()
                        .coerceAtLeast(1)

                HealthConnectNoiseDosePayload(
                    clientRecordId = "noise_dose_${date}_session_${report.sessionId}",
                    clientRecordVersion = report.endTime,
                    startTime = startTime,
                    endTime = endTime,
                    startUtcOffsetSeconds = report.timeZoneOffsets.startUtcOffsetSeconds,
                    endUtcOffsetSeconds = report.timeZoneOffsets.endUtcOffsetSeconds,
                    durationMinutes = durationMinutes,
                    title = text.title,
                    notes = report.toNotes(text),
                )
            }

        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
        private const val MILLIS_PER_MINUTE = 60_000.0
    }
}

data class HealthConnectNoiseDoseText(
    val title: String,
    val maxLabel: String,
    val peakLabel: String,
    val weightingLabel: String,
    val aWeightLabel: String,
    val bWeightLabel: String,
    val cWeightLabel: String,
    val zWeightLabel: String,
    val ituR468Label: String,
)

private fun SessionReportData.toNotes(text: HealthConnectNoiseDoseText): String = listOf(
        "$equivalentLevelLabel ${laeqDb.formatOne()} dB",
        "${text.maxLabel} ${maxDb.formatOne()} dB",
        "${text.peakLabel} ${lcPeakDb.formatOne()} dB",
        "${text.weightingLabel} ${weighting.toHealthConnectWeightingLabel(text)}",
    ).joinToString(separator = "\n")

internal fun HealthConnectNoiseDosePayload.toExerciseSessionRecord(device: Device): ExerciseSessionRecord =
    ExerciseSessionRecord(
        startTime = startTime,
        startZoneOffset = timeZoneOffset(startUtcOffsetSeconds),
        endTime = endTime,
        endZoneOffset = timeZoneOffset(endUtcOffsetSeconds),
        metadata =
            Metadata.activelyRecorded(
                device = device,
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion,
            ),
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
        title = title,
        notes = notes,
        segments = emptyList(),
        laps = emptyList(),
        exerciseRoute = null,
        plannedExerciseSessionId = null,
    )

data class HeartRateSample(val time: Instant, val beatsPerMinute: Long)

object HealthConnectHeartRateMapper {
    fun filterForSession(samples: List<HeartRateSample>, start: Instant, end: Instant): List<HeartRateSample> = samples
            .asSequence()
            .filter { sample -> !sample.time.isBefore(start) && sample.time.isBefore(end) }
            .sortedBy { it.time }
            .toList()
}

private fun Float.formatOne(): String = "%.1f".format(Locale.US, this)

private fun timeZoneOffset(totalSeconds: Int?): ZoneOffset? = totalSeconds?.let(ZoneOffset::ofTotalSeconds)

private fun String.toHealthConnectWeightingLabel(text: HealthConnectNoiseDoseText): String =
    when (WeightingType.entries.firstOrNull { it.name == this }) {
        WeightingType.A -> text.aWeightLabel
        WeightingType.B -> text.bWeightLabel
        WeightingType.C -> text.cWeightLabel
        WeightingType.Z -> text.zWeightLabel
        WeightingType.ITUR468 -> text.ituR468Label
        null -> this
    }
