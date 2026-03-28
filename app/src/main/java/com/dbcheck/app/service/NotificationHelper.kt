package com.dbcheck.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.dbcheck.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        companion object {
            const val MEASUREMENT_CHANNEL_ID = "measurement_channel"
            const val ALERTS_CHANNEL_ID = "alerts_channel"
            const val MEASUREMENT_NOTIFICATION_ID = 1
            const val ALERT_NOTIFICATION_ID = 2
        }

        private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun createChannels() {
            val measurementChannel =
                NotificationChannel(
                    MEASUREMENT_CHANNEL_ID,
                    "Measurement",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Ongoing measurement notification"
                    setShowBadge(false)
                }

            val alertsChannel =
                NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    "Noise Alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Exposure and peak noise alerts"
                }

            notificationManager.createNotificationChannels(
                listOf(measurementChannel, alertsChannel),
            )
        }

        fun updateNotification(
            id: Int,
            notification: android.app.Notification,
        ) {
            notificationManager.notify(id, notification)
        }

        fun buildMeasurementNotification(
            currentDb: Float,
            duration: String,
        ) = NotificationCompat
            .Builder(context, MEASUREMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("dBcheck Recording")
            .setContentText("${currentDb.toInt()} dB · $duration")
            .setOngoing(true)
            .setSilent(true)
            .build()

        fun sendExposureAlert(
            avgDb: Float,
            durationMinutes: Int,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, ALERTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("High Noise Exposure")
                    .setContentText("Average ${avgDb.toInt()} dB for ${durationMinutes}min. Consider reducing exposure.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
        }
    }
