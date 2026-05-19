package com.dbcheck.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.ExposureAnalyticsCalculator
import com.dbcheck.app.domain.analytics.ExposureNoiseZone
import com.dbcheck.app.domain.analytics.MonthlyExposureTrend
import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
import com.dbcheck.app.domain.analytics.YearlyExposureReport
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.SpectralFrame
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.DailyExposureUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixUiState
import com.dbcheck.app.ui.analytics.state.HealthStatus
import com.dbcheck.app.ui.analytics.state.MonthlyTrendPointUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private data class AnalyticsMeasurements(
    val dailyAverages: List<DailyExposureAverage>,
    val environmentMix: EnvironmentMixAnalytics,
)

private sealed interface EnvironmentMixAnalytics {
    data object Locked : EnvironmentMixAnalytics

    data class Data(val counts: EnvironmentExposureMixCounts) : EnvironmentMixAnalytics
}

private sealed interface ProExposureAnalytics {
    data object Locked : ProExposureAnalytics

    data class Data(
        val monthlyMeasurements: List<WeightedExposureMeasurement>,
        val yearlyMeasurements: List<WeightedExposureMeasurement>,
        val yearlySessionCount: Int,
        val nowMs: Long,
        val zoneId: ZoneId,
    ) : ProExposureAnalytics
}

private data class ProExposureWindow(
    val monthStartMs: Long,
    val yearStartMs: Long,
    val nowMs: Long,
    val zoneId: ZoneId,
)

@HiltViewModel
class AnalyticsViewModel
    @Inject
    constructor(
        private val measurementRepository: MeasurementRepository,
        private val sessionRepository: SessionRepository,
        private val preferencesRepository: PreferencesRepository,
        private val audioSessionManager: AudioSessionManager,
        private val audioEngine: AudioEngine,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
        val uiState: StateFlow<AnalyticsUiState> = _uiState

        init {
            loadAnalytics()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun loadAnalytics() {
            viewModelScope.launch {
                combine(
                    measurementAnalyticsFlow(),
                    preferencesRepository.userPreferences,
                    audioSessionManager.isRecording,
                    audioEngine.spectralFrame,
                    proExposureAnalyticsFlow(),
                ) { analyticsMeasurements, prefs, isRecording, spectralFrame, exposureAnalytics ->
                    buildUiState(analyticsMeasurements, prefs, isRecording, spectralFrame, exposureAnalytics)
                }.collect { _uiState.value = it }
            }
        }

        private fun measurementAnalyticsFlow() = combine(
                measurementRepository.getDailyAveragesLast7Days(),
                environmentMixAnalyticsFlow(),
            ) { dailyAverages, environmentMix ->
                AnalyticsMeasurements(
                    dailyAverages = dailyAverages,
                    environmentMix = environmentMix,
                )
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun environmentMixAnalyticsFlow() = preferencesRepository.userPreferences.flatMapLatest { prefs ->
                if (!prefs.isProUser) {
                    flowOf(EnvironmentMixAnalytics.Locked)
                } else {
                    measurementRepository.getEnvironmentMixLast7Days().map { counts ->
                        EnvironmentMixAnalytics.Data(counts)
                    }
                }
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun proExposureAnalyticsFlow() = preferencesRepository.userPreferences.flatMapLatest { prefs ->
            if (!prefs.isProUser) {
                flowOf(ProExposureAnalytics.Locked)
            } else {
                proExposureAnalyticsDataFlow()
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun proExposureAnalyticsDataFlow() = proExposureWindowFlow().flatMapLatest { window ->
            combine(
                measurementRepository.getWeightedMeasurementsInRange(window.monthStartMs, window.nowMs),
                measurementRepository.getWeightedMeasurementsInRange(window.yearStartMs, window.nowMs),
                sessionRepository.getCompletedSessionCountInRange(window.yearStartMs, window.nowMs),
            ) { monthlyMeasurements, yearlyMeasurements, yearlySessionCount ->
                ProExposureAnalytics.Data(
                    monthlyMeasurements = monthlyMeasurements,
                    yearlyMeasurements = yearlyMeasurements,
                    yearlySessionCount = yearlySessionCount,
                    nowMs = window.nowMs,
                    zoneId = window.zoneId,
                )
            }
        }

        private fun proExposureWindowFlow() = flow {
            while (currentCoroutineContext().isActive) {
                val nowMs = System.currentTimeMillis()
                val zoneId = ZoneId.systemDefault()
                emit(
                    ProExposureWindow(
                        monthStartMs = ExposureAnalyticsCalculator.rollingMonthStartMs(nowMs, zoneId),
                        yearStartMs = ExposureAnalyticsCalculator.rollingYearStartMs(nowMs, zoneId),
                        nowMs = nowMs,
                        zoneId = zoneId,
                    ),
                )
                delay(ROLLING_WINDOW_REFRESH_MILLIS)
            }
        }

        private fun buildUiState(
            analyticsMeasurements: AnalyticsMeasurements,
            prefs: UserPreferences,
            isRecording: Boolean,
            spectralFrame: SpectralFrame?,
            exposureAnalytics: ProExposureAnalytics,
        ): AnalyticsUiState {
            val dailyAverages = analyticsMeasurements.dailyAverages
            val hasWeeklyExposureData = dailyAverages.isNotEmpty()
            val hasProExposureData = exposureAnalytics.hasExposureData()
            if (!hasWeeklyExposureData && !hasProExposureData && !isRecording) return AnalyticsUiState.Empty

            val nowMs = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val weeklyAvg = weeklyAverage(dailyAverages, hasWeeklyExposureData)
            return AnalyticsUiState.Success(
                weeklyAverageDb = weeklyAvg,
                dailyAverages = dailyAverages.map { it.toUiState(nowMs = nowMs, zoneId = zoneId) },
                healthStatus = healthStatusFor(weeklyAvg),
                todayVsWeekPercent = todayVsWeekPercent(dailyAverages, weeklyAvg, nowMs, zoneId),
                isProUser = prefs.isProUser,
                hasExposureData = hasWeeklyExposureData,
                isRecording = isRecording,
                spectralAnalysis = mapSpectralState(prefs.isProUser, spectralFrame),
                environmentMix = mapEnvironmentMixState(analyticsMeasurements.environmentMix),
                monthlyTrend = mapMonthlyTrendState(exposureAnalytics),
                yearlyReport = mapYearlyReportState(exposureAnalytics),
            )
        }

        private fun weeklyAverage(dailyAverages: List<DailyExposureAverage>, hasExposureData: Boolean): Float =
            if (hasExposureData) {
                dailyAverages.energyAverage()
            } else {
                0f
            }

        private fun todayVsWeekPercent(
            dailyAverages: List<DailyExposureAverage>,
            weeklyAvg: Float,
            nowMs: Long,
            zoneId: ZoneId,
        ): Int {
            val todayStartMs = dayStartMs(nowMs, zoneId)
            val todayAvg = dailyAverages.firstOrNull { it.dayStartMs == todayStartMs }?.avgDb ?: return 0
            return if (weeklyAvg > 0f) {
                ((todayAvg - weeklyAvg) / weeklyAvg * PERCENT_TOTAL).toInt()
            } else {
                0
            }
        }

        private fun healthStatusFor(weeklyAvg: Float): HealthStatus = when {
                weeklyAvg < NoiseLevel.NORMAL.maxDb -> HealthStatus.SAFE
                weeklyAvg < NoiseLevel.ELEVATED.maxDb -> HealthStatus.WARNING
                else -> HealthStatus.DANGER
            }

        private fun mapSpectralState(isProUser: Boolean, spectralFrame: SpectralFrame?): SpectralAnalysisUiState =
            when {
                !isProUser -> SpectralAnalysisUiState.LockedPreview

                spectralFrame == null -> SpectralAnalysisUiState.Idle

                else ->
                    SpectralAnalysisUiState.Live(
                        bands =
                            spectralFrame.bands.map { band ->
                                SpectralBandUiState(
                                    normalizedAmplitude = band.normalizedAmplitude,
                                )
                            },
                        dominantFrequencyHz = spectralFrame.dominantFrequencyHz,
                        bandwidth = spectralFrame.bandwidth,
                    )
            }

        private fun mapEnvironmentMixState(environmentMix: EnvironmentMixAnalytics): EnvironmentMixUiState =
            when (environmentMix) {
                EnvironmentMixAnalytics.Locked -> EnvironmentMixUiState.LockedPreview

                is EnvironmentMixAnalytics.Data ->
                    if (environmentMix.counts.totalCount <= 0L) {
                        EnvironmentMixUiState.Empty
                    } else {
                        EnvironmentMixUiState.Data(rows = environmentMixRows(environmentMix.counts))
                    }
            }

        private fun mapMonthlyTrendState(exposureAnalytics: ProExposureAnalytics): MonthlyTrendUiState =
            when (exposureAnalytics) {
                ProExposureAnalytics.Locked -> MonthlyTrendUiState.LockedPreview

                is ProExposureAnalytics.Data -> {
                    val trend =
                        ExposureAnalyticsCalculator.buildMonthlyTrend(
                            measurements = exposureAnalytics.monthlyMeasurements,
                            nowMs = exposureAnalytics.nowMs,
                            zoneId = exposureAnalytics.zoneId,
                        )
                    trend.toUiState()
                }
            }

        private fun MonthlyExposureTrend.toUiState(): MonthlyTrendUiState = if (measurementCount <= 0) {
                MonthlyTrendUiState.Empty
            } else {
                MonthlyTrendUiState.Data(
                    points =
                        points.map { point ->
                            MonthlyTrendPointUiState(
                                dayStartMs = point.dayStartMs,
                                laeqDb = point.laeqDb,
                                maxDb = point.maxDb,
                            )
                        },
                    laeqDb = laeqDb,
                    loudestDb = loudestDb,
                )
            }

        private fun mapYearlyReportState(exposureAnalytics: ProExposureAnalytics): YearlyReportUiState =
            when (exposureAnalytics) {
                ProExposureAnalytics.Locked -> YearlyReportUiState.LockedPreview

                is ProExposureAnalytics.Data -> {
                    val report =
                        ExposureAnalyticsCalculator.buildYearlyReport(
                            measurements = exposureAnalytics.yearlyMeasurements,
                            completedSessionCount = exposureAnalytics.yearlySessionCount,
                            nowMs = exposureAnalytics.nowMs,
                            zoneId = exposureAnalytics.zoneId,
                        )
                    report.toUiState()
                }
            }

        private fun YearlyExposureReport.toUiState(): YearlyReportUiState = if (measurementCount <= 0) {
                YearlyReportUiState.Empty
            } else {
                YearlyReportUiState.Data(
                    totalSessions = totalSessions,
                    laeqDb = laeqDb,
                    loudestDayLabel = loudestDayStartMs?.let(::formatDayLabel) ?: "--",
                    loudestDb = loudestDb,
                    zoneRows =
                        zoneDistribution.map { row ->
                            EnvironmentMixRowUiState(
                                category = row.zone.toEnvironmentMixCategory(),
                                percent = row.percent,
                            )
                        },
                )
            }

        private fun environmentMixRows(counts: EnvironmentExposureMixCounts): List<EnvironmentMixRowUiState> {
            val categoryCounts =
                listOf(
                    EnvironmentMixCategory.QUIET to counts.quietCount,
                    EnvironmentMixCategory.MODERATE to counts.moderateCount,
                    EnvironmentMixCategory.LOUD to counts.loudCount,
                    EnvironmentMixCategory.CRITICAL to counts.criticalCount,
                )
            val totalCount = counts.totalCount.toDouble()
            val roundedRows =
                categoryCounts.mapIndexed { index, (category, count) ->
                    val rawPercent = count * PERCENT_TOTAL / totalCount
                    RoundedEnvironmentMixRow(
                        index = index,
                        category = category,
                        percent = rawPercent.toInt(),
                        remainder = rawPercent - rawPercent.toInt(),
                    )
                }
            val missingPercent = PERCENT_TOTAL - roundedRows.sumOf { it.percent }
            val incrementedIndexes =
                roundedRows
                    .sortedWith(compareByDescending<RoundedEnvironmentMixRow> { it.remainder }.thenBy { it.index })
                    .take(missingPercent)
                    .map { it.index }
                    .toSet()

            return roundedRows.map { row ->
                EnvironmentMixRowUiState(
                    category = row.category,
                    percent = row.percent + if (row.index in incrementedIndexes) 1 else 0,
                )
            }
        }

        private fun ExposureNoiseZone.toEnvironmentMixCategory(): EnvironmentMixCategory = when (this) {
                ExposureNoiseZone.QUIET -> EnvironmentMixCategory.QUIET
                ExposureNoiseZone.MODERATE -> EnvironmentMixCategory.MODERATE
                ExposureNoiseZone.LOUD -> EnvironmentMixCategory.LOUD
                ExposureNoiseZone.CRITICAL -> EnvironmentMixCategory.CRITICAL
            }

        private fun DailyExposureAverage.toUiState(nowMs: Long, zoneId: ZoneId): DailyExposureUiState =
            DailyExposureUiState(
                dayStartMs = dayStartMs,
                avgDb = avgDb,
                maxDb = maxDb,
                isToday = dayStartMs == dayStartMs(nowMs, zoneId),
            )

        private fun List<DailyExposureAverage>.energyAverage(): Float {
            val totalCount = sumOf { it.sampleCount }
            if (totalCount <= 0) return 0f

            val totalEnergy =
                sumOf { average ->
                    DecibelMath.energyFromDb(average.avgDb) * average.sampleCount
                }
            return DecibelMath.energyAverageDb(totalEnergy, totalCount) ?: 0f
        }

        private fun formatDayLabel(timestampMs: Long): String =
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))

        private fun ProExposureAnalytics.hasExposureData(): Boolean = when (this) {
            ProExposureAnalytics.Locked -> false
            is ProExposureAnalytics.Data -> monthlyMeasurements.isNotEmpty() || yearlyMeasurements.isNotEmpty()
        }

        private fun dayStartMs(timestampMs: Long, zoneId: ZoneId): Long = Instant
            .ofEpochMilli(timestampMs)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        private data class RoundedEnvironmentMixRow(
            val index: Int,
            val category: EnvironmentMixCategory,
            val percent: Int,
            val remainder: Double,
        )

        private companion object {
            const val PERCENT_TOTAL = 100
            const val ROLLING_WINDOW_REFRESH_MILLIS = 60_000L
        }
    }
