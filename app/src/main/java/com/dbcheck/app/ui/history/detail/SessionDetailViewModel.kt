package com.dbcheck.app.ui.history.detail

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.report.SessionReportCalculator
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
            val report = _uiState.value.report ?: return
            if (!_uiState.value.isProUser) {
                _uiState.update { it.copy(errorMessage = "PDF export requires dBcheck Pro") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isExporting = true, message = null, errorMessage = null) }
                runCatching { exportPdfReportUseCase.export(report, uri) }
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
                    val historyLocked = session != null && !session.canBeOpenedBy(prefs.isProUser)
                    val accessibleSession = session.takeUnless { historyLocked }
                    val report =
                        accessibleSession?.let {
                            SessionReportCalculator.build(
                                session = it,
                                measurements =
                                    measurements.map { measurement ->
                                        ReportMeasurement(
                                            timestamp = measurement.timestamp,
                                            dbWeighted = measurement.dbWeighted,
                                        )
                                    },
                            )
                        }
                    val healthConnectStatus =
                        if (report != null && prefs.isProUser && prefs.heartRateOverlayEnabled) {
                            healthConnectService.getStatus()
                        } else {
                            null
                        }
                    val canReadHeartRate =
                        healthConnectStatus?.availability == HealthConnectServiceAvailability.AVAILABLE &&
                            healthConnectStatus.heartRateReadGranted
                    val heartRateSamples =
                        if (report != null && canReadHeartRate) {
                            healthConnectService.readHeartRateForSession(
                                start = Instant.ofEpochMilli(report.startTime),
                                end = Instant.ofEpochMilli(report.endTime),
                            ).map { it.toUiState() }
                        } else {
                            emptyList()
                        }

                    SessionDetailLoadResult(
                        report = report,
                        isProUser = prefs.isProUser,
                        heartRateOverlayEnabled = prefs.heartRateOverlayEnabled && canReadHeartRate,
                        heartRateSamples = heartRateSamples,
                        historyLocked = historyLocked,
                        isMissing = session == null,
                    )
                }.collect { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            report = result.report,
                            isProUser = result.isProUser,
                            heartRateOverlayEnabled = result.heartRateOverlayEnabled,
                            heartRateSamples = result.heartRateSamples,
                            errorMessage =
                                when {
                                    result.historyLocked -> "Unlimited history requires dBcheck Pro"
                                    result.isMissing -> "Session not found"
                                    else -> it.errorMessage
                                },
                        )
                    }
                }
            }
        }
    }

private data class SessionDetailLoadResult(
    val report: com.dbcheck.app.domain.report.SessionReportData?,
    val isProUser: Boolean,
    val heartRateOverlayEnabled: Boolean,
    val heartRateSamples: List<HeartRateSampleUiState>,
    val historyLocked: Boolean,
    val isMissing: Boolean,
)

private fun Session.canBeOpenedBy(isProUser: Boolean): Boolean =
    SessionHistoryPolicy.canAccessSession(startTime, isProUser)

private fun HeartRateServiceSample.toUiState(): HeartRateSampleUiState =
    HeartRateSampleUiState(
        time = time,
        beatsPerMinute = beatsPerMinute,
    )
