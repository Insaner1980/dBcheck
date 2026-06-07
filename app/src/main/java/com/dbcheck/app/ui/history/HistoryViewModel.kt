package com.dbcheck.app.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.HourlyExposureAverage
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.session.SessionHistoryPolicy
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.ui.history.state.HistoryUiState
import com.dbcheck.app.ui.history.state.HourlyExposureUiState
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sessionRepository: SessionRepository,
        private val measurementRepository: MeasurementRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
        val uiState: StateFlow<HistoryUiState> = _uiState
        private val showAllSessions = MutableStateFlow(false)

        init {
            loadHistory()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun loadHistory() {
            viewModelScope.launch {
                combine(
                    measurementRepository.getHourlyAveragesLast24H(),
                    showAllSessions.flatMapLatest { showAll ->
                        if (showAll) {
                            sessionRepository.getAllCompletedSessions()
                        } else {
                            sessionRepository.getRecentSessions(20)
                        }
                    },
                    preferencesRepository.userPreferences,
                    showAllSessions,
                ) { hourlyAverages, sessions, prefs, isShowingAll ->
                    val isPro = prefs.isProUser
                    val filteredSessions =
                        if (isPro) {
                            sessions
                        } else {
                            val freeHistoryStart = SessionHistoryPolicy.freeHistoryStartMillis()
                            sessions.filter { it.startTime >= freeHistoryStart }
                        }

                    if (filteredSessions.isEmpty() && hourlyAverages.isEmpty()) {
                        HistoryUiState.Empty
                    } else {
                        val nowMs = System.currentTimeMillis()
                        val avg24h = energyAverage(hourlyAverages)
                        val max24h = hourlyAverages.maxOfOrNull { it.maxDb } ?: 0f
                        val safeHoursValue = safeHours(hourlyAverages)

                        HistoryUiState.Success(
                            last24HoursData = hourlyAverages.map { it.toUiState() },
                            last24HoursAvg = avg24h,
                            last24HoursMax = max24h,
                            last24HoursTrend = context.getString(R.string.history_trend_stable),
                            last24HoursWindowStartMs = nowMs - LAST_24_HOURS_MILLIS,
                            last24HoursWindowEndMs = nowMs,
                            recentSessions = filteredSessions,
                            weeklyTrendPercent = 0,
                            weeklyTrendLabel = context.getString(R.string.history_trend_similar_to_last_week),
                            safeHours = safeHoursValue,
                            isProUser = isPro,
                            isShowingAllSessions = isShowingAll,
                        )
                    }
                }.catch { error ->
                    if (error is CancellationException) throw error
                    _uiState.value =
                        HistoryUiState.Error(
                            error.toUserFacingMessage(context.getString(R.string.history_error_unable_to_load)),
                        )
                }.collect { _uiState.value = it }
            }
        }

        fun showAllSessions() {
            showAllSessions.value = true
        }

        fun saveSessionMetadata(sessionId: Long, name: String, emoji: String, tags: List<String>) {
            viewModelScope.launch {
                runCatching {
                    if (!preferencesRepository.userPreferences.first().isProUser) return@launch

                    sessionRepository.updateSessionMetadata(
                        id = sessionId,
                        name = SessionMetadata.normalizeName(name),
                        emoji = SessionMetadata.normalizeEmoji(emoji),
                        tags = SessionMetadata.normalizeTags(tags),
                    )
                }.onSuccess {
                    _uiState.update { state ->
                        if (state is HistoryUiState.Success) {
                            state.copy(metadataErrorMessage = null)
                        } else {
                            state
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update { state ->
                        if (state is HistoryUiState.Success) {
                            state.copy(
                                metadataErrorMessage =
                                    error.toUserFacingMessage(
                                        context.getString(R.string.session_name_unable_to_update),
                                    ),
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

private fun HourlyExposureAverage.toUiState(): HourlyExposureUiState = HourlyExposureUiState(
        hour = hour,
        avgDb = avgDb,
        maxDb = maxDb,
        hourStartMs = hourStartMs,
    )

private fun energyAverage(averages: List<HourlyExposureAverage>): Float {
    val totalCount = averages.sumOf { it.sampleCount }
    if (totalCount <= 0) return 0f

    val totalEnergy =
        averages.sumOf { average ->
            DecibelMath.energyFromDb(average.avgDb) * average.sampleCount
        }
    return DecibelMath.energyAverageDb(totalEnergy, totalCount) ?: 0f
}

private fun safeHours(averages: List<HourlyExposureAverage>): Float {
    val safeDurationMs =
        averages
            .filter { it.avgDb < NoiseLevel.ELEVATED.maxDb }
            .sumOf { it.durationMs }
    return safeDurationMs.toFloat() / HOUR_MILLIS
}

private const val HOUR_MILLIS = 60L * 60L * 1_000L
private const val LAST_24_HOURS_MILLIS = 24L * HOUR_MILLIS
