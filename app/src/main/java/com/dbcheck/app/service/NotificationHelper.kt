package com.dbcheck.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dbcheck.app.MainActivity
import com.dbcheck.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper
    @Inject
    constructor(@param:ApplicationContext private val context: Context) {
        companion object {
            const val MEASUREMENT_CHANNEL_ID = "measurement_channel"
            const val ALERTS_CHANNEL_ID = "alerts_channel"
            const val MEASUREMENT_NOTIFICATION_ID = 1
            const val EXPOSURE_ALERT_NOTIFICATION_ID = 2
            const val PEAK_ALERT_NOTIFICATION_ID = 3
            private const val MEASUREMENT_TAP_REQUEST_CODE = 10
        }

        private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun createChannels() {
            val measurementChannel =
                NotificationChannel(
                    MEASUREMENT_CHANNEL_ID,
                    context.getString(R.string.notification_measurement_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.notification_measurement_channel_description)
                    setShowBadge(false)
                }

            val alertsChannel =
                NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    context.getString(R.string.notification_alerts_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notification_alerts_channel_description)
                }

            notificationManager.createNotificationChannels(
                listOf(measurementChannel, alertsChannel),
            )
        }

        fun updateNotification(id: Int, notification: android.app.Notification) {
            postNotification(id, notification)
        }

        fun buildMeasurementNotification(currentDb: Float, duration: String): Notification =
            measurementNotificationBuilder(currentDb, duration).build()

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
                    setTextViewText(
                        R.id.notification_session_name,
                        context.getString(R.string.notification_live_measurement),
                    )
                }

            return builder
                .setCustomContentView(collapsed)
                .setCustomBigContentView(expanded)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .build()
        }

        private fun measurementNotificationBuilder(currentDb: Float, duration: String): NotificationCompat.Builder =
            NotificationCompat
                .Builder(context, MEASUREMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.notification_content_title))
                .setContentText(context.getString(R.string.notification_content_text, currentDb.toInt(), duration))
                .setContentIntent(measurementTapPendingIntent())
                .setVisibility(NotificationPrivacyPolicy.measurementLockscreenVisibility())
                .setOngoing(true)
                .setSilent(true)

        private fun measurementTapPendingIntent(): PendingIntent {
            val intent =
                Intent()
                    .setClass(context, MainActivity::class.java)
                    .apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

            return PendingIntent.getActivity(
                context,
                MEASUREMENT_TAP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun RemoteViews.bindMeasurementViews(
            currentDb: Float,
            peakDb: Float,
            duration: String,
            noiseLevel: NotificationNoiseLevel,
            includeLabel: Boolean,
        ) {
            setTextViewText(
                R.id.notification_db_value,
                context.getString(R.string.notification_db_value, currentDb.toInt()),
            )
            setTextViewText(
                R.id.notification_peak_duration,
                context.getString(R.string.notification_peak_duration, peakDb.toInt(), duration),
            )
            if (includeLabel) {
                setTextViewText(R.id.notification_noise_label, noiseLevel.label)
            }
            setInt(R.id.notification_noise_dot, "setBackgroundResource", noiseLevel.dotDrawableRes)
        }

        private val NotificationNoiseLevel.label: String
            get() =
                when (this) {
                    NotificationNoiseLevel.SAFE -> context.getString(R.string.notification_noise_safe)
                    NotificationNoiseLevel.ELEVATED -> context.getString(R.string.notification_noise_elevated)
                    NotificationNoiseLevel.DANGEROUS -> context.getString(R.string.notification_noise_dangerous)
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
                    .setContentTitle(context.getString(R.string.notification_exposure_alert_title))
                    .setContentText(
                        context.getString(
                            R.string.notification_exposure_alert_text,
                            avgDb.toInt(),
                            durationMinutes,
                        ),
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

            return postNotification(EXPOSURE_ALERT_NOTIFICATION_ID, notification)
        }

        fun sendPeakWarning(peakDb: Float): Boolean {
            if (!canPostRegularNotifications()) return false

            val notification =
                NotificationCompat
                    .Builder(context, ALERTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(R.string.notification_peak_warning_title))
                    .setContentText(
                        context.getString(R.string.notification_peak_warning_text, peakDb.toInt()),
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

            return postNotification(PEAK_ALERT_NOTIFICATION_ID, notification)
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
