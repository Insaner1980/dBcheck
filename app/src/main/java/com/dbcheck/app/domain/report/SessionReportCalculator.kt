package com.dbcheck.app.domain.report

import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.noise.DosimeterCalculator
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.session.Session

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
        val nioshExposure =
            if (aWeightedExposureMetricsAvailable) {
                DosimeterCalculator.calculate(
                    standard = DosimeterStandard.NIOSH_REL,
                    laeqDb = laeqDb,
                    durationMs = durationMs,
                )
            } else {
                null
            }

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
            equivalentLevelLabel = equivalentLevelLabelForWeighting(session.frequencyWeighting),
            minDb = session.minDb,
            maxDb = session.maxDb,
            laeqDb = laeqDb,
            lcPeakDb = session.peakDb,
            twaDb = nioshExposure?.twaDb,
            dosePercent = nioshExposure?.dosePercent,
            aWeightedExposureMetricsAvailable = aWeightedExposureMetricsAvailable,
            measurementCount = sortedMeasurements.size,
            timeSeries = sortedMeasurements.map { ReportPoint(timestamp = it.timestamp, db = it.dbWeighted) },
            peakEvents = detectPeakEvents(sortedMeasurements, aWeightedExposureMetricsAvailable),
            dbHistogramBuckets = DbHistogramCalculator.calculate(sortedMeasurements),
        )
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
}
