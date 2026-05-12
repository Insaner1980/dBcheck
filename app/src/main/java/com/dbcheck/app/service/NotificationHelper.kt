package com.dbcheck.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
            postNotification(id, notification)
        }

        fun buildMeasurementNotification(
            currentDb: Float,
            duration: String,
        ): Notification = measurementNotificationBuilder(currentDb, duration).build()

        fun buildRichMeasurementNotification(
            currentDb: Float,
            peakDb: Float,
            duration: String,
            noiseLevel: NotificationNoiseLevel,
            isProUser: Boolean,
            lockscreenMeterEnabled: Boolean,
        ): Notification {
            val builder = measurementNotificationBuilder(currentDb, duration)
            if (!isProUser || !lockscreenMeterEnabled) {
                return builder.build()
            }

            val collapsed =
                RemoteViews(context.packageName, R.layout.notification_measurement).apply {
                    bindMeasurementViews(currentDb, peakDb, duration, noiseLevel, includeLabel = false)
                }
            val expanded =
                RemoteViews(context.packageName, R.layout.notification_measurement_expanded).apply {
                    bindMeasurementViews(currentDb, peakDb, duration, noiseLevel, includeLabel = true)
                    setTextViewText(R.id.notification_session_name, "Live measurement")
                }

            return builder
                .setCustomContentView(collapsed)
                .setCustomBigContentView(expanded)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .build()
        }

        private fun measurementNotificationBuilder(
            currentDb: Float,
            duration: String,
        ): NotificationCompat.Builder =
            NotificationCompat
                .Builder(context, MEASUREMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("dBcheck Recording")
                .setContentText("${currentDb.toInt()} dB · $duration")
                .setVisibility(NotificationPrivacyPolicy.measurementLockscreenVisibility())
                .setOngoing(true)
                .setSilent(true)

        private fun RemoteViews.bindMeasurementViews(
            currentDb: Float,
            peakDb: Float,
            duration: String,
            noiseLevel: NotificationNoiseLevel,
            includeLabel: Boolean,
        ) {
            setTextViewText(R.id.notification_db_value, "${currentDb.toInt()} dB")
            setTextViewText(R.id.notification_peak_duration, "Peak ${peakDb.toInt()} dB · $duration")
            if (includeLabel) {
                setTextViewText(R.id.notification_noise_label, noiseLevel.label)
            }
            setInt(R.id.notification_noise_dot, "setBackgroundResource", noiseLevel.dotDrawableRes)
        }

        private val NotificationNoiseLevel.label: String
            get() =
                when (this) {
                    NotificationNoiseLevel.SAFE -> "Safe"
                    NotificationNoiseLevel.ELEVATED -> "Elevated"
                    NotificationNoiseLevel.DANGEROUS -> "Dangerous"
                }

        private val NotificationNoiseLevel.dotDrawableRes: Int
            get() =
                when (this) {
                    NotificationNoiseLevel.SAFE -> R.drawable.notification_dot_green
                    NotificationNoiseLevel.ELEVATED -> R.drawable.notification_dot_yellow
                    NotificationNoiseLevel.DANGEROUS -> R.drawable.notification_dot_red
                }

        fun sendExposureAlert(avgDb: Float, durationMinutes: Int): Boolean {
            if (!canPostRegularNotifications()) return false

            val notification =
                NotificationCompat
                    .Builder(context, ALERTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("High Noise Exposure")
                    .setContentText(
                        "Average ${avgDb.toInt()} dB for ${durationMinutes}min. " +
                            "Consider reducing exposure.",
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

            return postNotification(ALERT_NOTIFICATION_ID, notification)
        }

        fun sendPeakWarning(peakDb: Float): Boolean {
            if (!canPostRegularNotifications()) return false

            val notification =
                NotificationCompat
                    .Builder(context, ALERTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Peak Noise Warning")
                    .setContentText(
                        "Peak ${peakDb.toInt()} dB detected. Consider reducing exposure.",
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

            return postNotification(ALERT_NOTIFICATION_ID, notification)
        }

        private fun postNotification(id: Int, notification: Notification): Boolean = runCatching {
                notificationManager.notify(id, notification)
            }.isSuccess

        private fun canPostRegularNotifications(): Boolean {
            val runtimePermissionGranted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED

            return NotificationPermissionPolicy.canPostRegularNotification(
                sdkInt = Build.VERSION.SDK_INT,
                runtimePermissionGranted = runtimePermissionGranted,
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            )
        }
    }
