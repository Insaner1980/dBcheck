package com.dbcheck.app.ui.history.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
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
import com.dbcheck.app.util.ProductIdentity
import com.dbcheck.app.util.ShareResultsGenerator
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
        @param:ApplicationContext private val context: Context,
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
                _uiState.update { it.copy(errorMessage = context.getString(R.string.report_pdf_pro_required)) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isExporting = true, message = null, errorMessage = null) }
                val heartRate = loadCurrentHeartRateState(report)
                _uiState.update { it.withHeartRate(heartRate) }
                runCatching {
                    exportPdfReportUseCase.export(
                        report,
                        uri,
                        heartRate.toReportHeartRateSection(isProUser = state.isProUser),
                    )
                }
                    .onSuccess {
                        _uiState.update {
                            it.copy(isExporting = false, message = context.getString(R.string.report_pdf_exported))
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                errorMessage =
                                    error.toUserFacingMessage(
                                        context.getString(R.string.report_pdf_failed),
                                    ),
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
                        it.copy(
                            errorMessage =
                                error.toUserFacingMessage(
                                    context.getString(R.string.report_share_error_failed),
                                ),
                        )
                    }
                }
            }
        }

        fun onSharePngUnavailable() {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.report_share_error_no_app)) }
        }

        fun suggestedPdfName(): String {
            val report = _uiState.value.report
            val timestamp =
                SimpleDateFormat(PDF_FILE_TIMESTAMP_PATTERN, Locale.US)
                    .format(Date(report?.startTime ?: System.currentTimeMillis()))
            val name = SessionMetadata.slugify(report?.sessionName)
            return "$PDF_FILE_PREFIX$PDF_FILE_SEPARATOR$name$PDF_FILE_SEPARATOR$timestamp.$PDF_FILE_EXTENSION"
        }

        fun saveSessionMetadata(name: String, emoji: String, tags: List<String>) {
            if (!_uiState.value.isProUser) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.session_name_pro_required)) }
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
                    _uiState.update {
                        it.copy(message = context.getString(R.string.report_session_updated), errorMessage = null)
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage =
                                error.toUserFacingMessage(
                                    context.getString(R.string.session_name_unable_to_update),
                                ),
                        )
                    }
                }
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(message = null, errorMessage = null) }
        }

        fun refreshHeartRateState() {
            viewModelScope.launch {
                val heartRate = loadCurrentHeartRateState(_uiState.value.report)
                _uiState.update { it.withHeartRate(heartRate) }
            }
        }

        private fun loadSession() {
            viewModelScope.launch {
                combine(
                    sessionRepository.getSessionById(sessionId),
                    measurementRepository.getReportMeasurementsForSession(sessionId),
                    preferencesRepository.userPreferences,
                ) { session, measurements, prefs ->
                    buildLoadResult(session, measurements, prefs)
                }.catch { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            report = null,
                            unavailableReason = null,
                            errorMessage =
                                error.toUserFacingMessage(
                                    context.getString(R.string.report_session_load_failed),
                                ),
                        )
                    }
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
                    context = context,
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
                    status.errorMessage != null ->
                        HeartRateLoadResult(unavailableMessage = status.errorMessage)

                    status.availability != HealthConnectServiceAvailability.AVAILABLE ->
                        HeartRateLoadResult(
                            unavailableMessage =
                                context.getString(R.string.health_connect_unavailable_on_device),
                        )

                    !status.heartRateReadGranted ->
                        HeartRateLoadResult(
                            unavailableMessage =
                                context.getString(R.string.health_connect_heart_rate_permission_required),
                        )

                    else ->
                        runCatching {
                            readHeartRateSamples(report)
                        }.fold(
                            onSuccess = { samples ->
                                HeartRateLoadResult(
                                    enabled = true,
                                    samples = samples,
                                )
                            },
                            onFailure = { error ->
                                if (error is CancellationException) throw error
                                HeartRateLoadResult(
                                    unavailableMessage =
                                        context.getString(R.string.health_connect_heart_rate_read_failed),
                                )
                            },
                        )
                }
            }
        }

        private suspend fun readHeartRateSamples(report: SessionReportData): List<HeartRateSampleUiState> =
            healthConnectService.readHeartRateForSession(
                start = Instant.ofEpochMilli(report.startTime),
                end = Instant.ofEpochMilli(report.endTime),
            ).map { it.toUiState() }

        private suspend fun loadCurrentHeartRateState(report: SessionReportData?): HeartRateLoadResult =
            loadHeartRateState(report, preferencesRepository.userPreferences.first())
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

private fun SessionDetailUiState.withLoadResult(result: SessionDetailLoadResult): SessionDetailUiState = copy(
        isLoading = false,
        report = result.report,
        unavailableReason = result.unavailableReason,
        isProUser = result.isProUser,
        heartRateOverlayEnabled = result.heartRateOverlayEnabled,
        heartRateSamples = result.heartRateSamples,
        heartRateUnavailableMessage = result.heartRateUnavailableMessage,
        errorMessage = nextErrorMessage(result),
    )

private fun SessionDetailUiState.withHeartRate(heartRate: HeartRateLoadResult): SessionDetailUiState = copy(
        heartRateOverlayEnabled = heartRate.enabled,
        heartRateSamples = heartRate.samples,
        heartRateUnavailableMessage = heartRate.unavailableMessage,
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

private fun loadErrorMessage(context: Context, historyLocked: Boolean, isMissing: Boolean): String? = when {
    historyLocked -> context.getString(R.string.session_unlimited_history_requires_pro)
    isMissing -> context.getString(R.string.report_session_not_found_error)
    else -> null
}

private fun Session.canBeOpenedBy(isProUser: Boolean): Boolean =
    SessionHistoryPolicy.canAccessSession(startTime, isProUser)

private fun HeartRateLoadResult.toReportHeartRateSection(isProUser: Boolean): ReportHeartRateSection =
    if (isProUser && enabled) {
        ReportHeartRateSection(
            enabled = true,
            samples = samples.map { it.toReportHeartRateSample() },
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

private fun HeartRateServiceSample.toUiState(): HeartRateSampleUiState = HeartRateSampleUiState(
        time = time,
        beatsPerMinute = beatsPerMinute,
    )

private const val PDF_FILE_PREFIX = ProductIdentity.FILE_NAME_PREFIX
private const val PDF_FILE_SEPARATOR = "-"
private const val PDF_FILE_EXTENSION = "pdf"
private const val PDF_FILE_TIMESTAMP_PATTERN = "yyyyMMdd_HHmm"
