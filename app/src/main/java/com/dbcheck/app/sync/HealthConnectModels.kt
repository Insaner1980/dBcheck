package com.dbcheck.app.sync

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.report.equivalentLevelLabelForWeighting
import com.dbcheck.app.domain.session.Session
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
    val durationMinutes: Int,
    val title: String,
    val notes: String,
) {
    companion object {
        fun fromSession(
            session: Session,
            laeqDb: Float,
            text: HealthConnectNoiseDoseText,
        ): HealthConnectNoiseDosePayload? {
            val endTimeMs = session.endTime
            return if (endTimeMs == null || endTimeMs <= session.startTime) {
                null
            } else {
                val startTime = Instant.ofEpochMilli(session.startTime)
                val endTime = Instant.ofEpochMilli(endTimeMs)
                val date = DATE_FORMATTER.format(startTime.atOffset(ZoneOffset.UTC))
                val durationMinutes =
                    ((endTimeMs - session.startTime) / MILLIS_PER_MINUTE)
                        .roundToInt()
                        .coerceAtLeast(1)

                HealthConnectNoiseDosePayload(
                    clientRecordId = "noise_dose_${date}_session_${session.id}",
                    clientRecordVersion = endTimeMs,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    title = text.title,
                    notes = session.toNotes(laeqDb, text),
                )
            }
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

private fun Session.toNotes(laeqDb: Float, text: HealthConnectNoiseDoseText): String = listOf(
        "${equivalentLevelLabelForWeighting(frequencyWeighting)} ${laeqDb.formatOne()} dB",
        "${text.maxLabel} ${maxDb.formatOne()} dB",
        "${text.peakLabel} ${peakDb.formatOne()} dB",
        "${text.weightingLabel} ${frequencyWeighting.toHealthConnectWeightingLabel(text)}",
    ).joinToString(separator = "\n")

internal fun HealthConnectNoiseDosePayload.toExerciseSessionRecord(device: Device): ExerciseSessionRecord =
    ExerciseSessionRecord(
        startTime = startTime,
        startZoneOffset = null,
        endTime = endTime,
        endZoneOffset = null,
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

private fun String.toHealthConnectWeightingLabel(text: HealthConnectNoiseDoseText): String =
    when (WeightingType.entries.firstOrNull { it.name == this }) {
        WeightingType.A -> text.aWeightLabel
        WeightingType.B -> text.bWeightLabel
        WeightingType.C -> text.cWeightLabel
        WeightingType.Z -> text.zWeightLabel
        WeightingType.ITUR468 -> text.ituR468Label
        null -> this
    }
