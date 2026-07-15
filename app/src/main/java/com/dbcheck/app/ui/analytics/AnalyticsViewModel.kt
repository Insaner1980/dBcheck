package com.dbcheck.app.ui.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingRecoveryRepository
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.ExposureAnalyticsCalculator
import com.dbcheck.app.domain.analytics.ExposureNoiseZone
import com.dbcheck.app.domain.analytics.MonthlyExposureTrend
import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
import com.dbcheck.app.domain.analytics.YearlyExposureReport
import com.dbcheck.app.domain.audio.RtaFrame
import com.dbcheck.app.domain.audio.SoundDetection
import com.dbcheck.app.domain.audio.SoundDetectionError
import com.dbcheck.app.domain.audio.SoundDetectionState
import com.dbcheck.app.domain.audio.SpectralFrame
import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.service.AudioEngine
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.DailyExposureUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixUiState
import com.dbcheck.app.ui.analytics.state.HealthStatus
import com.dbcheck.app.ui.analytics.state.HearingRecoveryUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendPointUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.RtaBandUiState
import com.dbcheck.app.ui.analytics.state.RtaUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionChipUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.SpectralMode
import com.dbcheck.app.ui.analytics.state.SpectrogramBuffer
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

private data class AnalyticsMeasurements(
    val dailyAverages: List<DailyExposureAverage>,
    val environmentMix: EnvironmentMixAnalytics,
)

private data class LiveSpectralFrames(val spectralFrame: SpectralFrame?, val rtaFrame: RtaFrame?)

private data class LiveAnalyticsData(
    val isRecording: Boolean,
    val environmentMixCounts: EnvironmentExposureMixCounts,
    val soundDetectionState: SoundDetectionState,
    val spectralFrame: SpectralFrame?,
    val rtaFrame: RtaFrame?,
)

private data class HearingRecoveryAnalytics(
    val latestBaseline: HearingTestResult?,
    val latestRecovery: HearingRecoveryResult?,
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

private data class ProExposureUiStates(
    val hasExposureData: Boolean,
    val monthlyTrend: MonthlyTrendUiState,
    val yearlyReport: YearlyReportUiState,
)

private data class ProExposureWindow(
    val monthStartMs: Long,
    val yearStartMs: Long,
    val nowMs: Long,
    val zoneId: ZoneId,
)

internal const val ANALYTICS_LOAD_RETRY_DELAY_MILLIS = 1_000L

@Suppress("TooManyFunctions")
@HiltViewModel
class AnalyticsViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val measurementRepository: MeasurementRepository,
        private val sessionRepository: SessionRepository,
        private val preferencesRepository: PreferencesRepository,
        private val audioSessionManager: AudioSessionManager,
        private val audioEngine: AudioEngine,
        private val hearingTestRepository: HearingTestRepository,
        private val hearingRecoveryRepository: HearingRecoveryRepository,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
        val uiState: StateFlow<AnalyticsUiState> = _uiState
        private val selectedSection = MutableStateFlow(AnalyticsSection.OVERVIEW)
        private val selectedOverviewRange = MutableStateFlow(AnalyticsOverviewRange.WEEKLY)
        private val selectedSpectralMode = MutableStateFlow(SpectralMode.BARS)
        private val spectrogramBuffer = SpectrogramBuffer()

        init {
            loadAnalytics()
        }

        fun onSectionSelected(section: AnalyticsSection) {
            selectedSection.value = section
            _uiState.value =
                when (val state = _uiState.value) {
                    is AnalyticsUiState.Success -> state.copy(selectedSection = section)
                    else -> state
                }
        }

        fun onOverviewRangeSelected(range: AnalyticsOverviewRange) {
            selectedOverviewRange.value = range
            _uiState.value =
                when (val state = _uiState.value) {
                    is AnalyticsUiState.Success -> state.copy(selectedOverviewRange = range)
                    else -> state
                }
        }

        fun onSpectralModeSelected(mode: SpectralMode) {
            selectedSpectralMode.value = mode
            _uiState.value =
                when (val state = _uiState.value) {
                    is AnalyticsUiState.Success -> state.copy(selectedSpectralMode = mode)
                    else -> state
                }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun loadAnalytics() {
            viewModelScope.launch {
                combine(
                    measurementAnalyticsFlow(),
                    preferencesRepository.userPreferences,
                    liveAnalyticsDataFlow(),
                    proExposureUiStateFlow(),
                    hearingRecoveryAnalyticsFlow(),
                ) { analyticsMeasurements, prefs, liveAnalyticsData, exposureUiStates, hearingRecoveryAnalytics ->
                    buildUiState(
                        analyticsMeasurements,
                        prefs,
                        liveAnalyticsData,
                        exposureUiStates,
                        hearingRecoveryAnalytics,
                    )
                }.retryWhen { error, _ ->
                    if (error is CancellationException) throw error
                    _uiState.value =
                        AnalyticsUiState.Error(
                            error.toUserFacingMessage(context.getString(R.string.analytics_error_unable_to_load)),
                        )
                    delay(ANALYTICS_LOAD_RETRY_DELAY_MILLIS)
                    true
                }.collect { _uiState.value = it }
            }
        }

        private fun proExposureUiStateFlow() = proExposureAnalyticsFlow()
            .map { exposureAnalytics -> exposureAnalytics.toUiStates() }
            .flowOn(defaultDispatcher)

        private fun liveSpectralFramesFlow() = combine(
            audioEngine.spectralFrame,
            audioEngine.rtaFrame,
        ) { spectralFrame, rtaFrame ->
            LiveSpectralFrames(
                spectralFrame = spectralFrame,
                rtaFrame = rtaFrame,
            )
        }

        private fun liveAnalyticsDataFlow() = combine(
            audioSessionManager.isRecording,
            audioSessionManager.liveEnvironmentMixCounts,
            audioSessionManager.soundDetectionState,
            liveSpectralFramesFlow(),
        ) { isRecording, environmentMixCounts, soundDetectionState, liveSpectralFrames ->
            LiveAnalyticsData(
                isRecording = isRecording,
                environmentMixCounts = environmentMixCounts,
                soundDetectionState = soundDetectionState,
                spectralFrame = liveSpectralFrames.spectralFrame,
                rtaFrame = liveSpectralFrames.rtaFrame,
            )
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

        private fun hearingRecoveryAnalyticsFlow() = combine(
            hearingTestRepository.getLatestResult(),
            hearingRecoveryRepository.getLatestResult(),
        ) { latestBaseline, latestRecovery ->
            HearingRecoveryAnalytics(
                latestBaseline = latestBaseline,
                latestRecovery = latestRecovery,
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
            liveAnalyticsData: LiveAnalyticsData,
            exposureUiStates: ProExposureUiStates,
            hearingRecoveryAnalytics: HearingRecoveryAnalytics,
        ): AnalyticsUiState {
            val dailyAverages = analyticsMeasurements.dailyAverages
            val hasWeeklyExposureData = dailyAverages.isNotEmpty()
            val hasProExposureData = exposureUiStates.hasExposureData
            val spectralFrame = liveAnalyticsData.spectralFrame
            val spectrogramState = spectrogramBuffer.update(prefs.isProUser, spectralFrame)
            val hearingRecovery = mapHearingRecoveryState(prefs.isProUser, hearingRecoveryAnalytics)
            val hasRecoveryContent =
                hearingRecovery != HearingRecoveryUiState.LockedPreview ||
                    hearingRecoveryAnalytics.latestBaseline != null ||
                    hearingRecoveryAnalytics.latestRecovery != null
            if (
                shouldShowEmptyAnalytics(
                    hasWeeklyExposureData = hasWeeklyExposureData,
                    hasProExposureData = hasProExposureData,
                    isRecording = liveAnalyticsData.isRecording,
                    hasRecoveryContent = hasRecoveryContent,
                )
            ) {
                return AnalyticsUiState.Empty
            }

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
                isRecording = liveAnalyticsData.isRecording,
                selectedSection = selectedSection.value,
                selectedOverviewRange = selectedOverviewRange.value,
                selectedSpectralMode = selectedSpectralMode.value,
                spectralAnalysis = mapSpectralState(prefs.isProUser, spectralFrame),
                spectrogram = spectrogramState,
                rta = mapRtaState(prefs.isProUser, liveAnalyticsData.rtaFrame),
                environmentMix = mapEnvironmentMixState(analyticsMeasurements.environmentMix),
                activeEnvironmentMix =
                    mapActiveEnvironmentMixState(
                        isProUser = prefs.isProUser,
                        isRecording = liveAnalyticsData.isRecording,
                        counts = liveAnalyticsData.environmentMixCounts,
                    ),
                soundDetection =
                    mapSoundDetectionState(
                        isProUser = prefs.isProUser,
                        isFeatureEnabled = prefs.soundDetectionEnabled,
                        isRecording = liveAnalyticsData.isRecording,
                        state = liveAnalyticsData.soundDetectionState,
                    ),
                soundDetectionEnabled = prefs.isProUser && prefs.soundDetectionEnabled,
                sleepCardEnabled = prefs.isProUser && prefs.sleepCardEnabled,
                hearingRecovery = hearingRecovery,
                tinnitusPitchProfile =
                    if (prefs.isProUser) {
                        prefs.tinnitusPitchProfile
                    } else {
                        TinnitusPitchProfile()
                    },
                monthlyTrend = exposureUiStates.monthlyTrend,
                yearlyReport = exposureUiStates.yearlyReport,
            )
        }

        private fun weeklyAverage(dailyAverages: List<DailyExposureAverage>, hasExposureData: Boolean): Float =
            if (hasExposureData) {
                dailyAverages.energyAverage()
            } else {
                0f
            }

        private fun shouldShowEmptyAnalytics(
            hasWeeklyExposureData: Boolean,
            hasProExposureData: Boolean,
            isRecording: Boolean,
            hasRecoveryContent: Boolean,
        ): Boolean = listOf(
                hasWeeklyExposureData,
                hasProExposureData,
                isRecording,
                hasRecoveryContent,
            ).none { it }

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
                                    centerFrequencyHz = band.centerFrequencyHz,
                                )
                            },
                        dominantFrequencyHz = spectralFrame.dominantFrequencyHz,
                        bandwidth = spectralFrame.bandwidth,
                    )
            }

        private fun mapRtaState(isProUser: Boolean, rtaFrame: RtaFrame?): RtaUiState = when {
                !isProUser -> RtaUiState.LockedPreview

                rtaFrame == null -> RtaUiState.Empty

                else ->
                    RtaUiState.Data(
                        bands =
                            rtaFrame.bands.map { band ->
                                RtaBandUiState(
                                    centerFrequencyHz = band.centerFrequencyHz,
                                    normalizedAmplitude = band.normalizedAmplitude,
                                )
                            },
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

        private fun mapActiveEnvironmentMixState(
            isProUser: Boolean,
            isRecording: Boolean,
            counts: EnvironmentExposureMixCounts,
        ): EnvironmentMixUiState = when {
                !isProUser -> EnvironmentMixUiState.LockedPreview
                !isRecording || counts.totalCount <= 0L -> EnvironmentMixUiState.Empty
                else -> EnvironmentMixUiState.Data(rows = environmentMixRows(counts))
            }

        private fun mapSoundDetectionState(
            isProUser: Boolean,
            isFeatureEnabled: Boolean,
            isRecording: Boolean,
            state: SoundDetectionState,
        ): SoundDetectionUiState = when {
                !isProUser -> SoundDetectionUiState.LockedPreview
                !isFeatureEnabled -> SoundDetectionUiState.Idle
                state.error != null -> SoundDetectionUiState.Error(state.error.toMessage())
                !isRecording || !state.isEnabled || state.current == null -> SoundDetectionUiState.Idle
                else -> state.toLiveUiState()
            }

        private fun mapHearingRecoveryState(
            isProUser: Boolean,
            analytics: HearingRecoveryAnalytics,
        ): HearingRecoveryUiState = when {
            !isProUser -> HearingRecoveryUiState.LockedPreview

            analytics.latestBaseline == null -> HearingRecoveryUiState.MissingBaseline

            analytics.latestRecovery == null -> HearingRecoveryUiState.Ready

            else ->
                HearingRecoveryUiState.Result(
                    averageShiftDb = analytics.latestRecovery.averageShiftDb,
                    maxShiftDb = analytics.latestRecovery.maxShiftDb,
                    status = analytics.latestRecovery.status,
                    timestamp = analytics.latestRecovery.timestamp,
                )
        }

        private fun SoundDetectionState.toLiveUiState(): SoundDetectionUiState.Live = SoundDetectionUiState.Live(
                label = current?.label.orEmpty(),
                confidencePercent = current?.confidence.toPercent(),
                recentDetections = recentDetections.map { it.toChipUiState() },
            )

        private fun SoundDetection.toChipUiState(): SoundDetectionChipUiState = SoundDetectionChipUiState(
                label = label,
                confidencePercent = confidence.toPercent(),
            )

        private fun Float?.toPercent(): Int =
            ((this ?: 0f).coerceIn(MIN_CONFIDENCE, MAX_CONFIDENCE) * PERCENT_TOTAL).roundToInt()

        private fun SoundDetectionError.toMessage(): String = when (this) {
            SoundDetectionError.CLASSIFICATION_UNAVAILABLE ->
                context.getString(R.string.sound_detection_error_unavailable)
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

        private fun environmentMixRows(counts: EnvironmentExposureMixCounts): List<EnvironmentMixRowUiState> =
            ExposureAnalyticsCalculator.environmentMixPercentages(counts).map { row ->
                EnvironmentMixRowUiState(
                    category = row.zone.toEnvironmentMixCategory(),
                    percent = row.percent,
                )
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

        private fun ProExposureAnalytics.toUiStates(): ProExposureUiStates = ProExposureUiStates(
            hasExposureData = hasExposureData(),
            monthlyTrend = mapMonthlyTrendState(this),
            yearlyReport = mapYearlyReportState(this),
        )

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

        private companion object {
            const val MIN_CONFIDENCE = 0f
            const val MAX_CONFIDENCE = 1f
            const val PERCENT_TOTAL = 100
            const val ROLLING_WINDOW_REFRESH_MILLIS = 60_000L
        }
    }
