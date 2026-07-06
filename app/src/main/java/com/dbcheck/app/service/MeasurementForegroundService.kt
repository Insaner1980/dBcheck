package com.dbcheck.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dbcheck.app.billing.ProFeatureManager
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.di.MainDispatcher
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.passive.PassiveMonitoringConfig
import com.dbcheck.app.domain.sleep.SleepRecordingConfig
import com.dbcheck.app.service.AudioEngine
import com.dbcheck.app.util.DurationFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeasurementForegroundService : Service() {
    companion object {
        const val ACTION_STOP_MEASUREMENT = "com.dbcheck.app.action.STOP_MEASUREMENT"
        const val EXTRA_EMIT_COMPLETED = "com.dbcheck.app.extra.EMIT_COMPLETED"
        const val EXTRA_RECORDING_MODE = "com.dbcheck.app.extra.RECORDING_MODE"
        const val EXTRA_SLEEP_TARGET_DURATION_MINUTES = "com.dbcheck.app.extra.SLEEP_TARGET_DURATION_MINUTES"
        const val EXTRA_SLEEP_KEEP_AWAKE_ENABLED = "com.dbcheck.app.extra.SLEEP_KEEP_AWAKE_ENABLED"

        fun startMeasurementIntent(context: Context): Intent = Intent(context, MeasurementForegroundService::class.java)

        fun startPassiveMonitoringIntent(context: Context): Intent =
            Intent(context, MeasurementForegroundService::class.java).apply {
                putExtra(EXTRA_RECORDING_MODE, MeasurementRecordingMode.Passive.intentValue)
            }

        fun startSleepIntent(context: Context, targetDurationMinutes: Int, keepAwakeEnabled: Boolean): Intent =
            Intent(context, MeasurementForegroundService::class.java).apply {
                putExtra(EXTRA_RECORDING_MODE, MeasurementRecordingMode.Sleep.intentValue)
                putExtra(EXTRA_SLEEP_TARGET_DURATION_MINUTES, targetDurationMinutes)
                putExtra(EXTRA_SLEEP_KEEP_AWAKE_ENABLED, keepAwakeEnabled)
            }

        fun stopIntent(context: Context, emitCompleted: Boolean): Intent =
            Intent(context, MeasurementForegroundService::class.java).apply {
                action = ACTION_STOP_MEASUREMENT
                putExtra(EXTRA_EMIT_COMPLETED, emitCompleted)
            }
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var audioEngine: AudioEngine

    @Inject
    lateinit var audioSessionManager: AudioSessionManager

    @Inject
    lateinit var passiveMonitoringManager: PassiveMonitoringManager

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var proFeatureManager: ProFeatureManager

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var serviceScope: CoroutineScope
    private var updateJob: Job? = null
    private var startTimeMs = 0L
    private var latestDb = 0f
    private var latestPeakDb = 0f
    private var lockscreenMeterEnabled = false
    private var showLockscreenMeterPublicly = false
    private var isProUser = false
    private var emitCompletionOnDestroy = true
    private var recordingMode = MeasurementRecordingMode.Meter
    private var sleepRecordingConfig: SleepRecordingConfig? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + mainDispatcher)
        notificationHelper.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stopRequest = MeasurementForegroundServicePolicy.stopRequest(intent)
        val startRequest = MeasurementForegroundServicePolicy.startRequest(intent)

        return when {
            stopRequest != null -> {
                handleStopRequest(stopRequest, startId)
                START_NOT_STICKY
            }

            MeasurementForegroundServicePolicy.shouldIgnoreDuplicateStart(updateJob?.isActive == true) ->
                MeasurementForegroundServicePolicy.successStartResult

            !hasMicrophonePermission() -> {
                stopSelf(startId)
                START_NOT_STICKY
            }

            else -> startForegroundMeasurement(startId, startRequest)
        }
    }

    private fun handleStopRequest(request: MeasurementStopRequest, startId: Int) {
        emitCompletionOnDestroy = request.emitCompleted
        updateJob?.cancel()
        stopCurrentRecording(emitCompleted = request.emitCompleted)
        stopSelf(startId)
    }

    private fun startForegroundMeasurement(startId: Int, request: MeasurementStartRequest): Int {
        startTimeMs = System.currentTimeMillis()
        latestDb = 0f
        latestPeakDb = 0f
        emitCompletionOnDestroy = true
        recordingMode = request.recordingMode
        sleepRecordingConfig = request.sleepRecordingConfig
        val notification =
            notificationHelper.buildRichMeasurementNotification(
                reading =
                    MeasurementNotificationReading(
                        currentDb = 0f,
                        peakDb = 0f,
                        duration = DurationFormatter.formatClockDuration(0L),
                        noiseLevel = NotificationNoiseLevel.SAFE,
                    ),
                visibility = MeasurementNotificationVisibility.privateOnly,
                recordingMode = recordingMode,
            )

        val foregroundStarted = startMeasurementForeground(notification)

        return if (MeasurementForegroundServicePolicy.shouldStartAudioSession(foregroundStarted)) {
            startLiveUpdates()
            serviceScope.launch {
                val sessionStarted =
                    when (request.recordingMode) {
                        MeasurementRecordingMode.Meter -> audioSessionManager.startSession()

                        MeasurementRecordingMode.Sleep ->
                            audioSessionManager.startSleepSession(
                                request.sleepRecordingConfig ?: SleepRecordingConfig(),
                            )

                        MeasurementRecordingMode.Passive -> passiveMonitoringManager.startMonitoring()
                    }
                if (!sessionStarted) {
                    updateJob?.cancel()
                    ServiceCompat.stopForeground(
                        this@MeasurementForegroundService,
                        ServiceCompat.STOP_FOREGROUND_REMOVE,
                    )
                    stopSelf(startId)
                }
            }
            MeasurementForegroundServicePolicy.successStartResult
        } else {
            MeasurementForegroundServicePolicy.recordingFailureForForegroundStart(foregroundStarted)?.let { failure ->
                audioSessionManager.reportRecordingFailure(failure)
            }
            if (foregroundStarted) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
            stopSelf(startId)
            START_NOT_STICKY
        }
    }

    private fun startMeasurementForeground(notification: android.app.Notification): Boolean = runCatching {
        ServiceCompat.startForeground(
            this,
            NotificationHelper.MEASUREMENT_NOTIFICATION_ID,
            notification,
            MeasurementForegroundServicePolicy.foregroundServiceType(Build.VERSION.SDK_INT),
        )
    }.isSuccess

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun startLiveUpdates() {
        updateJob?.cancel()
        updateJob =
            serviceScope.launch {
                launchRecordingFailureCollectors()
                launchNotificationStateCollectors()
                runNotificationLoop()
            }
    }

    private fun CoroutineScope.launchRecordingFailureCollectors() {
        launch {
            audioSessionManager.recordingFailures.collect {
                emitCompletionOnDestroy = false
                stopSelf()
            }
        }

        launch {
            passiveMonitoringManager.recordingFailures.collect {
                emitCompletionOnDestroy = false
                stopSelf()
            }
        }
    }

    private fun CoroutineScope.launchNotificationStateCollectors() {
        launch {
            audioEngine.decibelFlow.collect { reading ->
                if (
                    MeasurementForegroundServicePolicy.shouldUseReadingForNotification(
                        readingTimestamp = reading.timestamp,
                        serviceStartTimeMs = startTimeMs,
                    )
                ) {
                    latestDb = reading.weightedDb
                }
            }
        }

        launch {
            audioSessionManager.sessionStats.collect { stats ->
                if (recordingMode != MeasurementRecordingMode.Passive) {
                    latestPeakDb = stats.peakDb
                }
            }
        }

        launch {
            passiveMonitoringManager.monitoringStats.collect { stats ->
                if (recordingMode == MeasurementRecordingMode.Passive) {
                    latestPeakDb = stats.peakDb
                }
            }
        }

        launch {
            preferencesRepository.userPreferences.collect { prefs ->
                lockscreenMeterEnabled = prefs.lockscreenMeterEnabled
                showLockscreenMeterPublicly = prefs.showLockscreenMeterPublicly
            }
        }

        launch {
            proFeatureManager.isProUser.collect { isPro ->
                isProUser = isPro
            }
        }
    }

    private suspend fun runNotificationLoop() {
        while (currentCoroutineContext().isActive) {
            delay(1000)
            val elapsedMs = System.currentTimeMillis() - startTimeMs
            when {
                shouldStopForSleepTarget(elapsedMs) -> {
                    emitCompletionOnDestroy = true
                    audioSessionManager.stopSession(emitCompleted = true)
                    stopSelf()
                    return
                }

                shouldStopForPassiveTarget(elapsedMs) -> {
                    emitCompletionOnDestroy = false
                    passiveMonitoringManager.stopMonitoring()
                    stopSelf()
                    return
                }

                else -> updateMeasurementNotification(elapsedMs)
            }
        }
    }

    private fun shouldStopForSleepTarget(elapsedMs: Long): Boolean =
        MeasurementForegroundServicePolicy.shouldStopForSleepTarget(
            recordingMode = recordingMode,
            elapsedMs = elapsedMs,
            targetDurationMinutes = sleepRecordingConfig?.targetDurationMinutes,
        )

    private fun shouldStopForPassiveTarget(elapsedMs: Long): Boolean =
        MeasurementForegroundServicePolicy.shouldStopForPassiveTarget(
            recordingMode = recordingMode,
            elapsedMs = elapsedMs,
        )

    private fun updateMeasurementNotification(elapsedMs: Long) {
        val notification =
            notificationHelper.buildRichMeasurementNotification(
                reading =
                    MeasurementNotificationReading(
                        currentDb = latestDb,
                        peakDb = latestPeakDb,
                        duration = DurationFormatter.formatClockDuration(elapsedMs),
                        noiseLevel = NotificationNoiseLevel.fromDb(latestDb),
                    ),
                visibility =
                    MeasurementNotificationVisibility(
                        isProUser = isProUser,
                        lockscreenMeterEnabled = lockscreenMeterEnabled,
                        showLockscreenMeterPublicly = showLockscreenMeterPublicly,
                    ),
                recordingMode = recordingMode,
            )
        notificationHelper.updateNotification(
            NotificationHelper.MEASUREMENT_NOTIFICATION_ID,
            notification,
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateJob?.cancel()
        stopCurrentRecording(emitCompleted = emitCompletionOnDestroy)
        sleepRecordingConfig = null
        recordingMode = MeasurementRecordingMode.Meter
        if (::serviceScope.isInitialized) {
            serviceScope.cancel()
        }
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun stopCurrentRecording(emitCompleted: Boolean) {
        when (recordingMode) {
            MeasurementRecordingMode.Meter,
            MeasurementRecordingMode.Sleep,
            -> audioSessionManager.stopSession(emitCompleted = emitCompleted)

            MeasurementRecordingMode.Passive -> passiveMonitoringManager.stopMonitoring()
        }
    }
}

enum class MeasurementRecordingMode(val intentValue: String) {
    Meter("meter"),
    Sleep("sleep"),
    Passive("passive"),
    ;

    companion object {
        fun fromIntentValue(value: String?): MeasurementRecordingMode =
            entries.firstOrNull { it.intentValue == value } ?: Meter
    }
}

internal data class MeasurementStartRequest(
    val recordingMode: MeasurementRecordingMode = MeasurementRecordingMode.Meter,
    val sleepRecordingConfig: SleepRecordingConfig? = null,
)

internal data class MeasurementStopRequest(val emitCompleted: Boolean)

internal object MeasurementForegroundServicePolicy {
    val successStartResult: Int = Service.START_NOT_STICKY

    fun shouldStartAudioSession(foregroundStarted: Boolean): Boolean = foregroundStarted

    fun recordingFailureForForegroundStart(foregroundStarted: Boolean): AudioRecordingFailure? =
        if (foregroundStarted) {
            null
        } else {
            AudioRecordingFailure.StartFailed
        }

    fun shouldIgnoreDuplicateStart(updateLoopActive: Boolean): Boolean = updateLoopActive

    fun shouldUseReadingForNotification(readingTimestamp: Long, serviceStartTimeMs: Long): Boolean =
        readingTimestamp >= serviceStartTimeMs

    fun shouldStopForSleepTarget(
        recordingMode: MeasurementRecordingMode,
        elapsedMs: Long,
        targetDurationMinutes: Int?,
    ): Boolean = recordingMode == MeasurementRecordingMode.Sleep &&
            targetDurationMinutes != null &&
            targetDurationMinutes > 0 &&
            elapsedMs >= targetDurationMinutes * 60_000L

    fun shouldStopForPassiveTarget(recordingMode: MeasurementRecordingMode, elapsedMs: Long): Boolean =
        recordingMode == MeasurementRecordingMode.Passive &&
            elapsedMs >= PassiveMonitoringConfig.DEFAULT_SAMPLE_DURATION_MINUTES * 60_000L

    fun startRequest(intent: Intent?): MeasurementStartRequest {
        val recordingMode =
            MeasurementRecordingMode.fromIntentValue(
                intent?.getStringExtra(MeasurementForegroundService.EXTRA_RECORDING_MODE),
            )
        val sleepRecordingConfig =
            if (recordingMode == MeasurementRecordingMode.Sleep) {
                SleepRecordingConfig.fromPreparedOptions(
                    targetDurationMinutes =
                        intent?.getIntExtra(
                            MeasurementForegroundService.EXTRA_SLEEP_TARGET_DURATION_MINUTES,
                            SleepRecordingConfig.DEFAULT_TARGET_DURATION_MINUTES,
                        ) ?: SleepRecordingConfig.DEFAULT_TARGET_DURATION_MINUTES,
                    keepAwakeEnabled =
                        intent?.getBooleanExtra(
                            MeasurementForegroundService.EXTRA_SLEEP_KEEP_AWAKE_ENABLED,
                            SleepRecordingConfig.DEFAULT_KEEP_AWAKE_ENABLED,
                        ) ?: SleepRecordingConfig.DEFAULT_KEEP_AWAKE_ENABLED,
                )
            } else {
                null
            }
        return MeasurementStartRequest(
            recordingMode = recordingMode,
            sleepRecordingConfig = sleepRecordingConfig,
        )
    }

    fun stopRequest(intent: Intent?): MeasurementStopRequest? = intent
            ?.takeIf { it.action == MeasurementForegroundService.ACTION_STOP_MEASUREMENT }
            ?.let {
                MeasurementStopRequest(
                    emitCompleted =
                        it.getBooleanExtra(
                            MeasurementForegroundService.EXTRA_EMIT_COMPLETED,
                            true,
                        ),
                )
            }

    @SuppressLint("InlinedApi")
    fun foregroundServiceType(sdkInt: Int): Int = if (sdkInt >= Build.VERSION_CODES.R) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    } else {
        0
    }
}
