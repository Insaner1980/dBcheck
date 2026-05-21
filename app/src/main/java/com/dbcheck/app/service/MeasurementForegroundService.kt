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
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.util.DurationFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeasurementForegroundService : Service() {
    companion object {
        const val ACTION_STOP_MEASUREMENT = "com.dbcheck.app.action.STOP_MEASUREMENT"
        const val EXTRA_EMIT_COMPLETED = "com.dbcheck.app.extra.EMIT_COMPLETED"

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
    private var isProUser = false
    private var emitCompletionOnDestroy = true

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + mainDispatcher)
        notificationHelper.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stopRequest = MeasurementForegroundServicePolicy.stopRequest(intent)

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

            else -> startForegroundMeasurement(startId)
        }
    }

    private fun handleStopRequest(request: MeasurementStopRequest, startId: Int) {
        emitCompletionOnDestroy = request.emitCompleted
        updateJob?.cancel()
        audioSessionManager.stopSession(emitCompleted = request.emitCompleted)
        stopSelf(startId)
    }

    private fun startForegroundMeasurement(startId: Int): Int {
        startTimeMs = System.currentTimeMillis()
        latestDb = 0f
        latestPeakDb = 0f
        emitCompletionOnDestroy = true
        val notification =
            notificationHelper.buildRichMeasurementNotification(
                currentDb = 0f,
                peakDb = 0f,
                duration = DurationFormatter.formatClockDuration(0L),
                noiseLevel = NotificationNoiseLevel.SAFE,
                isProUser = false,
                lockscreenMeterEnabled = false,
            )

        val foregroundStarted = startMeasurementForeground(notification)

        return if (MeasurementForegroundServicePolicy.shouldStartAudioSession(foregroundStarted)) {
            startLiveUpdates()
            serviceScope.launch {
                val sessionStarted = audioSessionManager.startSession()
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

        // Collect latest dB reading
        updateJob =
            serviceScope.launch {
                launch {
                    audioSessionManager.recordingFailures.collect {
                        emitCompletionOnDestroy = false
                        stopSelf()
                    }
                }

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
                        latestPeakDb = stats.peakDb
                    }
                }

                launch {
                    preferencesRepository.userPreferences.collect { prefs ->
                        lockscreenMeterEnabled = prefs.lockscreenMeterEnabled
                    }
                }

                launch {
                    proFeatureManager.isProUser.collect { isPro ->
                        isProUser = isPro
                    }
                }

                // Update notification every second
                while (isActive) {
                    delay(1000)
                    val elapsedMs = System.currentTimeMillis() - startTimeMs
                    val duration = DurationFormatter.formatClockDuration(elapsedMs)
                    val notification =
                        notificationHelper.buildRichMeasurementNotification(
                            currentDb = latestDb,
                            peakDb = latestPeakDb,
                            duration = duration,
                            noiseLevel = NotificationNoiseLevel.fromDb(latestDb),
                            isProUser = isProUser,
                            lockscreenMeterEnabled = lockscreenMeterEnabled,
                        )
                    notificationHelper.updateNotification(
                        NotificationHelper.MEASUREMENT_NOTIFICATION_ID,
                        notification,
                    )
                }
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateJob?.cancel()
        audioSessionManager.stopSession(emitCompleted = emitCompletionOnDestroy)
        if (::serviceScope.isInitialized) {
            serviceScope.cancel()
        }
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}

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
