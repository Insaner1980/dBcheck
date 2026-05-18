package com.dbcheck.app.ui.analytics

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.DailyExposureAverage
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.analytics.WeightedExposureMeasurement
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.SpectralBand
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.audio.SpectralFrame
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelSpectralTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dailyAverages = MutableStateFlow<List<DailyExposureAverage>>(emptyList())
    private val environmentMixCounts = MutableStateFlow(EnvironmentExposureMixCounts())
    private val monthlyMeasurements = MutableStateFlow<List<WeightedExposureMeasurement>>(emptyList())
    private val yearlyMeasurements = MutableStateFlow<List<WeightedExposureMeasurement>>(emptyList())
    private val yearlySessionCount = MutableStateFlow(0)
    private val preferences = MutableStateFlow(UserPreferences(isProUser = true))
    private val isRecording = MutableStateFlow(false)
    private val spectralFrame = MutableStateFlow<SpectralFrame?>(null)
    private val createdViewModels = mutableListOf<AnalyticsViewModel>()

    private val measurementRepository =
        mockk<MeasurementRepository> {
            every { getDailyAveragesLast7Days() } returns dailyAverages
            every { getEnvironmentMixLast7Days() } returns environmentMixCounts
            every { getWeightedMeasurementsInRange(any(), any()) } returnsMany
                listOf(monthlyMeasurements, yearlyMeasurements)
        }
    private val sessionRepository =
        mockk<SessionRepository> {
            every { getCompletedSessionCountInRange(any(), any()) } returns yearlySessionCount
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }
    private val audioSessionManager =
        mockk<AudioSessionManager>()
    private val audioEngine =
        mockk<AudioEngine>()

    @Test
    fun noDataAndNoRecordingShowsEmptyState() =
        runAnalyticsTest {
            val viewModel = createViewModel()

            assertEquals(AnalyticsUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun proUserWithYearlyDataButNoWeeklyDataShowsSuccessState() = runAnalyticsTest {
        yearlyMeasurements.value =
            listOf(
                WeightedExposureMeasurement(timestamp = System.currentTimeMillis(), dbWeighted = 72f),
            )
        yearlySessionCount.value = 1

        val state = createViewModel().uiState.value as AnalyticsUiState.Success

        assertFalse(state.hasExposureData)
        assertTrue(state.yearlyReport is YearlyReportUiState.Data)
    }

    @Test
    fun noDataWhileRecordingShowsSuccessSoLiveSpectrumCanRender() =
        runAnalyticsTest {
            isRecording.value = true

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertFalse(state.hasExposureData)
            assertTrue(state.isRecording)
        }

    @Test
    fun proUserReceivesLiveSpectralFrame() =
        runAnalyticsTest {
            isRecording.value = true
            spectralFrame.value = liveFrame()

            val state = createViewModel().uiState.value as AnalyticsUiState.Success
            val spectralState = state.spectralAnalysis as SpectralAnalysisUiState.Live

            assertEquals(1000f, spectralState.dominantFrequencyHz, 0f)
            assertEquals(SpectralBandwidth.NARROW, spectralState.bandwidth)
            assertEquals(24, spectralState.bands.size)
        }

    @Test
    fun freeUserDoesNotReceiveLiveSpectralFrame() =
        runAnalyticsTest {
            preferences.value = UserPreferences(isProUser = false)
            isRecording.value = true
            spectralFrame.value = liveFrame()

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(SpectralAnalysisUiState.LockedPreview, state.spectralAnalysis)
        }

    @Test
    fun proUserReceivesEnvironmentMixPercentagesFromSevenDayCounts() =
        runAnalyticsTest {
            seedEnvironmentMixInput(
                quietCount = 1L,
                moderateCount = 2L,
                loudCount = 3L,
                criticalCount = 4L,
                totalCount = 10L,
            )

            val state = createViewModel().uiState.value as AnalyticsUiState.Success
            val environmentMix = state.environmentMix as EnvironmentMixUiState.Data

            assertEquals(
                listOf(
                    EnvironmentMixCategory.QUIET to 10,
                    EnvironmentMixCategory.MODERATE to 20,
                    EnvironmentMixCategory.LOUD to 30,
                    EnvironmentMixCategory.CRITICAL to 40,
                ),
                environmentMix.rows.map { it.category to it.percent },
            )
        }

    @Test
    fun freeUserDoesNotReceiveEnvironmentMixCounts() =
        runAnalyticsTest {
            preferences.value = UserPreferences(isProUser = false)
            seedEnvironmentMixInput(
                quietCount = 1L,
                moderateCount = 2L,
                loudCount = 3L,
                criticalCount = 4L,
                totalCount = 10L,
            )

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(EnvironmentMixUiState.LockedPreview, state.environmentMix)
            verify(exactly = 0) { measurementRepository.getEnvironmentMixLast7Days() }
        }

    @Test
    fun emptyEnvironmentMixCountsReturnEmptyState() =
        runAnalyticsTest {
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            environmentMixCounts.value = EnvironmentExposureMixCounts()

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(EnvironmentMixUiState.Empty, state.environmentMix)
        }

    @Test
    fun missingTodayDoesNotCompareYesterdayAsToday() = runAnalyticsTest {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        dailyAverages.value =
            listOf(
                DailyExposureAverage(dayStartMs = today.minusDays(2).toStartMs(zoneId), avgDb = 60f, maxDb = 60f),
                DailyExposureAverage(dayStartMs = today.minusDays(1).toStartMs(zoneId), avgDb = 80f, maxDb = 80f),
            )

        val state = createViewModel().uiState.value as AnalyticsUiState.Success

        assertEquals(0, state.todayVsWeekPercent)
    }

    @Test
    fun environmentMixPercentagesSumToOneHundredAfterRounding() =
        runAnalyticsTest {
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            environmentMixCounts.value =
                EnvironmentExposureMixCounts(
                    quietCount = 1L,
                    moderateCount = 1L,
                    loudCount = 1L,
                    criticalCount = 0L,
                    totalCount = 3L,
                )

            val state = createViewModel().uiState.value as AnalyticsUiState.Success
            val environmentMix = state.environmentMix as EnvironmentMixUiState.Data

            assertEquals(100, environmentMix.rows.sumOf { it.percent })
        }

    @Test
    fun proUserReceivesMonthlyTrendAndYearlyReportFromExposureMeasurements() =
        runAnalyticsTest {
            val now = System.currentTimeMillis()
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            monthlyMeasurements.value =
                listOf(
                    WeightedExposureMeasurement(timestamp = now - 1_000L, dbWeighted = 64f),
                    WeightedExposureMeasurement(timestamp = now, dbWeighted = 74f),
                )
            yearlyMeasurements.value =
                listOf(
                    WeightedExposureMeasurement(timestamp = now - 1_000L, dbWeighted = 64f),
                    WeightedExposureMeasurement(timestamp = now, dbWeighted = 86f),
                )
            yearlySessionCount.value = 7

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertTrue(state.monthlyTrend is MonthlyTrendUiState.Data)
            val yearlyReport = state.yearlyReport as YearlyReportUiState.Data
            assertEquals(7, yearlyReport.totalSessions)
            assertEquals(86f, yearlyReport.loudestDb ?: 0f, 0.001f)
        }

    @Test
    fun freeUserReceivesLockedMonthlyAndYearlyPreviewsWithoutExposureData() =
        runAnalyticsTest {
            val now = System.currentTimeMillis()
            preferences.value = UserPreferences(isProUser = false)
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            monthlyMeasurements.value = listOf(WeightedExposureMeasurement(timestamp = now, dbWeighted = 64f))
            yearlyMeasurements.value = listOf(WeightedExposureMeasurement(timestamp = now, dbWeighted = 86f))
            yearlySessionCount.value = 7

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(MonthlyTrendUiState.LockedPreview, state.monthlyTrend)
            assertEquals(YearlyReportUiState.LockedPreview, state.yearlyReport)
        }

    @Test
    fun emptyProExposureAnalyticsReturnEmptyStatesWithoutPlaceholderMetrics() =
        runAnalyticsTest {
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            monthlyMeasurements.value = emptyList()
            yearlyMeasurements.value = emptyList()
            yearlySessionCount.value = 0

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(MonthlyTrendUiState.Empty, state.monthlyTrend)
            assertEquals(YearlyReportUiState.Empty, state.yearlyReport)
        }

    private fun createViewModel(): AnalyticsViewModel =
        stubAudioFlows().let {
            AnalyticsViewModel(
                measurementRepository = measurementRepository,
                sessionRepository = sessionRepository,
                preferencesRepository = preferencesRepository,
                audioSessionManager = audioSessionManager,
                audioEngine = audioEngine,
            ).also(createdViewModels::add)
        }

    private fun runAnalyticsTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            createdViewModels.forEach { it.clearForTest() }
            createdViewModels.clear()
            runCurrent()
        }
    }

    private fun stubAudioFlows() {
        every { audioSessionManager.isRecording } returns isRecording
        every { audioEngine.spectralFrame } returns spectralFrame
    }

    private fun seedEnvironmentMixInput(
        quietCount: Long,
        moderateCount: Long,
        loudCount: Long,
        criticalCount: Long,
        totalCount: Long,
    ) {
        dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
        environmentMixCounts.value =
            EnvironmentExposureMixCounts(
                quietCount = quietCount,
                moderateCount = moderateCount,
                loudCount = loudCount,
                criticalCount = criticalCount,
                totalCount = totalCount,
            )
    }

    private fun liveFrame(): SpectralFrame =
        SpectralFrame(
            bands =
                List(24) { index ->
                    SpectralBand(
                        startFrequencyHz = 20f + index,
                        endFrequencyHz = 40f + index,
                        centerFrequencyHz = 30f + index,
                        normalizedAmplitude = 0.5f,
                    )
                },
            dominantFrequencyHz = 1000f,
            bandwidth = SpectralBandwidth.NARROW,
            timestamp = 123L,
        )

    private fun LocalDate.toStartMs(zoneId: ZoneId): Long = atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
}
