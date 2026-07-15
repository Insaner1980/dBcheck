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
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.util.DurationFormatter
import com.dbcheck.app.util.ExternalBrand
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class MeasurementNotificationReading(
    val currentDb: Float,
    val peakDb: Float,
    val duration: String,
    val noiseLevel: NotificationNoiseLevel,
)

data class MeasurementNotificationVisibility(
    val isProUser: Boolean,
    val lockscreenMeterEnabled: Boolean,
    val showLockscreenMeterPublicly: Boolean,
) {
    companion object {
        val privateOnly =
            MeasurementNotificationVisibility(
                isProUser = false,
                lockscreenMeterEnabled = false,
                showLockscreenMeterPublicly = false,
            )
    }
}

@Singleton
class NotificationHelper
    @Inject
    constructor(@param:ApplicationContext private val context: Context) {
        companion object {
            const val MEASUREMENT_CHANNEL_ID = "measurement_channel"
            const val ALERTS_CHANNEL_ID = "alerts_channel"
            const val AMBIENT_PLAYBACK_CHANNEL_ID = "ambient_playback_channel"
            const val MEASUREMENT_NOTIFICATION_ID = 1
            const val EXPOSURE_ALERT_NOTIFICATION_ID = 2
            const val PEAK_ALERT_NOTIFICATION_ID = 3
            const val VOICE_VOLUME_WARNING_NOTIFICATION_ID = 4
            const val AMBIENT_SOUND_NOTIFICATION_ID = 5
            private const val MEASUREMENT_TAP_REQUEST_CODE = 10
            private const val MEASUREMENT_STOP_REQUEST_CODE = 11
            private const val AMBIENT_TAP_REQUEST_CODE = 12
            private const val AMBIENT_STOP_REQUEST_CODE = 13
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

            val ambientPlaybackChannel =
                NotificationChannel(
                    AMBIENT_PLAYBACK_CHANNEL_ID,
                    context.getString(R.string.notification_ambient_sound_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.notification_ambient_sound_channel_description)
                    setShowBadge(false)
                }

            notificationManager.createNotificationChannels(
                listOf(measurementChannel, alertsChannel, ambientPlaybackChannel),
            )
        }

        fun updateNotification(id: Int, notification: android.app.Notification) {
            postNotification(id, notification)
        }

        fun canPostPlaybackNotification(): Boolean = canPostRegularNotifications()

        fun buildRichMeasurementNotification(
            reading: MeasurementNotificationReading,
            visibility: MeasurementNotificationVisibility,
            recordingMode: MeasurementRecordingMode = MeasurementRecordingMode.Meter,
        ): Notification {
            val builder =
                measurementNotificationBuilder(
                    currentDb = reading.currentDb,
                    duration = reading.duration,
                    recordingMode = recordingMode,
                    lockscreenVisibility =
                        NotificationPrivacyPolicy.measurementLockscreenVisibility(
                            isProUser = visibility.isProUser,
                            lockscreenMeterEnabled = visibility.lockscreenMeterEnabled,
                            showLockscreenMeterPublicly = visibility.showLockscreenMeterPublicly,
                        ),
                )
            if (!visibility.isProUser || !visibility.lockscreenMeterEnabled) {
                return builder.build()
            }

            val collapsed =
                RemoteViews(context.packageName, R.layout.notification_measurement).apply {
                    bindMeasurementViews(
                        currentDb = reading.currentDb,
                        peakDb = reading.peakDb,
                        duration = reading.duration,
                        noiseLevel = reading.noiseLevel,
                        includeLabel = false,
                    )
                }
            val expanded =
                RemoteViews(context.packageName, R.layout.notification_measurement_expanded).apply {
                    bindMeasurementViews(
                        currentDb = reading.currentDb,
                        peakDb = reading.peakDb,
                        duration = reading.duration,
                        noiseLevel = reading.noiseLevel,
                        includeLabel = true,
                    )
                    setTextViewText(
                        R.id.notification_session_name,
                        context.getString(recordingMode.liveMeasurementLabelRes),
                    )
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
            recordingMode: MeasurementRecordingMode = MeasurementRecordingMode.Meter,
            lockscreenVisibility: Int = NotificationCompat.VISIBILITY_PRIVATE,
        ): NotificationCompat.Builder = NotificationCompat
                .Builder(context, MEASUREMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(recordingMode.contentTitleRes))
                .setContentText(context.getString(recordingMode.contentTextRes, currentDb.toInt(), duration))
                .setContentIntent(measurementTapPendingIntent())
                .setVisibility(lockscreenVisibility)
                .setOngoing(true)
                .setSilent(true)
                .addAction(
                    R.drawable.ic_notification_stop,
                    context.getString(R.string.notification_action_stop),
                    measurementStopPendingIntent(),
                )

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

        private fun measurementStopPendingIntent(): PendingIntent {
            val intent = MeasurementForegroundService.stopIntent(context, emitCompleted = true)

            return PendingIntent.getService(
                context,
                MEASUREMENT_STOP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun buildAmbientSoundNotification(
            preset: AmbientSoundPreset,
            timerMinutes: Int,
            remainingMillis: Long?,
        ): Notification {
            val contentText =
                if (timerMinutes > 0 && remainingMillis != null) {
                    context.getString(
                        R.string.notification_ambient_sound_text_timer,
                        preset.label,
                        DurationFormatter.formatClockDuration(remainingMillis),
                    )
                } else {
                    context.getString(R.string.notification_ambient_sound_text, preset.label)
                }

            return NotificationCompat
                .Builder(context, AMBIENT_PLAYBACK_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.notification_ambient_sound_title))
                .setContentText(contentText)
                .setContentIntent(ambientTapPendingIntent())
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOngoing(true)
                .setSilent(true)
                .addAction(
                    R.drawable.ic_notification_stop,
                    context.getString(R.string.notification_action_stop),
                    ambientStopPendingIntent(),
                ).build()
        }

        private fun ambientTapPendingIntent(): PendingIntent {
            val intent =
                Intent()
                    .setClass(context, MainActivity::class.java)
                    .apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

            return PendingIntent.getActivity(
                context,
                AMBIENT_TAP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun ambientStopPendingIntent(): PendingIntent {
            val intent = AmbientSoundPlaybackService.stopIntent(context)

            return PendingIntent.getService(
                context,
                AMBIENT_STOP_REQUEST_CODE,
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
                setTextColor(R.id.notification_noise_label, noiseLevel.textColor)
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

        private val NotificationNoiseLevel.textColor: Int
            get() = ExternalBrand.noiseLevelArgb(noiseLevel)

        private val NotificationNoiseLevel.noiseLevel: NoiseLevel
            get() =
                when (this) {
                    NotificationNoiseLevel.SAFE -> NoiseLevel.QUIET
                    NotificationNoiseLevel.ELEVATED -> NoiseLevel.ELEVATED
                    NotificationNoiseLevel.DANGEROUS -> NoiseLevel.DANGEROUS
                }

        private val NotificationNoiseLevel.dotDrawableRes: Int
            get() =
                when (this) {
                    NotificationNoiseLevel.SAFE -> R.drawable.notification_dot_green
                    NotificationNoiseLevel.ELEVATED -> R.drawable.notification_dot_yellow
                    NotificationNoiseLevel.DANGEROUS -> R.drawable.notification_dot_red
                }

        private val MeasurementRecordingMode.contentTitleRes: Int
            get() =
                when (this) {
                    MeasurementRecordingMode.Meter -> R.string.notification_content_title
                    MeasurementRecordingMode.Sleep -> R.string.notification_sleep_content_title
                    MeasurementRecordingMode.Passive -> R.string.notification_passive_content_title
                }

        private val MeasurementRecordingMode.contentTextRes: Int
            get() =
                when (this) {
                    MeasurementRecordingMode.Meter -> R.string.notification_content_text
                    MeasurementRecordingMode.Sleep -> R.string.notification_sleep_content_text
                    MeasurementRecordingMode.Passive -> R.string.notification_passive_content_text
                }

        private val MeasurementRecordingMode.liveMeasurementLabelRes: Int
            get() =
                when (this) {
                    MeasurementRecordingMode.Meter -> R.string.notification_live_measurement
                    MeasurementRecordingMode.Sleep -> R.string.notification_sleep_live_measurement
                    MeasurementRecordingMode.Passive -> R.string.notification_passive_live_measurement
                }

        private val AmbientSoundPreset.label: String
            get() =
                when (this) {
                    AmbientSoundPreset.WHITE_NOISE -> context.getString(R.string.ambient_sound_preset_white_noise)
                    AmbientSoundPreset.PINK_NOISE -> context.getString(R.string.ambient_sound_preset_pink_noise)
                    AmbientSoundPreset.BROWN_NOISE -> context.getString(R.string.ambient_sound_preset_brown_noise)
                    AmbientSoundPreset.FAN -> context.getString(R.string.ambient_sound_preset_fan)
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

        fun sendVoiceVolumeWarning(currentDb: Float, baselineDb: Float): Boolean {
            if (!canPostRegularNotifications()) return false

            val notification =
                NotificationCompat
                    .Builder(context, ALERTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(R.string.notification_voice_volume_warning_title))
                    .setContentText(
                        context.getString(
                            R.string.notification_voice_volume_warning_text,
                            currentDb.toInt(),
                            baselineDb.toInt(),
                        ),
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

            return postNotification(VOICE_VOLUME_WARNING_NOTIFICATION_ID, notification)
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
