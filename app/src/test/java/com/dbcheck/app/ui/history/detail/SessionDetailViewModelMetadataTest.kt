package com.dbcheck.app.ui.history.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.data.repository.SleepSessionRepository
import com.dbcheck.app.data.repository.SoundDetectionRepository
import com.dbcheck.app.domain.report.ReportHeartRateSample
import com.dbcheck.app.domain.report.ReportHeartRateSection
import com.dbcheck.app.domain.report.ReportMeasurement
import com.dbcheck.app.domain.report.ReportSoundEvent
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.sleep.SleepSession
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.WavRecordingFileStore
import com.dbcheck.app.sync.HealthConnectAvailability
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectPermissions
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.sync.HeartRateSample
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.util.ExportPdfReportUseCase
import com.dbcheck.app.util.PdfReportExportMetadata
import com.dbcheck.app.util.ShareResultsGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelMetadataTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionFlow = MutableStateFlow(session())
    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = true))
    private val sleepSessionFlow = MutableStateFlow<SleepSession?>(null)
    private val sessionRepository =
        mockk<SessionRepository> {
            every { getSessionById(SESSION_ID) } returns sessionFlow
            coEvery { updateSessionMetadata(any(), any(), any(), any()) } just runs
        }
    private val measurementRepository =
        mockk<MeasurementRepository> {
            every { getReportMeasurementsForSession(SESSION_ID) } returns flowOf(emptyList())
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }
    private val soundDetectionRepository =
        mockk<SoundDetectionRepository> {
            every { getReportSoundEventsForSession(SESSION_ID) } returns flowOf(emptyList())
        }
    private val sleepSessionRepository =
        mockk<SleepSessionRepository> {
            every { getSleepSession(SESSION_ID) } returns sleepSessionFlow
        }
    private val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { readHeartRateForSession(any(), any()) } returns emptyList()
        }
    private val wavRecordingFileStore =
        mockk<WavRecordingFileStore>(relaxed = true) {
            every { hasRecordingForSession(SESSION_ID) } returns false
            every { createShareIntent(SESSION_ID) } returns null
            every { deleteRecordingForSession(SESSION_ID) } returns false
        }

    @Test
    fun proUserCanSaveSessionMetadata() = runTest {
            val viewModel = createViewModel()

            viewModel.saveSessionMetadata(
                name = "  Workshop  ",
                emoji = "🎧",
                tags = listOf("Work", "work", "Music"),
            )

            coVerify {
                sessionRepository.updateSessionMetadata(
                    id = SESSION_ID,
                    name = "Workshop",
                    emoji = "🎧",
                    tags = listOf("Work", "Music"),
                )
            }
            assertEquals("Session updated", viewModel.uiState.value.message)
        }

    @Test
    fun freeUserCannotSaveSessionMetadata() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()

            viewModel.saveSessionMetadata(
                name = "Workshop",
                emoji = "🎧",
                tags = listOf("Work"),
            )

            coVerify(exactly = 0) { sessionRepository.updateSessionMetadata(any(), any(), any(), any()) }
            assertEquals("Session naming requires dBcheck Pro", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun freeUserCannotLoadSessionOutsideFreeHistoryWindow() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)

            val viewModel = createViewModel()

            assertEquals(null, viewModel.uiState.value.report)
            assertEquals(false, viewModel.uiState.value.isNotFound)
            assertEquals(true, viewModel.uiState.value.isHistoryLocked)
            assertEquals("Unlimited history requires dBcheck Pro", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun proUpgradeReloadsLockedSessionAndClearsLockedError() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()

            preferencesFlow.value = UserPreferences(isProUser = true)

            assertEquals(SESSION_ID, viewModel.uiState.value.report?.sessionId)
            assertEquals(false, viewModel.uiState.value.isHistoryLocked)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun sessionLoadFailureShowsUserFacingError() = runTest {
            every { sessionRepository.getSessionById(SESSION_ID) } returns
                flow { throw IllegalStateException("db") }

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isLoading)
            assertEquals(null, viewModel.uiState.value.report)
            assertEquals("Unable to load session", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun sharePngFailureShowsError() = runTest {
            val shareResultsGenerator =
                mockk<ShareResultsGenerator> {
                    coEvery { shareSessionReportCard(any()) } throws IllegalStateException("Share failed")
                }
            val viewModel = createViewModel(shareResultsGenerator = shareResultsGenerator)

            viewModel.createSharePngIntent()

            assertEquals("Unable to share session", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun sessionLoadShowsExistingWavRecordingAvailability() = runTest {
            every { wavRecordingFileStore.hasRecordingForSession(SESSION_ID) } returns true

            val viewModel = createViewModel()

            assertEquals(true, viewModel.uiState.value.hasWavRecording)
        }

    @Test
    fun proUserCanCreateWavShareIntent() = runTest {
            val wavShareIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
            every { wavRecordingFileStore.hasRecordingForSession(SESSION_ID) } returns true
            every { wavRecordingFileStore.createShareIntent(SESSION_ID) } returns wavShareIntent
            val viewModel = createViewModel()

            viewModel.shareWavIntents.test {
                viewModel.createShareWavIntent()
                assertSame(wavShareIntent, awaitItem())
            }
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun freeUserCannotCreateWavShareIntent() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            every { wavRecordingFileStore.hasRecordingForSession(SESSION_ID) } returns true
            val viewModel = createViewModel()

            viewModel.shareWavIntents.test {
                viewModel.createShareWavIntent()
                expectNoEvents()
            }

            assertEquals("WAV export requires dBcheck Pro", viewModel.uiState.value.errorMessage)
            io.mockk.verify(exactly = 0) { wavRecordingFileStore.createShareIntent(any()) }
        }

    @Test
    fun deleteWavRecordingRemovesFileAndClearsAvailability() = runTest {
            every { wavRecordingFileStore.hasRecordingForSession(SESSION_ID) } returns true
            every { wavRecordingFileStore.deleteRecordingForSession(SESSION_ID) } returns true
            val viewModel = createViewModel()

            viewModel.deleteWavRecording()

            assertEquals(false, viewModel.uiState.value.hasWavRecording)
            assertEquals("WAV recording deleted", viewModel.uiState.value.message)
        }

    @Test
    fun suggestedPdfNameUsesBrandedFilePrefix() = runTest {
            val viewModel = createViewModel()

            val fileName = viewModel.suggestedPdfName()

            assertTrue(fileName.startsWith("dBcheck-"))
            assertTrue(fileName.endsWith(".pdf"))
            assertFalse(fileName.startsWith("dbcheck-"))
        }

    @Test
    fun revokedHeartRatePermissionDisablesEffectiveOverlayInSessionDetail() = runTest {
            enableHeartRateOverlayStatus(grantedPermissions = emptySet())
            val viewModel = createViewModel()

            assertEquals(false, viewModel.uiState.value.heartRateOverlayEnabled)
            assertEquals(
                "Health Connect heart rate permission is required to show this overlay",
                viewModel.uiState.value.heartRateUnavailableMessage,
            )
            coVerify(exactly = 0) { healthConnectManager.readHeartRateForSession(any(), any()) }
        }

    @Test
    fun unavailableHealthConnectShowsHeartRateUnavailableReason() = runTest {
            enableHeartRateOverlayStatus(
                availability = HealthConnectAvailability.UNAVAILABLE,
                grantedPermissions = emptySet(),
            )
            val viewModel = createViewModel()

            assertEquals(false, viewModel.uiState.value.heartRateOverlayEnabled)
            assertEquals(
                "Health Connect is unavailable on this device",
                viewModel.uiState.value.heartRateUnavailableMessage,
            )
            coVerify(exactly = 0) { healthConnectManager.readHeartRateForSession(any(), any()) }
        }

    @Test
    fun heartRateReadFailureShowsUnavailableReason() = runTest {
            enableHeartRateOverlayStatus()
            coEvery { healthConnectManager.readHeartRateForSession(any(), any()) } throws
                IllegalStateException("read failed")

            val viewModel = createViewModel()

            assertEquals(false, viewModel.uiState.value.heartRateOverlayEnabled)
            assertEquals(
                "Unable to read Health Connect heart rate samples",
                viewModel.uiState.value.heartRateUnavailableMessage,
            )
        }

    @Test
    fun heartRateStatusFailureShowsStatusErrorInsteadOfPermissionReason() = runTest {
            enableHeartRateOverlayStatus()
            coEvery { healthConnectManager.getStatus() } returns
                HealthConnectStatus(
                    availability = HealthConnectAvailability.AVAILABLE,
                    grantedPermissions = emptySet(),
                    errorMessage = "Unable to check Health Connect status",
                )

            val viewModel = createViewModel()

            assertEquals(false, viewModel.uiState.value.heartRateOverlayEnabled)
            assertEquals(
                "Unable to check Health Connect status",
                viewModel.uiState.value.heartRateUnavailableMessage,
            )
            coVerify(exactly = 0) { healthConnectManager.readHeartRateForSession(any(), any()) }
        }

    @Test
    fun exportPdfIncludesEnabledHeartRateOverlayData() = runTest {
            val (exportPdfReportUseCase, viewModel) = createHeartRatePdfExportViewModel()

            viewModel.exportPdf(mockk<Uri>())

            coVerify {
                exportPdfReportUseCase.export(
                    report = any(),
                    outputUri = any(),
                    heartRate = ReportHeartRateSection(
                        enabled = true,
                        samples =
                            listOf(
                                ReportHeartRateSample(
                                    timestamp = 1_700_000_010_000L,
                                    beatsPerMinute = 72L,
                                ),
                            ),
                    ),
                    metadata = any(),
                )
            }
        }

    @Test
    fun heartRateRefreshAfterPermissionRevocationClearsSamplesBeforePdfExport() = runTest {
            val (exportPdfReportUseCase, viewModel) = createHeartRatePdfExportViewModel()
            assertEquals(true, viewModel.uiState.value.heartRateOverlayEnabled)
            assertEquals(1, viewModel.uiState.value.heartRateSamples.size)
            coEvery { healthConnectManager.getStatus() } returns
                HealthConnectStatus(
                    availability = HealthConnectAvailability.AVAILABLE,
                    grantedPermissions = emptySet(),
                )

            viewModel.refreshHeartRateState()
            advanceUntilIdle()
            viewModel.exportPdf(mockk<Uri>())

            assertEquals(false, viewModel.uiState.value.heartRateOverlayEnabled)
            assertEquals(emptyList<HeartRateSampleUiState>(), viewModel.uiState.value.heartRateSamples)
            assertEquals(
                "Health Connect heart rate permission is required to show this overlay",
                viewModel.uiState.value.heartRateUnavailableMessage,
            )
            coVerify {
                exportPdfReportUseCase.export(
                    report = any(),
                    outputUri = any(),
                    heartRate = ReportHeartRateSection(),
                    metadata = any(),
                )
            }
        }

    @Test
    fun exportPdfIncludesCurrentEffectiveCalibrationOffsetMetadata() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true, micSensitivityOffset = 3.5f)
            val exportPdfReportUseCase =
                mockk<ExportPdfReportUseCase> {
                    coEvery { export(any(), any(), any(), any()) } just runs
                }
            val viewModel = createViewModel(exportPdfReportUseCase = exportPdfReportUseCase)

            viewModel.exportPdf(mockk<Uri>())

            coVerify {
                exportPdfReportUseCase.export(
                    report = any(),
                    outputUri = any(),
                    heartRate = any(),
                    metadata =
                        match<PdfReportExportMetadata> {
                            it.calibrationOffsetDb == 3.5f
                        },
                )
            }
        }

    @Test
    fun loadedReportIncludesPersistedSoundTypeEvents() = runTest {
            every { soundDetectionRepository.getReportSoundEventsForSession(SESSION_ID) } returns
                flowOf(
                    listOf(
                        ReportSoundEvent(
                            timestamp = 1_700_000_001_000L,
                            label = "Speech",
                            confidence = 0.82f,
                        ),
                    ),
                )

            val viewModel = createViewModel()

            assertEquals("Speech", viewModel.uiState.value.report?.soundTypeSummary?.label)
        }

    @Test
    fun sleepSessionBuildsSleepResultsFromReport() = runTest {
            sessionFlow.value =
                session().copy(
                    endTime = 1_700_000_300_000L,
                    avgDb = 71.4f,
                    maxDb = 91.2f,
                    peakDb = 110.5f,
                )
            sleepSessionFlow.value =
                SleepSession(
                    sessionId = SESSION_ID,
                    targetDurationMinutes = 480,
                    keepAwakeEnabled = false,
                    createdAt = 1_700_000_000_000L,
                )
            every { measurementRepository.getReportMeasurementsForSession(SESSION_ID) } returns
                flowOf(
                    listOf(
                        ReportMeasurement(timestamp = 1_700_000_000_000L, dbWeighted = 60f),
                        ReportMeasurement(timestamp = 1_700_000_060_000L, dbWeighted = 86f),
                        ReportMeasurement(timestamp = 1_700_000_120_000L, dbWeighted = 87f),
                        ReportMeasurement(timestamp = 1_700_000_180_000L, dbWeighted = 72f),
                        ReportMeasurement(timestamp = 1_700_000_240_000L, dbWeighted = 90f),
                    ),
                )

            val viewModel = createViewModel()
            val results = requireNotNull(viewModel.uiState.value.sleepResults)
            val insights = requireNotNull(viewModel.uiState.value.sleepInsights)
            val reportSleep = requireNotNull(viewModel.uiState.value.report?.sleep)

            assertEquals(480, results.targetDurationMinutes)
            assertEquals(5 * 60_000L, results.recordedDurationMs)
            assertEquals("LAeq", results.equivalentLevelLabel)
            assertEquals(71.4f, results.equivalentLevelDb, 0.001f)
            assertEquals(91.2f, results.maxDb, 0.001f)
            assertEquals(110.5f, results.lcPeakDb, 0.001f)
            assertEquals(2, results.peakEventCount)
            assertEquals(2, results.loudPeriodCount)
            assertEquals(5, results.sampleCount)
            assertTrue(results.histogramBuckets.isNotEmpty())
            assertEquals(true, insights.isAvailable)
            assertEquals(2, insights.notableEventCount)
            assertEquals(90f, insights.loudestPeriod?.maxDb ?: 0f, 0.001f)
            assertEquals(480, reportSleep.targetDurationMinutes)
            assertEquals(5 * 60_000L, reportSleep.recordedDurationMs)
            assertEquals(false, reportSleep.keepAwakeEnabled)
            assertEquals(2, reportSleep.peakEventCount)
            assertEquals(2, reportSleep.loudPeriodCount)
        }

    @Test
    fun sleepSessionWithoutMeasurementsKeepsInsightCountsUnavailable() = runTest {
            sleepSessionFlow.value =
                SleepSession(
                    sessionId = SESSION_ID,
                    targetDurationMinutes = 480,
                    keepAwakeEnabled = false,
                    createdAt = 1_700_000_000_000L,
                )
            every { measurementRepository.getReportMeasurementsForSession(SESSION_ID) } returns flowOf(emptyList())

            val viewModel = createViewModel()
            val results = requireNotNull(viewModel.uiState.value.sleepResults)
            val insights = requireNotNull(viewModel.uiState.value.sleepInsights)

            assertEquals(null, results.peakEventCount)
            assertEquals(null, results.loudPeriodCount)
            assertEquals(null, results.sampleCount)
            assertEquals(false, insights.isAvailable)
            assertEquals(null, insights.notableEventCount)
            assertEquals(null, insights.loudestPeriod)
        }

    private fun createViewModel(
        shareResultsGenerator: ShareResultsGenerator = mockk<ShareResultsGenerator>(),
        exportPdfReportUseCase: ExportPdfReportUseCase = mockk<ExportPdfReportUseCase>(),
    ): SessionDetailViewModel = SessionDetailViewModel(
            context = testStringContext(),
            savedStateHandle = SavedStateHandle(mapOf(Screen.SessionDetail.ARG_SESSION_ID to SESSION_ID)),
            sessionRepository = sessionRepository,
            measurementRepository = measurementRepository,
            preferencesRepository = preferencesRepository,
            sleepSessionRepository = sleepSessionRepository,
            soundDetectionRepository = soundDetectionRepository,
            exportPdfReportUseCase = exportPdfReportUseCase,
            shareResultsGenerator = shareResultsGenerator,
            healthConnectService = HealthConnectService(healthConnectManager),
            wavRecordingFileStore = wavRecordingFileStore,
        )

    private fun createHeartRatePdfExportViewModel(): Pair<ExportPdfReportUseCase, SessionDetailViewModel> {
        enableHeartRateOverlayStatus()
        coEvery { healthConnectManager.readHeartRateForSession(any(), any()) } returns enabledHeartRateSamples()
        val exportPdfReportUseCase =
            mockk<ExportPdfReportUseCase> {
                coEvery { export(any(), any(), any(), any()) } just runs
            }

        return exportPdfReportUseCase to createViewModel(exportPdfReportUseCase = exportPdfReportUseCase)
    }

    private fun enabledHeartRateSamples(): List<HeartRateSample> = listOf(
        HeartRateSample(
            time = Instant.ofEpochMilli(1_700_000_010_000L),
            beatsPerMinute = 72L,
        ),
    )

    private fun enableHeartRateOverlayStatus(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String> = HealthConnectPermissions.HEART_RATE_READ,
    ) {
        preferencesFlow.value = UserPreferences(isProUser = true, heartRateOverlayEnabled = true)
        coEvery { healthConnectManager.getStatus() } returns
            HealthConnectStatus(
                availability = availability,
                grantedPermissions = grantedPermissions,
            )
    }

    private companion object {
        const val SESSION_ID = 42L

        fun session() = Session(
                id = SESSION_ID,
                startTime = 1_700_000_000_000L,
                endTime = 1_700_000_060_000L,
                minDb = 60f,
                avgDb = 70f,
                maxDb = 82f,
                peakDb = 91f,
                name = null,
                emoji = null,
                tags = emptyList(),
                isActive = false,
                frequencyWeighting = "A",
            )
    }
}
