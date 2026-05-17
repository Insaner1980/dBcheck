package com.dbcheck.app.ui.history.detail

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.report.ReportHeartRateSample
import com.dbcheck.app.domain.report.ReportHeartRateSection
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.report.SessionReportCalculator
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionHistoryPolicy
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.HealthConnectServiceAvailability
import com.dbcheck.app.service.HeartRateServiceSample
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.util.ExportPdfReportUseCase
import com.dbcheck.app.util.ShareResultsGenerator
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val sessionRepository: SessionRepository,
        private val measurementRepository: MeasurementRepository,
        private val preferencesRepository: PreferencesRepository,
        private val exportPdfReportUseCase: ExportPdfReportUseCase,
        private val shareResultsGenerator: ShareResultsGenerator,
        private val healthConnectService: HealthConnectService,
    ) : ViewModel() {
        private val sessionId =
            savedStateHandle.get<Long>(Screen.SessionDetail.ARG_SESSION_ID)
                ?: savedStateHandle.get<String>(Screen.SessionDetail.ARG_SESSION_ID)?.toLongOrNull()
                ?: -1L

        private val _uiState = MutableStateFlow(SessionDetailUiState())
        val uiState: StateFlow<SessionDetailUiState> = _uiState
        private val _sharePngIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val sharePngIntents: SharedFlow<Intent> = _sharePngIntents.asSharedFlow()

        init {
            loadSession()
        }

        fun exportPdf(uri: Uri) {
            val state = _uiState.value
            val report = state.report ?: return
            if (!state.isProUser) {
                _uiState.update { it.copy(errorMessage = "PDF export requires dBcheck Pro") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isExporting = true, message = null, errorMessage = null) }
                runCatching { exportPdfReportUseCase.export(report, uri, state.toReportHeartRateSection()) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(isExporting = false, message = "PDF report exported")
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                errorMessage = error.toUserFacingMessage("PDF export failed"),
                            )
                        }
                    }
            }
        }

        fun createSharePngIntent() {
            val report = _uiState.value.report ?: return
            viewModelScope.launch {
                runCatching {
                    shareResultsGenerator.shareSessionReportCard(report)
                }.onSuccess { intent ->
                    _uiState.update { it.copy(errorMessage = null) }
                    _sharePngIntents.emit(intent)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.toUserFacingMessage("Unable to share session"))
                    }
                }
            }
        }

        fun onSharePngUnavailable() {
            _uiState.update { it.copy(errorMessage = "No app available to share session") }
        }

        fun suggestedPdfName(): String {
            val report = _uiState.value.report
            val timestamp =
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
                    .format(Date(report?.startTime ?: System.currentTimeMillis()))
            val name = SessionMetadata.slugify(report?.sessionName)
            return "dbcheck-$name-$timestamp.pdf"
        }

        fun saveSessionMetadata(
            name: String,
            emoji: String,
            tags: List<String>,
        ) {
            if (!_uiState.value.isProUser) {
                _uiState.update { it.copy(errorMessage = "Session naming requires dBcheck Pro") }
                return
            }

            viewModelScope.launch {
                runCatching {
                    sessionRepository.updateSessionMetadata(
                        id = sessionId,
                        name = SessionMetadata.normalizeName(name),
                        emoji = SessionMetadata.normalizeEmoji(emoji),
                        tags = SessionMetadata.normalizeTags(tags),
                    )
                }.onSuccess {
                    _uiState.update { it.copy(message = "Session updated", errorMessage = null) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.toUserFacingMessage("Unable to update session"))
                    }
                }
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(message = null, errorMessage = null) }
        }

        private fun loadSession() {
            viewModelScope.launch {
                combine(
                    sessionRepository.getSessionById(sessionId),
                    measurementRepository.getMeasurementsForSession(sessionId),
                    preferencesRepository.userPreferences,
                ) { session, measurements, prefs ->
                    val reportMeasurements =
                        measurements.map { measurement ->
                            ReportMeasurement(
                                timestamp = measurement.timestamp,
                                dbWeighted = measurement.dbWeighted,
                                peakDb = measurement.peakDb,
                            )
                        }
                    buildLoadResult(session, reportMeasurements, prefs)
                }.collect { result ->
                    _uiState.update { it.withLoadResult(result) }
                }
            }
        }

        private suspend fun buildLoadResult(
            session: Session?,
            measurements: List<ReportMeasurement>,
            prefs: UserPreferences,
        ): SessionDetailLoadResult {
            val historyLocked = session != null && !session.canBeOpenedBy(prefs.isProUser)
            val report = session.takeUnless { historyLocked }?.toReport(measurements)
            val heartRate = loadHeartRateState(report, prefs)

            return SessionDetailLoadResult(
                report = report,
                isProUser = prefs.isProUser,
                unavailableReason = unavailableReason(
                    historyLocked = historyLocked,
                    isMissing = session == null,
                ),
                heartRateOverlayEnabled = heartRate.enabled,
                heartRateSamples = heartRate.samples,
                heartRateUnavailableMessage = heartRate.unavailableMessage,
                errorMessage = loadErrorMessage(
                    historyLocked = historyLocked,
                    isMissing = session == null,
                ),
            )
        }

        private suspend fun loadHeartRateState(
            report: SessionReportData?,
            prefs: UserPreferences,
        ): HeartRateLoadResult = when {
            report == null || !prefs.isProUser || !prefs.heartRateOverlayEnabled -> HeartRateLoadResult()

            else -> {
                val status = healthConnectService.getStatus()
                when {
                    status.availability != HealthConnectServiceAvailability.AVAILABLE ->
                        HeartRateLoadResult(
                            unavailableMessage = "Health Connect is unavailable on this device",
                        )

                    !status.heartRateReadGranted ->
                        HeartRateLoadResult(
                            unavailableMessage =
                                "Health Connect heart rate permission is required to show this overlay",
                        )

                    else ->
                        HeartRateLoadResult(
                            enabled = true,
                            samples = readHeartRateSamples(report),
                        )
                }
            }
        }

        private suspend fun readHeartRateSamples(report: SessionReportData): List<HeartRateSampleUiState> =
            healthConnectService.readHeartRateForSession(
                start = Instant.ofEpochMilli(report.startTime),
                end = Instant.ofEpochMilli(report.endTime),
            ).map { it.toUiState() }
    }

private data class SessionDetailLoadResult(
    val report: SessionReportData?,
    val isProUser: Boolean,
    val unavailableReason: SessionDetailUnavailableReason?,
    val heartRateOverlayEnabled: Boolean,
    val heartRateSamples: List<HeartRateSampleUiState>,
    val heartRateUnavailableMessage: String?,
    val errorMessage: String?,
)

private data class HeartRateLoadResult(
    val enabled: Boolean = false,
    val samples: List<HeartRateSampleUiState> = emptyList(),
    val unavailableMessage: String? = null,
)

private fun SessionDetailUiState.withLoadResult(result: SessionDetailLoadResult): SessionDetailUiState =
    copy(
        isLoading = false,
        report = result.report,
        unavailableReason = result.unavailableReason,
        isProUser = result.isProUser,
        heartRateOverlayEnabled = result.heartRateOverlayEnabled,
        heartRateSamples = result.heartRateSamples,
        heartRateUnavailableMessage = result.heartRateUnavailableMessage,
        errorMessage = nextErrorMessage(result),
    )

private fun SessionDetailUiState.nextErrorMessage(result: SessionDetailLoadResult): String? = when {
    result.errorMessage != null -> result.errorMessage
    unavailableReason != null -> null
    else -> errorMessage
}

private fun unavailableReason(historyLocked: Boolean, isMissing: Boolean): SessionDetailUnavailableReason? = when {
    historyLocked -> SessionDetailUnavailableReason.HISTORY_LOCKED
    isMissing -> SessionDetailUnavailableReason.SESSION_NOT_FOUND
    else -> null
}

private fun loadErrorMessage(historyLocked: Boolean, isMissing: Boolean): String? = when {
    historyLocked -> "Unlimited history requires dBcheck Pro"
    isMissing -> "Session not found"
    else -> null
}

private fun Session.canBeOpenedBy(isProUser: Boolean): Boolean =
    SessionHistoryPolicy.canAccessSession(startTime, isProUser)

private fun SessionDetailUiState.toReportHeartRateSection(): ReportHeartRateSection =
    if (isProUser && heartRateOverlayEnabled) {
        ReportHeartRateSection(
            enabled = true,
            samples = heartRateSamples.map { it.toReportHeartRateSample() },
        )
    } else {
        ReportHeartRateSection()
    }

private fun HeartRateSampleUiState.toReportHeartRateSample(): ReportHeartRateSample = ReportHeartRateSample(
        timestamp = time.toEpochMilli(),
        beatsPerMinute = beatsPerMinute,
    )

private fun Session.toReport(measurements: List<ReportMeasurement>): SessionReportData = SessionReportCalculator.build(
        session = this,
        measurements = measurements,
    )

private fun HeartRateServiceSample.toUiState(): HeartRateSampleUiState =
    HeartRateSampleUiState(
        time = time,
        beatsPerMinute = beatsPerMinute,
    )
