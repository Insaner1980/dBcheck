package com.dbcheck.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.ui.history.state.HistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

        init {
            loadHistory()
        }

        private fun loadHistory() {
            viewModelScope.launch {
                combine(
                    measurementRepository.getHourlyAveragesLast24H(),
                    sessionRepository.getRecentSessions(20),
                    preferencesRepository.userPreferences,
                ) { hourlyAverages, sessions, prefs ->
                    if (sessions.isEmpty() && hourlyAverages.isEmpty()) {
                        HistoryUiState.Empty
                    } else {
                        val avg24h = hourlyAverages.map { it.avgDb }.average().toFloat()
                        val peak24h = hourlyAverages.maxOfOrNull { it.maxDb } ?: 0f
                        val safeHours = hourlyAverages.count { it.avgDb < 85f }.toFloat()
                        val isPro = prefs.isProUser

                        // Free users: limit to last 7 days
                        val filteredSessions =
                            if (isPro) {
                                sessions
                            } else {
                                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                                sessions.filter { it.startTime >= sevenDaysAgo }
                            }

                        HistoryUiState.Success(
                            last24HoursData = hourlyAverages,
                            last24HoursAvg = avg24h,
                            last24HoursPeak = peak24h,
                            last24HoursTrend = "Stable",
                            recentSessions = filteredSessions,
                            weeklyTrendPercent = 0,
                            weeklyTrendLabel = "Similar to last week",
                            safeHours = safeHours,
                            isProUser = isPro,
                        )
                    }
                }.collect { _uiState.value = it }
            }
        }
    }
