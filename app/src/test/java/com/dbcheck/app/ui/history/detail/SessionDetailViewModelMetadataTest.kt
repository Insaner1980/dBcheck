package com.dbcheck.app.ui.history.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.report.ReportHeartRateSample
import com.dbcheck.app.domain.report.ReportHeartRateSection
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.sync.HealthConnectAvailability
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectPermissions
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.sync.HeartRateSample
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.util.ExportPdfReportUseCase
import com.dbcheck.app.util.ShareResultsGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SessionDetailViewModelMetadataTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionFlow = MutableStateFlow(session())
    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = true))
    private val sessionRepository =
        mockk<SessionRepository> {
            every { getSessionById(SESSION_ID) } returns sessionFlow
            coEvery { updateSessionMetadata(any(), any(), any(), any()) } just runs
        }
    private val measurementRepository =
        mockk<MeasurementRepository> {
            every { getMeasurementsForSession(SESSION_ID) } returns flowOf(emptyList<MeasurementEntity>())
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }
    private val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { readHeartRateForSession(any(), any()) } returns emptyList()
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
    fun revokedHeartRatePermissionDisablesEffectiveOverlayInSessionDetail() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true, heartRateOverlayEnabled = true)
            coEvery { healthConnectManager.getStatus() } returns
                HealthConnectStatus(
                    availability = HealthConnectAvailability.AVAILABLE,
                    grantedPermissions = emptySet(),
                )
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
            preferencesFlow.value = UserPreferences(isProUser = true, heartRateOverlayEnabled = true)
            coEvery { healthConnectManager.getStatus() } returns
                HealthConnectStatus(
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
    fun exportPdfIncludesEnabledHeartRateOverlayData() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true, heartRateOverlayEnabled = true)
            coEvery { healthConnectManager.getStatus() } returns
                HealthConnectStatus(
                    availability = HealthConnectAvailability.AVAILABLE,
                    grantedPermissions = HealthConnectPermissions.HEART_RATE_READ,
                )
            coEvery { healthConnectManager.readHeartRateForSession(any(), any()) } returns
                listOf(
                    HeartRateSample(
                        time = Instant.ofEpochMilli(1_700_000_010_000L),
                        beatsPerMinute = 72L,
                    ),
                )
            val exportPdfReportUseCase =
                mockk<ExportPdfReportUseCase> {
                    coEvery { export(any(), any(), any()) } just runs
                }
            val viewModel = createViewModel(exportPdfReportUseCase = exportPdfReportUseCase)

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
                )
            }
        }

    private fun createViewModel(
        shareResultsGenerator: ShareResultsGenerator = mockk<ShareResultsGenerator>(),
        exportPdfReportUseCase: ExportPdfReportUseCase = mockk<ExportPdfReportUseCase>(),
    ): SessionDetailViewModel = SessionDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Screen.SessionDetail.ARG_SESSION_ID to SESSION_ID)),
            sessionRepository = sessionRepository,
            measurementRepository = measurementRepository,
            preferencesRepository = preferencesRepository,
            exportPdfReportUseCase = exportPdfReportUseCase,
            shareResultsGenerator = shareResultsGenerator,
            healthConnectService = HealthConnectService(healthConnectManager),
        )

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
