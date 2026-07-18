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
import com.dbcheck.app.domain.audio.RtaBand
import com.dbcheck.app.domain.audio.RtaFrame
import com.dbcheck.app.domain.audio.RtaResolution
import com.dbcheck.app.domain.audio.SoundDetection
import com.dbcheck.app.domain.audio.SoundDetectionError
import com.dbcheck.app.domain.audio.SoundDetectionState
import com.dbcheck.app.domain.audio.SpectralBand
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.domain.audio.SpectralFrame
import com.dbcheck.app.service.AudioEngine
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.RtaUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionChipUiState
import com.dbcheck.app.ui.analytics.state.SoundDetectionUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralMode
import com.dbcheck.app.ui.analytics.state.SpectrogramUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    private val liveEnvironmentMixCounts = MutableStateFlow(EnvironmentExposureMixCounts())
    private val soundDetectionState = MutableStateFlow(SoundDetectionState())
    private val spectralFrame = MutableStateFlow<SpectralFrame?>(null)
    private val rtaFrame = MutableStateFlow<RtaFrame?>(null)
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
    fun noDataAndNoRecordingShowsEmptyTrendsState() = runAnalyticsTest {
            assertEquals(AnalyticsUiState.Empty, createViewModel().uiState.value)
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
    fun successStateDefaultsToOverviewAnalyticsSection() = runAnalyticsTest {
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(
                listOf(
                    AnalyticsSection.OVERVIEW,
                    AnalyticsSection.SPECTRAL,
                    AnalyticsSection.ENVIRONMENT,
                ),
                enumValues<AnalyticsSection>().toList(),
            )
            assertEquals(
                listOf(AnalyticsOverviewRange.WEEKLY, AnalyticsOverviewRange.MONTHLY),
                enumValues<AnalyticsOverviewRange>().toList(),
            )
            assertEquals(AnalyticsSection.OVERVIEW, state.selectedSection)
            assertEquals(AnalyticsOverviewRange.WEEKLY, state.selectedOverviewRange)
            assertEquals(
                listOf(SpectralMode.BARS, SpectralMode.SPECTROGRAM, SpectralMode.RTA),
                enumValues<SpectralMode>().toList(),
            )
            assertEquals(SpectralMode.BARS, state.selectedSpectralMode)
        }

    @Test
    fun selectedAnalyticsSectionPersistsWhenAnalyticsStateRebuilds() = runAnalyticsTest {
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            val viewModel = createViewModel()

            viewModel.onSectionSelected(AnalyticsSection.SPECTRAL)
            runCurrent()
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 2L, avgDb = 70f, maxDb = 92f))
            runCurrent()

            val state = viewModel.uiState.value as AnalyticsUiState.Success
            assertEquals(AnalyticsSection.SPECTRAL, state.selectedSection)
            assertEquals(70f, state.weeklyAverageDb, 0.001f)
        }

    @Test
    fun selectedOverviewRangePersistsWhenAnalyticsStateRebuilds() = runAnalyticsTest {
        dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
        val viewModel = createViewModel()

        viewModel.onOverviewRangeSelected(AnalyticsOverviewRange.MONTHLY)
        runCurrent()
        dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 2L, avgDb = 70f, maxDb = 92f))
        runCurrent()

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertEquals(AnalyticsOverviewRange.MONTHLY, state.selectedOverviewRange)
        assertEquals(70f, state.weeklyAverageDb, 0.001f)
    }

    @Test
    fun selectedSpectralModePersistsWhenAnalyticsStateRebuilds() = runAnalyticsTest {
        dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
        val viewModel = createViewModel()

        viewModel.onSpectralModeSelected(SpectralMode.RTA)
        runCurrent()
        dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 2L, avgDb = 70f, maxDb = 92f))
        runCurrent()

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertEquals(SpectralMode.RTA, state.selectedSpectralMode)
        assertEquals(70f, state.weeklyAverageDb, 0.001f)
    }

    @Test
    fun noDataWhileRecordingShowsSuccessSoLiveSpectrumCanRender() = runAnalyticsTest {
            isRecording.value = true

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertFalse(state.hasExposureData)
            assertTrue(state.isRecording)
            assertNull(state.hearingHealthSummary)
        }

    @Test
    fun proUserReceivesLiveSpectralFrame() = runAnalyticsTest {
            isRecording.value = true
            spectralFrame.value = liveFrame()

            val state = createViewModel().uiState.value as AnalyticsUiState.Success
            val spectralState = state.spectralAnalysis as SpectralAnalysisUiState.Live

            assertEquals(1000f, spectralState.dominantFrequencyHz, 0f)
            assertEquals(SpectralBandwidth.NARROW, spectralState.bandwidth)
            assertEquals(24, spectralState.bands.size)
            assertEquals(30f, spectralState.bands.first().centerFrequencyHz, 0f)
        }

    @Test
    fun proUserReceivesSpectrogramRowsFromLiveSpectralFrames() = runAnalyticsTest {
        isRecording.value = true
        spectralFrame.value = liveFrame(timestamp = 1L, normalizedAmplitude = 0.25f)
        val viewModel = createViewModel()

        spectralFrame.value = liveFrame(timestamp = 2L, normalizedAmplitude = 0.75f)
        runCurrent()

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        val spectrogram = state.spectrogram as SpectrogramUiState.Data
        assertEquals(listOf(1L, 2L), spectrogram.rows.map { it.timestampMs })
        assertEquals(24, spectrogram.rows.last().bands.size)
        assertEquals(0.75f, spectrogram.rows.last().bands.first().normalizedAmplitude, 0.001f)
    }

    @Test
    fun proUserReceivesRtaBandsFromLiveRtaFrame() = runAnalyticsTest {
        isRecording.value = true
        rtaFrame.value = liveRtaFrame()

        val state = createViewModel().uiState.value as AnalyticsUiState.Success
        val rta = state.rta as RtaUiState.Data

        assertEquals(10, rta.bands.size)
        assertEquals(31.62f, rta.bands.first().centerFrequencyHz, 0.01f)
        assertEquals(1f, rta.bands.last().normalizedAmplitude, 0.001f)
    }

    @Test
    fun freeUserDoesNotReceiveLiveSpectralFrame() = runAnalyticsTest {
            preferences.value = UserPreferences(isProUser = false)
            isRecording.value = true
            spectralFrame.value = liveFrame()

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(SpectralAnalysisUiState.LockedPreview, state.spectralAnalysis)
            assertEquals(SpectrogramUiState.LockedPreview, state.spectrogram)
            assertEquals(RtaUiState.LockedPreview, state.rta)
        }

    @Test
    fun proUserReceivesEnvironmentMixPercentagesFromSevenDayCounts() = runAnalyticsTest {
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
    fun freeUserDoesNotReceiveEnvironmentMixCounts() = runAnalyticsTest {
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
    fun emptyEnvironmentMixCountsReturnEmptyState() = runAnalyticsTest {
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

        assertEquals(0, requireNotNull(state.hearingHealthSummary).todayVsWeekPercent)
    }

    @Test
    fun environmentMixPercentagesSumToOneHundredAfterRounding() = runAnalyticsTest {
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
    fun proUserReceivesActiveEnvironmentMixWhileRecording() = runAnalyticsTest {
        isRecording.value = true
        liveEnvironmentMixCounts.value =
            EnvironmentExposureMixCounts(
                quietCount = 1,
                moderateCount = 1,
                loudCount = 1,
                criticalCount = 0,
                totalCount = 3,
            )

        val state = createViewModel().uiState.value as AnalyticsUiState.Success
        val activeMix = state.activeEnvironmentMix as EnvironmentMixUiState.Data

        assertEquals(
            listOf(
                EnvironmentMixCategory.QUIET to 34,
                EnvironmentMixCategory.MODERATE to 33,
                EnvironmentMixCategory.LOUD to 33,
                EnvironmentMixCategory.CRITICAL to 0,
            ),
            activeMix.rows.map { it.category to it.percent },
        )
    }

    @Test
    fun freeUserDoesNotReceiveActiveEnvironmentMixWhileRecording() = runAnalyticsTest {
        preferences.value = UserPreferences(isProUser = false)
        isRecording.value = true
        liveEnvironmentMixCounts.value =
            EnvironmentExposureMixCounts(
                quietCount = 1,
                moderateCount = 1,
                loudCount = 1,
                criticalCount = 0,
                totalCount = 3,
            )

        val state = createViewModel().uiState.value as AnalyticsUiState.Success

        assertEquals(EnvironmentMixUiState.LockedPreview, state.activeEnvironmentMix)
    }

    @Test
    fun freeUserReceivesLockedSoundDetectionPreview() = runAnalyticsTest {
        preferences.value = UserPreferences(isProUser = false)
        isRecording.value = true
        soundDetectionState.value =
            SoundDetectionState(
                isEnabled = true,
                current = SoundDetection(label = "Speech", confidence = 0.82f, timestamp = 123L),
            )

        val state = createViewModel().uiState.value as AnalyticsUiState.Success

        assertEquals(SoundDetectionUiState.LockedPreview, state.soundDetection)
    }

    @Test
    fun proUserReceivesIdleSoundDetectionWhenNoCurrentTypeExists() = runAnalyticsTest {
        isRecording.value = true
        soundDetectionState.value = SoundDetectionState(isEnabled = true)

        val state = createViewModel().uiState.value as AnalyticsUiState.Success

        assertEquals(SoundDetectionUiState.Idle, state.soundDetection)
    }

    @Test
    fun proUserReceivesLiveSoundDetectionFromSessionManager() = runAnalyticsTest {
        preferences.value = UserPreferences(isProUser = true, soundDetectionEnabled = true)
        isRecording.value = true
        soundDetectionState.value =
            SoundDetectionState(
                isEnabled = true,
                current = SoundDetection(label = "Speech", confidence = 0.824f, timestamp = 200L),
                recentDetections =
                    listOf(
                        SoundDetection(label = "Speech", confidence = 0.824f, timestamp = 200L),
                        SoundDetection(label = "Music", confidence = 0.61f, timestamp = 100L),
                    ),
            )

        val state = createViewModel().uiState.value as AnalyticsUiState.Success
        val soundDetection = state.soundDetection as SoundDetectionUiState.Live

        assertEquals("Speech", soundDetection.label)
        assertEquals(82, soundDetection.confidencePercent)
        assertEquals(
            listOf(
                SoundDetectionChipUiState(label = "Speech", confidencePercent = 82),
                SoundDetectionChipUiState(label = "Music", confidencePercent = 61),
            ),
            soundDetection.recentDetections,
        )
    }

    @Test
    fun disabledSoundDetectionToggleSuppressesLiveSoundDetection() = runAnalyticsTest {
        preferences.value = UserPreferences(isProUser = true, soundDetectionEnabled = false)
        isRecording.value = true
        soundDetectionState.value =
            SoundDetectionState(
                isEnabled = true,
                current = SoundDetection(label = "Speech", confidence = 0.824f, timestamp = 200L),
            )

        val state = createViewModel().uiState.value as AnalyticsUiState.Success

        assertEquals(SoundDetectionUiState.Idle, state.soundDetection)
    }

    @Test
    fun proUserReceivesSoundDetectionErrorState() = runAnalyticsTest {
        preferences.value = UserPreferences(isProUser = true, soundDetectionEnabled = true)
        isRecording.value = true
        soundDetectionState.value =
            SoundDetectionState(
                isEnabled = true,
                error = SoundDetectionError.CLASSIFICATION_UNAVAILABLE,
            )

        val state = createViewModel().uiState.value as AnalyticsUiState.Success
        val soundDetection = state.soundDetection as SoundDetectionUiState.Error

        assertEquals("Sound detection unavailable", soundDetection.message)
    }

    @Test
    fun proUserReceivesMonthlyTrendAndYearlyReportFromExposureMeasurements() = runAnalyticsTest {
            seedProExposureAnalyticsData()

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertTrue(state.monthlyTrend is MonthlyTrendUiState.Data)
            val yearlyReport = state.yearlyReport as YearlyReportUiState.Data
            assertEquals(7, yearlyReport.totalSessions)
            assertEquals(86f, yearlyReport.loudestDb ?: 0f, 0.001f)
        }

    @Test
    fun liveSpectralFramesDoNotRebuildHistoricalExposureUiState() = runAnalyticsTest {
        seedProExposureAnalyticsData()
        isRecording.value = true
        val viewModel = createViewModel()
        val initialState = viewModel.uiState.value as AnalyticsUiState.Success

        spectralFrame.value = liveFrame(timestamp = 2L)
        runCurrent()

        val updatedState = viewModel.uiState.value as AnalyticsUiState.Success
        assertSame(initialState.monthlyTrend, updatedState.monthlyTrend)
        assertSame(initialState.yearlyReport, updatedState.yearlyReport)
        assertTrue(updatedState.spectralAnalysis is SpectralAnalysisUiState.Live)
    }

    @Test
    fun freeUserReceivesLockedMonthlyAndYearlyPreviewsWithoutExposureData() = runAnalyticsTest {
            val now = System.currentTimeMillis()
            preferences.value = UserPreferences(isProUser = false)
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            monthlyMeasurements.value = listOf(WeightedExposureMeasurement(timestamp = now, dbWeighted = 64f))
            yearlyMeasurements.value = listOf(WeightedExposureMeasurement(timestamp = now, dbWeighted = 86f))
            yearlySessionCount.value = 7

            val viewModel = createViewModel()
            viewModel.onOverviewRangeSelected(AnalyticsOverviewRange.MONTHLY)
            runCurrent()

            val state = viewModel.uiState.value as AnalyticsUiState.Success
            assertEquals(AnalyticsOverviewRange.MONTHLY, state.selectedOverviewRange)
            assertEquals(MonthlyTrendUiState.LockedPreview, state.monthlyTrend)
            assertEquals(YearlyReportUiState.LockedPreview, state.yearlyReport)
            verify(exactly = 0) { measurementRepository.getWeightedMeasurementsInRange(any(), any()) }
            verify(exactly = 0) { sessionRepository.getCompletedSessionCountInRange(any(), any()) }
        }

    @Test
    fun emptyProExposureAnalyticsReturnEmptyStatesWithoutPlaceholderMetrics() = runAnalyticsTest {
            dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
            monthlyMeasurements.value = emptyList()
            yearlyMeasurements.value = emptyList()
            yearlySessionCount.value = 0

            val state = createViewModel().uiState.value as AnalyticsUiState.Success

            assertEquals(MonthlyTrendUiState.Empty, state.monthlyTrend)
            assertEquals(YearlyReportUiState.Empty, state.yearlyReport)
        }

    private fun createViewModel(): AnalyticsViewModel = stubAudioFlows().let {
            AnalyticsViewModel(
                context = testStringContext(),
                measurementRepository = measurementRepository,
                sessionRepository = sessionRepository,
                preferencesRepository = preferencesRepository,
                audioSessionManager = audioSessionManager,
                audioEngine = audioEngine,
                defaultDispatcher = Dispatchers.Main,
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

    private fun seedProExposureAnalyticsData(now: Long = System.currentTimeMillis()) {
        dailyAverages.value = listOf(DailyExposureAverage(dayStartMs = 1L, avgDb = 64f, maxDb = 91f))
        monthlyMeasurements.value = weightedExposureMeasurements(now = now, latestDbWeighted = 74f)
        yearlyMeasurements.value = weightedExposureMeasurements(now = now, latestDbWeighted = 86f)
        yearlySessionCount.value = 7
    }

    private fun weightedExposureMeasurements(now: Long, latestDbWeighted: Float): List<WeightedExposureMeasurement> =
        listOf(
            WeightedExposureMeasurement(timestamp = now - 1_000L, dbWeighted = 64f),
            WeightedExposureMeasurement(timestamp = now, dbWeighted = latestDbWeighted),
        )

    private fun stubAudioFlows() {
        every { audioSessionManager.isRecording } returns isRecording
        every { audioSessionManager.liveEnvironmentMixCounts } returns liveEnvironmentMixCounts
        every { audioSessionManager.soundDetectionState } returns soundDetectionState
        every { audioEngine.spectralFrame } returns spectralFrame
        every { audioEngine.rtaFrame } returns rtaFrame
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

    private fun liveFrame(timestamp: Long = 123L, normalizedAmplitude: Float = 0.5f): SpectralFrame = SpectralFrame(
            bands =
                List(24) { index ->
                    SpectralBand(
                        startFrequencyHz = 20f + index,
                        endFrequencyHz = 40f + index,
                        centerFrequencyHz = 30f + index,
                        normalizedAmplitude = normalizedAmplitude,
                    )
                },
            dominantFrequencyHz = 1000f,
            bandwidth = SpectralBandwidth.NARROW,
            timestamp = timestamp,
        )

    private fun liveRtaFrame(): RtaFrame = RtaFrame(
            bands =
                List(10) { index ->
                    RtaBand(
                        lowerEdgeFrequencyHz = 20f + index,
                        centerFrequencyHz =
                            if (index == 0) {
                                31.62f
                            } else {
                                31.62f * (index + 1)
                            },
                        upperEdgeFrequencyHz = 40f + index,
                        normalizedAmplitude = (index + 1) / 10f,
                    )
                },
            resolution = RtaResolution.OCTAVE,
            timestamp = 123L,
        )

    private fun LocalDate.toStartMs(zoneId: ZoneId): Long = atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
}
