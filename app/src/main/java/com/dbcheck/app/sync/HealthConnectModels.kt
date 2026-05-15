package com.dbcheck.app.sync

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.dbcheck.app.domain.audio.WeightingType
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
                    title = "dBcheck noise exposure",
                    notes = session.toNotes(laeqDb),
                )
            }
        }

        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
        private const val MILLIS_PER_MINUTE = 60_000.0
    }
}

private fun Session.toNotes(laeqDb: Float): String =
    listOf(
        "LAeq ${laeqDb.formatOne()} dB",
        "Max ${maxDb.formatOne()} dB",
        "Peak ${peakDb.formatOne()} dB",
        "Weighting ${frequencyWeighting.toHealthConnectWeightingLabel()}",
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

data class HeartRateSample(
    val time: Instant,
    val beatsPerMinute: Long,
)

object HealthConnectHeartRateMapper {
    fun filterForSession(
        samples: List<HeartRateSample>,
        start: Instant,
        end: Instant,
    ): List<HeartRateSample> =
        samples
            .asSequence()
            .filter { sample -> !sample.time.isBefore(start) && sample.time.isBefore(end) }
            .sortedBy { it.time }
            .toList()
}

private fun Float.formatOne(): String = "%.1f".format(Locale.US, this)

private fun String.toHealthConnectWeightingLabel(): String =
    WeightingType.entries.firstOrNull { it.name == this }?.displayName ?: this
