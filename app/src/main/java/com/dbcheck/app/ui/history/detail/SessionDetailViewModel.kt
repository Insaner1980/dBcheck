package com.dbcheck.app.ui.history.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.data.repository.SleepSessionRepository
import com.dbcheck.app.data.repository.SoundDetectionRepository
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.report.ReportHeartRateSample
import com.dbcheck.app.domain.report.ReportHeartRateSection
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.report.ReportSleepSection
import com.dbcheck.app.domain.report.ReportSoundEvent
import com.dbcheck.app.domain.report.SessionReportCalculator
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionHistoryPolicy
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.domain.sleep.SleepInsightsAvailability
import com.dbcheck.app.domain.sleep.SleepInsightsSummary
import com.dbcheck.app.domain.sleep.SleepNotableEventSummary
import com.dbcheck.app.domain.sleep.SleepResultsCalculator
import com.dbcheck.app.domain.sleep.SleepResultsSummary
import com.dbcheck.app.domain.sleep.SleepSession
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.HealthConnectServiceAvailability
import com.dbcheck.app.service.HeartRateServiceSample
import com.dbcheck.app.service.WavRecordingFileStore
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.util.ExportPdfReportUseCase
import com.dbcheck.app.util.PdfReportExportMetadata
import com.dbcheck.app.util.ProductIdentity
import com.dbcheck.app.util.ShareResultsGenerator
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.withContext
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
        private val soundDetectionRepository: SoundDetectionRepository,
        private val preferencesRepository: PreferencesRepository,
        private val sleepSessionRepository: SleepSessionRepository,
        private val exportPdfReportUseCase: ExportPdfReportUseCase,
        private val shareResultsGenerator: ShareResultsGenerator,
        private val healthConnectService: HealthConnectService,
        private val wavRecordingFileStore: WavRecordingFileStore,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val sessionId =
            savedStateHandle.get<Long>(Screen.SessionDetail.ARG_SESSION_ID)
                ?: savedStateHandle.get<String>(Screen.SessionDetail.ARG_SESSION_ID)?.toLongOrNull()
                ?: -1L

        private val _uiState = MutableStateFlow(SessionDetailUiState())
        val uiState: StateFlow<SessionDetailUiState> = _uiState
        private val _sharePngIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val sharePngIntents: SharedFlow<Intent> = _sharePngIntents.asSharedFlow()
        private val _shareWavIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val shareWavIntents: SharedFlow<Intent> = _shareWavIntents.asSharedFlow()

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
                val prefs = preferencesRepository.userPreferences.first()
                val heartRate = loadHeartRateState(report, prefs)
                _uiState.update { it.withHeartRate(heartRate) }
                runCatching {
                    exportPdfReportUseCase.export(
                        report,
                        uri,
                        heartRate.toReportHeartRateSection(isProUser = state.isProUser),
                        PdfReportExportMetadata.current(
                            calibrationOffsetDb = ProAudioPreferencePolicy.micOffset(prefs),
                        ),
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

        fun createShareWavIntent() {
            val state = _uiState.value
            when {
                !state.isProUser -> {
                    _uiState.update {
                        it.copy(errorMessage = context.getString(R.string.report_wav_export_requires_pro))
                    }
                    return
                }

                !state.hasWavRecording -> {
                    _uiState.update { it.copy(errorMessage = context.getString(R.string.report_wav_not_available)) }
                    return
                }
            }

            viewModelScope.launch {
                runCatching {
                    withContext(ioDispatcher) {
                        wavRecordingFileStore.createShareIntent(sessionId)
                            ?: error("No WAV recording for session $sessionId")
                    }
                }.onSuccess { intent ->
                    _uiState.update { it.copy(errorMessage = null) }
                    _shareWavIntents.emit(intent)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            errorMessage =
                                error.toUserFacingMessage(
                                    context.getString(R.string.report_wav_share_failed),
                                ),
                        )
                    }
                }
            }
        }

        fun onShareWavUnavailable() {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.report_wav_share_no_app)) }
        }

        fun deleteWavRecording() {
            if (!_uiState.value.hasWavRecording) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.report_wav_not_available)) }
                return
            }

            viewModelScope.launch {
                val deleted =
                    withContext(ioDispatcher) {
                        wavRecordingFileStore.deleteRecordingForSession(sessionId)
                    }
                _uiState.update {
                    if (deleted) {
                        it.copy(
                            hasWavRecording = false,
                            message = context.getString(R.string.report_wav_deleted),
                            errorMessage = null,
                        )
                    } else {
                        it.copy(errorMessage = context.getString(R.string.report_wav_delete_failed))
                    }
                }
            }
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
                    soundDetectionRepository.getReportSoundEventsForSession(sessionId),
                    sleepSessionRepository.getSleepSession(sessionId),
                    preferencesRepository.userPreferences,
                ) { session, measurements, soundEvents, sleepSession, prefs ->
                    buildLoadResult(session, measurements, soundEvents, sleepSession, prefs)
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
            soundEvents: List<ReportSoundEvent>,
            sleepSession: SleepSession?,
            prefs: UserPreferences,
        ): SessionDetailLoadResult {
            val historyLocked = session != null && !session.canBeOpenedBy(prefs.isProUser)
            val baseReport = session.takeUnless { historyLocked }?.toReport(measurements, soundEvents)
            val sleepSummary =
                if (baseReport != null && sleepSession != null) {
                    SleepResultsCalculator.build(sleepSession = sleepSession, report = baseReport)
                } else {
                    null
                }
            val report = baseReport?.copy(sleep = sleepSummary?.toReportSleepSection())
            val heartRate = loadHeartRateState(report, prefs)
            val hasWavRecording =
                !historyLocked &&
                    session != null &&
                    withContext(ioDispatcher) {
                        wavRecordingFileStore.hasRecordingForSession(sessionId)
                    }

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
                sleepResults = sleepSummary?.toUiState(),
                sleepInsights = sleepSummary?.insights?.toUiState(),
                hasWavRecording = hasWavRecording,
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
    val sleepResults: SleepResultsUiState?,
    val sleepInsights: SleepInsightsUiState?,
    val hasWavRecording: Boolean,
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
        sleepResults = result.sleepResults,
        sleepInsights = result.sleepInsights,
        hasWavRecording = result.hasWavRecording,
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

private fun SleepResultsSummary.toUiState(): SleepResultsUiState = SleepResultsUiState(
        targetDurationMinutes = targetDurationMinutes,
        recordedDurationMs = recordedDurationMs,
        equivalentLevelLabel = equivalentLevelLabel,
        equivalentLevelDb = equivalentLevelDb,
        maxDb = maxDb,
        lcPeakDb = lcPeakDb,
        peakEventCount = peakEventCount,
        loudPeriodCount = loudPeriodCount,
        sampleCount = sampleCount,
        histogramBuckets = histogramBuckets,
    )

private fun SleepInsightsSummary.toUiState(): SleepInsightsUiState = SleepInsightsUiState(
        isAvailable = availability == SleepInsightsAvailability.Available,
        notableEventCount = notableEvents.size.takeIf { availability == SleepInsightsAvailability.Available },
        loudestPeriod = loudestEvent?.toUiState(),
    )

private fun SleepNotableEventSummary.toUiState(): SleepInsightPeriodUiState = SleepInsightPeriodUiState(
        durationMs = durationMs,
        maxDb = maxDb,
    )

private fun SleepResultsSummary.toReportSleepSection(): ReportSleepSection = ReportSleepSection(
        targetDurationMinutes = targetDurationMinutes,
        recordedDurationMs = recordedDurationMs,
        keepAwakeEnabled = keepAwakeEnabled,
        peakEventCount = peakEventCount,
        loudPeriodCount = loudPeriodCount,
    )

private fun Session.toReport(
    measurements: List<ReportMeasurement>,
    soundEvents: List<ReportSoundEvent>,
): SessionReportData = SessionReportCalculator.build(
        session = this,
        measurements = measurements,
        soundEvents = soundEvents,
    )

private fun HeartRateServiceSample.toUiState(): HeartRateSampleUiState = HeartRateSampleUiState(
        time = time,
        beatsPerMinute = beatsPerMinute,
    )

private const val PDF_FILE_PREFIX = ProductIdentity.FILE_NAME_PREFIX
private const val PDF_FILE_SEPARATOR = "-"
private const val PDF_FILE_EXTENSION = "pdf"
private const val PDF_FILE_TIMESTAMP_PATTERN = "yyyyMMdd_HHmm"
