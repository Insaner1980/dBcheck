package com.dbcheck.app.service

import android.Manifest
import android.app.Service
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

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + mainDispatcher)
        notificationHelper.createChannels()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int =
        if (!hasMicrophonePermission()) {
            stopSelf(startId)
            START_NOT_STICKY
        } else {
            startTimeMs = System.currentTimeMillis()
            val notification =
                notificationHelper.buildRichMeasurementNotification(
                    currentDb = 0f,
                    peakDb = 0f,
                    duration = "00:00",
                    noiseLevel = NotificationNoiseLevel.SAFE,
                    isProUser = false,
                    lockscreenMeterEnabled = false,
                )

            val foregroundStarted = startMeasurementForeground(notification)
            val sessionStarted =
                MeasurementForegroundServicePolicy.shouldStartAudioSession(foregroundStarted) &&
                    audioSessionManager.startSession()

            if (sessionStarted) {
                startLiveUpdates()
                MeasurementForegroundServicePolicy.successStartResult
            } else {
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
                    audioEngine.decibelFlow.collect { reading ->
                        latestDb = reading.weightedDb
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
        if (audioSessionManager.isRecording.value) {
            audioSessionManager.stopSession()
        }
        if (::serviceScope.isInitialized) {
            serviceScope.cancel()
        }
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}

internal object MeasurementForegroundServicePolicy {
    val successStartResult: Int = Service.START_NOT_STICKY

    fun shouldStartAudioSession(foregroundStarted: Boolean): Boolean = foregroundStarted

    fun foregroundServiceType(sdkInt: Int): Int = if (sdkInt >= Build.VERSION_CODES.R) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    } else {
        0
    }
}
