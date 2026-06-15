package com.dbcheck.app.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.HourlyExposureAverage
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionHistoryPolicy
import com.dbcheck.app.domain.session.SessionHistoryQuery
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.ui.history.state.HistorySearchFilter
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
        private val searchQuery = MutableStateFlow("")
        private val selectedSearchFilter = MutableStateFlow(HistorySearchFilter.ALL)

        init {
            loadHistory()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun loadHistory() {
            viewModelScope.launch {
                val preferences = preferencesRepository.userPreferences
                val searchControls =
                    combine(showAllSessions, searchQuery, selectedSearchFilter) { showAll, query, filter ->
                        HistorySearchControls(
                            showAllSessions = showAll,
                            searchQuery = query,
                            selectedFilter = filter,
                        )
                    }
                val sessions =
                    combine(preferences, searchControls) { prefs, controls ->
                        prefs.isProUser to controls
                    }.flatMapLatest { (isProUser, controls) ->
                        val query = controls.toSessionHistoryQuery()
                        when {
                            isProUser && query.hasConstraints() -> sessionRepository.getFilteredSessions(query)
                            controls.showAllSessions -> sessionRepository.getAllCompletedSessions()
                            else -> sessionRepository.getRecentSessions(20)
                        }
                    }

                combine(
                    measurementRepository.getHourlyAveragesLast24H(),
                    sessions,
                    preferences,
                    searchControls,
                ) { hourlyAverages, sessions, prefs, controls ->
                    val isPro = prefs.isProUser
                    val query = controls.toSessionHistoryQuery()
                    val hasActiveSearch = isPro && query.hasConstraints()
                    createHistoryUiState(
                        hourlyAverages = hourlyAverages,
                        sessions = sessions,
                        isPro = isPro,
                        isShowingAll = controls.showAllSessions || hasActiveSearch,
                        searchQuery = if (isPro) controls.searchQuery else "",
                        selectedFilter =
                            if (isPro) {
                                controls.selectedFilter
                            } else {
                                HistorySearchFilter.ALL
                            },
                        hasActiveSearch = hasActiveSearch,
                        isSearchLocked = !isPro,
                    )
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

        fun updateSearchQuery(query: String) {
            searchQuery.value = query
        }

        fun selectSearchFilter(filter: HistorySearchFilter) {
            selectedSearchFilter.value = filter
        }

        fun clearHistorySearch() {
            searchQuery.value = ""
            selectedSearchFilter.value = HistorySearchFilter.ALL
        }

        fun saveSessionMetadata(sessionId: Long, name: String, emoji: String, tags: List<String>) {
            viewModelScope.launch {
                if (!preferencesRepository.userPreferences.first().isProUser) return@launch

                runCatching {
                    updateSessionMetadata(sessionId, name, emoji, tags)
                }.onSuccess {
                    updateSuccessState { it.copy(metadataErrorMessage = null) }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    updateSuccessState {
                        it.copy(
                            metadataErrorMessage =
                                error.toUserFacingMessage(
                                    context.getString(R.string.session_name_unable_to_update),
                                ),
                        )
                    }
                }
            }
        }

        private fun createHistoryUiState(
            hourlyAverages: List<HourlyExposureAverage>,
            sessions: List<Session>,
            isPro: Boolean,
            isShowingAll: Boolean,
            searchQuery: String,
            selectedFilter: HistorySearchFilter,
            hasActiveSearch: Boolean,
            isSearchLocked: Boolean,
        ): HistoryUiState {
            val visibleSessions = sessions.visibleToUser(isPro)
            if (visibleSessions.isEmpty() && hourlyAverages.isEmpty() && !hasActiveSearch) return HistoryUiState.Empty

            val nowMs = System.currentTimeMillis()
            return HistoryUiState.Success(
                last24HoursData = hourlyAverages.map { it.toUiState() },
                last24HoursAvg = energyAverage(hourlyAverages),
                last24HoursMax = hourlyAverages.maxOfOrNull { it.maxDb } ?: 0f,
                last24HoursTrend = context.getString(R.string.history_trend_stable),
                last24HoursWindowStartMs = nowMs - LAST_24_HOURS_MILLIS,
                last24HoursWindowEndMs = nowMs,
                recentSessions = visibleSessions,
                weeklyTrendPercent = 0,
                weeklyTrendLabel = context.getString(R.string.history_trend_similar_to_last_week),
                safeHours = safeHours(hourlyAverages),
                isProUser = isPro,
                isShowingAllSessions = isShowingAll,
                searchQuery = searchQuery,
                selectedSearchFilter = selectedFilter,
                hasActiveSearch = hasActiveSearch,
                isHistorySearchLocked = isSearchLocked,
            )
        }

        private suspend fun updateSessionMetadata(sessionId: Long, name: String, emoji: String, tags: List<String>) {
            sessionRepository.updateSessionMetadata(
                id = sessionId,
                name = SessionMetadata.normalizeName(name),
                emoji = SessionMetadata.normalizeEmoji(emoji),
                tags = SessionMetadata.normalizeTags(tags),
            )
        }

        private fun updateSuccessState(transform: (HistoryUiState.Success) -> HistoryUiState.Success) {
            _uiState.update { state ->
                (state as? HistoryUiState.Success)?.let(transform) ?: state
            }
        }
    }

private data class HistorySearchControls(
    val showAllSessions: Boolean,
    val searchQuery: String,
    val selectedFilter: HistorySearchFilter,
)

private fun HistorySearchControls.toSessionHistoryQuery(): SessionHistoryQuery {
    val normalizedQuery = searchQuery.trim().takeIf { it.isNotEmpty() }
    return when (selectedFilter) {
        HistorySearchFilter.ALL -> SessionHistoryQuery(nameOrTag = normalizedQuery)

        HistorySearchFilter.A_WEIGHTED ->
            SessionHistoryQuery(
                nameOrTag = normalizedQuery,
                frequencyWeighting = WeightingType.A.name,
            )

        HistorySearchFilter.LOUD ->
            SessionHistoryQuery(
                nameOrTag = normalizedQuery,
                minAvgDb = NoiseLevel.DANGEROUS.minDb,
            )

        HistorySearchFilter.WITH_LOCATION ->
            SessionHistoryQuery(
                nameOrTag = normalizedQuery,
                hasLocation = true,
            )
    }
}

private fun SessionHistoryQuery.hasConstraints(): Boolean = nameOrTag != null ||
        startTimeFrom != null ||
        startTimeTo != null ||
        minAvgDb != null ||
        maxAvgDb != null ||
        frequencyWeighting != null ||
        hasLocation != null

private fun List<Session>.visibleToUser(isPro: Boolean): List<Session> {
    if (isPro) return this
    val freeHistoryStart = SessionHistoryPolicy.freeHistoryStartMillis()
    return filter { it.startTime >= freeHistoryStart }
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
