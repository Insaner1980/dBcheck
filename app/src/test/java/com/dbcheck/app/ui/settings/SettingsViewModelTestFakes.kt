package com.dbcheck.app.ui.settings

import android.app.Activity
import com.dbcheck.app.billing.BillingGateway
import com.dbcheck.app.billing.PurchaseEvent
import com.dbcheck.app.billing.PurchaseLaunchResult
import com.dbcheck.app.data.export.ExportCsvUseCase
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.CalibrationProfileRepository
import com.dbcheck.app.data.repository.PassiveMonitoringRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioInputDevice
import com.dbcheck.app.domain.passive.PassiveMonitoringDailySummary
import com.dbcheck.app.service.AudioInputDeviceDiscoveryPort
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.BackupService
import com.dbcheck.app.service.HealthConnectService
import com.dbcheck.app.service.HistoryClearService
import com.dbcheck.app.service.PassiveMonitoringManager
import com.dbcheck.app.service.PassiveMonitoringServiceController
import com.dbcheck.app.sync.BackupGateway
import com.dbcheck.app.sync.BackupResult
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectStatus
import com.dbcheck.app.sync.LocalBackup
import com.dbcheck.app.sync.RestoreResult
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

internal class SettingsViewModelTestHarness(
    initialPreferences: UserPreferences = UserPreferences(),
    val passiveMonitoringManager: PassiveMonitoringManager? = null,
    val passiveMonitoringServiceController: PassiveMonitoringServiceController? = null,
) {
    val preferencesFlow = MutableStateFlow(initialPreferences)
    val recordingFlow = MutableStateFlow(false)
    val passiveMonitoringFlow = MutableStateFlow(false)
    val passiveMonitoringDailySummaryFlow = MutableStateFlow(PassiveMonitoringDailySummary())
    val preferencesRepository =
        mockk<PreferencesRepository>(relaxed = true) {
            every { userPreferences } returns preferencesFlow
        }
    val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { getStatus() } returns HealthConnectStatus()
        }
    val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns recordingFlow
        }
    val passiveMonitoringRepository =
        mockk<PassiveMonitoringRepository> {
            every { observeDailySummary(any(), any()) } returns passiveMonitoringDailySummaryFlow
        }

    fun createViewModel(
        calibrationProfileRepository: CalibrationProfileRepository? = null,
        billingGateway: BillingGateway? = null,
        exportCsvUseCase: ExportCsvUseCase? = null,
        backupGateway: BackupGateway? = null,
        historyClearService: HistoryClearService? = null,
        audioInputDeviceDiscoveryPort: AudioInputDeviceDiscoveryPort? = null,
        passiveMonitoringRepository: PassiveMonitoringRepository? = null,
    ): SettingsViewModel = settingsViewModelForTest(
        preferencesRepository = preferencesRepository,
        healthConnectManager = healthConnectManager,
        audioSessionManager = audioSessionManager,
        passiveMonitoringManager =
            passiveMonitoringManager
                ?: mockk {
                    every { isMonitoring } returns passiveMonitoringFlow
                },
        passiveMonitoringRepository = passiveMonitoringRepository ?: this.passiveMonitoringRepository,
        passiveMonitoringServiceController =
            passiveMonitoringServiceController
                ?: mockk(relaxed = true) {
                    coEvery { startPassiveMonitoring() } returns true
                    coEvery { stopPassiveMonitoring() } returns true
                },
        calibrationProfileRepository = calibrationProfileRepository ?: testCalibrationProfileRepository(),
        billingGateway = billingGateway ?: TestBillingGateway(),
        exportCsvUseCase = exportCsvUseCase ?: mockk(relaxed = true),
        backupGateway = backupGateway ?: TestBackupGateway(),
        historyClearService = historyClearService ?: mockk(relaxed = true),
        audioInputDeviceDiscoveryPort = audioInputDeviceDiscoveryPort ?: EmptyAudioInputDeviceDiscoveryPort,
    )
}

internal fun settingsViewModelForTest(
    preferencesRepository: PreferencesRepository,
    healthConnectManager: HealthConnectManager,
    audioSessionManager: AudioSessionManager,
    passiveMonitoringManager: PassiveMonitoringManager = mockk {
        every { isMonitoring } returns MutableStateFlow(false)
    },
    passiveMonitoringRepository: PassiveMonitoringRepository = mockk {
        every { observeDailySummary(any(), any()) } returns MutableStateFlow(PassiveMonitoringDailySummary())
    },
    passiveMonitoringServiceController: PassiveMonitoringServiceController = mockk(relaxed = true) {
        coEvery { startPassiveMonitoring() } returns true
        coEvery { stopPassiveMonitoring() } returns true
    },
    calibrationProfileRepository: CalibrationProfileRepository = testCalibrationProfileRepository(),
    billingGateway: BillingGateway = TestBillingGateway(),
    exportCsvUseCase: ExportCsvUseCase = mockk(relaxed = true),
    backupGateway: BackupGateway = TestBackupGateway(),
    historyClearService: HistoryClearService = mockk(relaxed = true),
    audioInputDeviceDiscoveryPort: AudioInputDeviceDiscoveryPort = EmptyAudioInputDeviceDiscoveryPort,
): SettingsViewModel = SettingsViewModel(
    context = testStringContext(),
    preferencesRepository = preferencesRepository,
    calibrationProfileRepository = calibrationProfileRepository,
    healthConnectService = HealthConnectService(healthConnectManager),
    billingGateway = billingGateway,
    exportCsvUseCase = exportCsvUseCase,
    backupService = BackupService(backupGateway),
    audioSessionManager = audioSessionManager,
    passiveMonitoringManager = passiveMonitoringManager,
    passiveMonitoringRepository = passiveMonitoringRepository,
    passiveMonitoringServiceController = passiveMonitoringServiceController,
    historyClearService = historyClearService,
    audioInputDeviceDiscoveryPort = audioInputDeviceDiscoveryPort,
)

internal object EmptyAudioInputDeviceDiscoveryPort : AudioInputDeviceDiscoveryPort {
    override suspend fun listInputDevices(): List<AudioInputDevice> = emptyList()
}

internal class TestBillingGateway : BillingGateway {
    val events = MutableSharedFlow<PurchaseEvent>()
    var launchResult: PurchaseLaunchResult = PurchaseLaunchResult.Started
    var launchFailure: Throwable? = null

    override val purchaseEvents = events

    override suspend fun launchPurchaseFlow(activity: Activity): PurchaseLaunchResult {
        launchFailure?.let { throw it }
        return launchResult
    }
}

internal class TestBackupGateway : BackupGateway {
    override suspend fun listBackups(): List<LocalBackup> = emptyList()

    override suspend fun createLocalBackup(): BackupResult = BackupResult.Failed("Not configured")

    override suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult = RestoreResult.Failed("Not configured")
}
