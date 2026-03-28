package com.dbcheck.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.dbcheck.app.domain.audio.AudioEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var updateJob: Job? = null
    private var startTimeMs = 0L
    private var latestDb = 0f

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startTimeMs = System.currentTimeMillis()
        val notification = notificationHelper.buildMeasurementNotification(0f, "00:00")

        ServiceCompat.startForeground(
            this,
            NotificationHelper.MEASUREMENT_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )

        startLiveUpdates()

        return START_STICKY
    }

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

                // Update notification every 2 seconds
                while (isActive) {
                    delay(2000)
                    val elapsedMs = System.currentTimeMillis() - startTimeMs
                    val duration = formatDuration(elapsedMs)
                    val notification = notificationHelper.buildMeasurementNotification(latestDb, duration)
                    notificationHelper.updateNotification(
                        NotificationHelper.MEASUREMENT_NOTIFICATION_ID,
                        notification,
                    )
                }
            }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}
