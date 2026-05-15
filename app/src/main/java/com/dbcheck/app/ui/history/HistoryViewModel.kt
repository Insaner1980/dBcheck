package com.dbcheck.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
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
                        val avg24h = energyAverage(hourlyAverages)
                        val peak24h = hourlyAverages.maxOfOrNull { it.maxDb } ?: 0f
                        val safeHours = hourlyAverages.count { it.avgDb < NoiseLevel.ELEVATED.maxDb }.toFloat()

                        HistoryUiState.Success(
                            last24HoursData = hourlyAverages.map { it.toUiState() },
                            last24HoursAvg = avg24h,
                            last24HoursPeak = peak24h,
                            last24HoursTrend = "Stable",
                            recentSessions = filteredSessions,
                            weeklyTrendPercent = 0,
                            weeklyTrendLabel = "Similar to last week",
                            safeHours = safeHours,
                            isProUser = isPro,
                            isShowingAllSessions = isShowingAll,
                        )
                    }
                }.collect { _uiState.value = it }
            }
        }

        fun showAllSessions() {
            showAllSessions.value = true
        }

        fun saveSessionMetadata(sessionId: Long, name: String, emoji: String, tags: List<String>) {
            viewModelScope.launch {
                if (!preferencesRepository.userPreferences.first().isProUser) return@launch

                sessionRepository.updateSessionMetadata(
                    id = sessionId,
                    name = SessionMetadata.normalizeName(name),
                    emoji = SessionMetadata.normalizeEmoji(emoji),
                    tags = SessionMetadata.normalizeTags(tags),
                )
            }
        }
    }

private fun HourlyExposureAverage.toUiState(): HourlyExposureUiState = HourlyExposureUiState(
        hour = hour,
        avgDb = avgDb,
        maxDb = maxDb,
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
