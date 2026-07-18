package com.dbcheck.app.ui.analytics

import com.dbcheck.app.data.local.db.dao.WeightedMeasurementPoint
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.EnvironmentExposureMixCounts
import com.dbcheck.app.domain.audio.RtaFrame
import com.dbcheck.app.domain.audio.SoundDetectionState
import com.dbcheck.app.domain.audio.SpectralFrame
import com.dbcheck.app.service.AudioEngine
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.test.EmptyMeasurementDao
import com.dbcheck.app.testStringContext
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal class AnalyticsViewModelTestFixture(
    private val defaultDispatcher: CoroutineDispatcher,
    val preferences: MutableStateFlow<UserPreferences> = MutableStateFlow(UserPreferences(isProUser = true)),
) {
    val isRecordingFlow = MutableStateFlow(false)
    val liveEnvironmentMixCountsFlow = MutableStateFlow(EnvironmentExposureMixCounts())
    val soundDetectionStateFlow = MutableStateFlow(SoundDetectionState())
    val spectralFrameFlow = MutableStateFlow<SpectralFrame?>(null)
    val rtaFrameFlow = MutableStateFlow<RtaFrame?>(null)
    val measurementDao = AnalyticsMeasurementDao()
    val measurementRepository = MeasurementRepository(measurementDao, defaultDispatcher)
    val sessionRepository =
        mockk<SessionRepository> {
            every { getCompletedSessionCountInRange(any(), any()) } answers { flowOf(0) }
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns isRecordingFlow
            every { liveEnvironmentMixCounts } returns liveEnvironmentMixCountsFlow
            every { soundDetectionState } returns soundDetectionStateFlow
        }
    private val audioEngine =
        mockk<AudioEngine> {
            every { spectralFrame } returns spectralFrameFlow
            every { rtaFrame } returns rtaFrameFlow
        }
    fun createViewModel(): AnalyticsViewModel = AnalyticsViewModel(
        context = testStringContext(),
        measurementRepository = measurementRepository,
        sessionRepository = sessionRepository,
        preferencesRepository = preferencesRepository,
        audioSessionManager = audioSessionManager,
        audioEngine = audioEngine,
        defaultDispatcher = defaultDispatcher,
    )
}

internal class AnalyticsMeasurementDao : EmptyMeasurementDao() {
    val weightedRangeCalls = mutableListOf<Pair<Long, Long>>()
    var measurementRangeFailure: Throwable? = null

    override fun getMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<MeasurementEntity>> =
        measurementRangeFailure?.let { failure ->
            flow { throw failure }
        } ?: flowOf(emptyList())

    override fun getWeightedMeasurementsInRange(startTime: Long, endTime: Long): Flow<List<WeightedMeasurementPoint>> {
        weightedRangeCalls += startTime to endTime
        return flowOf(emptyList())
    }
}
