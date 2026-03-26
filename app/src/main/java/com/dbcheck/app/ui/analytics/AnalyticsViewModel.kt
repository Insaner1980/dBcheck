package com.dbcheck.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.HealthStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            combine(
                measurementRepository.getDailyAveragesLast7Days(),
                preferencesRepository.userPreferences,
            ) { dailyAverages, prefs ->
                if (dailyAverages.isEmpty()) {
                    AnalyticsUiState.Empty
                } else {
                    val weeklyAvg = dailyAverages.map { it.avgDb }.average().toFloat()
                    val todayAvg = dailyAverages.lastOrNull()?.avgDb ?: 0f
                    val percentDiff = if (weeklyAvg > 0) {
                        ((todayAvg - weeklyAvg) / weeklyAvg * 100).toInt()
                    } else 0

                    val healthStatus = when {
                        weeklyAvg < 70 -> HealthStatus.SAFE
                        weeklyAvg < 85 -> HealthStatus.WARNING
                        else -> HealthStatus.DANGER
                    }

                    AnalyticsUiState.Success(
                        weeklyAverageDb = weeklyAvg,
                        dailyAverages = dailyAverages,
                        healthStatus = healthStatus,
                        todayVsWeekPercent = percentDiff,
                        isProUser = prefs.isProUser,
                    )
                }
            }.collect { _uiState.value = it }
        }
    }
}
