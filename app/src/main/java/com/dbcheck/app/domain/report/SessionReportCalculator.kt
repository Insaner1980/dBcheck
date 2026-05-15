package com.dbcheck.app.domain.report

import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.session.Session
import kotlin.math.log10
import kotlin.math.pow

object SessionReportCalculator {
    fun build(
        session: Session,
        measurements: List<ReportMeasurement>,
        generatedAtMs: Long = System.currentTimeMillis(),
    ): SessionReportData {
        val endTime = session.endTime ?: generatedAtMs
        val durationMs = (endTime - session.startTime).coerceAtLeast(0L)
        val sortedMeasurements = measurements.sortedBy { it.timestamp }
        val laeqDb = session.avgDb
        val aWeightedExposureMetricsAvailable = session.frequencyWeighting == WeightingType.A.name

        return SessionReportData(
            sessionId = session.id,
            sessionName = session.name ?: defaultSessionName(session.startTime),
            sessionCustomName = session.name,
            sessionEmoji = session.emoji,
            sessionTags = session.tags,
            startTime = session.startTime,
            endTime = endTime,
            generatedAtMs = generatedAtMs,
            durationMs = durationMs,
            weighting = session.frequencyWeighting,
            minDb = session.minDb,
            maxDb = session.maxDb,
            laeqDb = laeqDb,
            lcPeakDb = session.peakDb,
            twaDb = if (aWeightedExposureMetricsAvailable) calculateTwaDb(laeqDb, durationMs) else null,
            dosePercent =
                if (aWeightedExposureMetricsAvailable) {
                    calculateNioshDosePercent(laeqDb, durationMs)
                } else {
                    null
                },
            aWeightedExposureMetricsAvailable = aWeightedExposureMetricsAvailable,
            measurementCount = sortedMeasurements.size,
            timeSeries = sortedMeasurements.map { ReportPoint(timestamp = it.timestamp, db = it.dbWeighted) },
            peakEvents = detectPeakEvents(sortedMeasurements, aWeightedExposureMetricsAvailable),
        )
    }

    private fun calculateNioshDosePercent(laeqDb: Float, durationMs: Long): Float {
        val durationHours = durationMs / MILLIS_PER_HOUR
        if (durationHours <= 0.0 || laeqDb <= 0f) return 0f

        val allowableHours = NIOSH_REFERENCE_HOURS * 2.0.pow((NIOSH_REFERENCE_DB - laeqDb) / NIOSH_EXCHANGE_RATE_DB)
        return (durationHours / allowableHours * 100.0).toFloat()
    }

    private fun calculateTwaDb(laeqDb: Float, durationMs: Long): Float {
        val durationHours = durationMs / MILLIS_PER_HOUR
        return if (durationHours <= 0.0 || laeqDb <= 0f) {
            0f
        } else {
            (laeqDb + 10.0 * log10(durationHours / NIOSH_REFERENCE_HOURS)).toFloat().coerceAtLeast(0f)
        }
    }

    private fun detectPeakEvents(
        measurements: List<ReportMeasurement>,
        aWeightedExposureMetricsAvailable: Boolean,
    ): List<PeakEvent> {
        if (!aWeightedExposureMetricsAvailable) return emptyList()

        val events = mutableListOf<PeakEvent>()
        var activeStart: Long? = null
        var activeEnd = 0L
        var activePeakTime = 0L
        var activePeakDb = 0f

        fun finishActiveEvent() {
            val start = activeStart ?: return
            events +=
                PeakEvent(
                    startTime = start,
                    endTime = activeEnd,
                    peakTime = activePeakTime,
                    maxDb = activePeakDb,
                )
            activeStart = null
            activeEnd = 0L
            activePeakTime = 0L
            activePeakDb = 0f
        }

        measurements.forEach { measurement ->
            val eventDb = measurement.dbWeighted
            if (eventDb >= NoiseLevel.ELEVATED.maxDb) {
                if (activeStart == null) {
                    activeStart = measurement.timestamp
                    activePeakTime = measurement.timestamp
                    activePeakDb = eventDb
                }
                activeEnd = measurement.timestamp
                if (eventDb > activePeakDb) {
                    activePeakDb = eventDb
                    activePeakTime = measurement.timestamp
                }
            } else {
                finishActiveEvent()
            }
        }
        finishActiveEvent()

        return events
    }

    private fun defaultSessionName(startTime: Long): String = "Session $startTime"

    private const val NIOSH_REFERENCE_DB = 85.0
    private const val NIOSH_REFERENCE_HOURS = 8.0
    private const val NIOSH_EXCHANGE_RATE_DB = 3.0
    private const val MILLIS_PER_HOUR = 60.0 * 60.0 * 1000.0
}
