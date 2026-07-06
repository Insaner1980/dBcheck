package com.dbcheck.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.di.MainDispatcher
import com.dbcheck.app.domain.ambient.AmbientSoundPolicy
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AmbientSoundPlaybackService : Service() {
    companion object {
        const val ACTION_START_AMBIENT_SOUND = "com.dbcheck.app.action.START_AMBIENT_SOUND"
        const val ACTION_STOP_AMBIENT_SOUND = "com.dbcheck.app.action.STOP_AMBIENT_SOUND"
        const val EXTRA_PRESET = "com.dbcheck.app.extra.AMBIENT_SOUND_PRESET"
        const val EXTRA_VOLUME = "com.dbcheck.app.extra.AMBIENT_SOUND_VOLUME"
        const val EXTRA_TIMER_MINUTES = "com.dbcheck.app.extra.AMBIENT_SOUND_TIMER_MINUTES"
        const val EXTRA_REQUESTED_BY_USER = "com.dbcheck.app.extra.AMBIENT_SOUND_REQUESTED_BY_USER"
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1_000L
        private const val MINUTE_MS = 60_000L

        fun startIntent(
            context: Context,
            preset: AmbientSoundPreset,
            volume: Float,
            timerMinutes: Int,
            requestedByUser: Boolean,
        ): Intent = Intent(context, AmbientSoundPlaybackService::class.java).apply {
                action = ACTION_START_AMBIENT_SOUND
                putExtra(EXTRA_PRESET, preset.preferenceValue)
                putExtra(EXTRA_VOLUME, AmbientSoundPolicy.normalizeVolume(volume))
                putExtra(EXTRA_TIMER_MINUTES, AmbientSoundPolicy.normalizeTimerMinutes(timerMinutes))
                putExtra(EXTRA_REQUESTED_BY_USER, requestedByUser)
            }

        fun stopIntent(context: Context): Intent = Intent(context, AmbientSoundPlaybackService::class.java).apply {
                action = ACTION_STOP_AMBIENT_SOUND
            }
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var ambientSoundPlayer: AmbientSoundPlayer

    @Inject
    lateinit var playbackController: AmbientSoundPlaybackController

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var serviceScope: CoroutineScope
    private lateinit var audioManager: AudioManager
    private var updateJob: Job? = null
    private var currentRequest: AmbientSoundStartRequest? = null
    private var playbackStartedAtMs: Long? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pausedForTransientFocus = false

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + mainDispatcher)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationHelper.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = when {
            AmbientSoundPlaybackServicePolicy.stopRequest(intent) != null -> {
                stopCurrentPlayback()
                stopSelf(startId)
                AmbientSoundPlaybackServicePolicy.successStartResult
            }

            else -> {
                val request = AmbientSoundPlaybackServicePolicy.startRequest(intent)
                serviceScope.launch {
                    startPlaybackIfAllowed(request, startId)
                }
                AmbientSoundPlaybackServicePolicy.successStartResult
            }
        }

    private suspend fun startPlaybackIfAllowed(request: AmbientSoundStartRequest, startId: Int) {
        val prefs = preferencesRepository.userPreferences.first()
        val allowed =
            AmbientSoundPlaybackServicePolicy.canStartPlayback(
                isProUser = prefs.isProUser,
                requestedByUser = request.requestedByUser,
                notificationPermissionGranted = notificationHelper.canPostPlaybackNotification(),
            )
        if (!allowed) {
            stopSelf(startId)
            return
        }

        stopCurrentPlayback()
        val timerMillis = request.timerMillisOrNull()
        val notification =
            notificationHelper.buildAmbientSoundNotification(
                preset = request.preset,
                timerMinutes = request.timerMinutes,
                remainingMillis = timerMillis,
            )
        val foregroundStarted = startPlaybackForeground(notification)
        val focusGranted = foregroundStarted && requestAudioFocus()
        val playbackStarted = focusGranted && ambientSoundPlayer.play(request.preset, request.volume)

        if (!playbackStarted) {
            stopCurrentPlayback()
            stopSelf(startId)
            return
        }

        currentRequest = request
        playbackStartedAtMs = System.currentTimeMillis()
        playbackController.markPlaying()
        startNotificationLoop()
    }

    private fun startPlaybackForeground(notification: android.app.Notification): Boolean = runCatching {
        ServiceCompat.startForeground(
            this,
            NotificationHelper.AMBIENT_SOUND_NOTIFICATION_ID,
            notification,
            AmbientSoundPlaybackServicePolicy.foregroundServiceType(Build.VERSION.SDK_INT),
        )
    }.isSuccess

    private fun requestAudioFocus(): Boolean {
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAudioAttributes())
                .setOnAudioFocusChangeListener(::onAudioFocusChange)
                .build()
        audioFocusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> stopSelf()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> {
                pausedForTransientFocus = true
                ambientSoundPlayer.pause()
            }

            AudioManager.AUDIOFOCUS_GAIN ->
                if (pausedForTransientFocus) {
                    pausedForTransientFocus = false
                    ambientSoundPlayer.resume()
                }
        }
    }

    private fun playbackAudioAttributes(): AudioAttributes = AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private fun startNotificationLoop() {
        updateJob?.cancel()
        updateJob =
            serviceScope.launch {
                while (isActive) {
                    delay(NOTIFICATION_UPDATE_INTERVAL_MS)
                    val request = currentRequest ?: return@launch
                    val startedAtMs = playbackStartedAtMs
                    val nowMs = System.currentTimeMillis()
                    if (
                        AmbientSoundPlaybackServicePolicy.shouldStopForTimer(
                            playbackStartedAtMs = startedAtMs,
                            nowMs = nowMs,
                            timerMinutes = request.timerMinutes,
                        )
                    ) {
                        stopSelf()
                        return@launch
                    }
                    updatePlaybackNotification(request, startedAtMs, nowMs)
                }
            }
    }

    private fun updatePlaybackNotification(request: AmbientSoundStartRequest, startedAtMs: Long?, nowMs: Long) {
        val remainingMillis =
            startedAtMs?.let { started ->
                request.timerMillisOrNull()?.let { timerMillis ->
                    (timerMillis - (nowMs - started)).coerceAtLeast(0L)
                }
            }
        notificationHelper.updateNotification(
            NotificationHelper.AMBIENT_SOUND_NOTIFICATION_ID,
            notificationHelper.buildAmbientSoundNotification(
                preset = request.preset,
                timerMinutes = request.timerMinutes,
                remainingMillis = remainingMillis,
            ),
        )
    }

    private fun stopCurrentPlayback() {
        updateJob?.cancel()
        updateJob = null
        currentRequest = null
        playbackStartedAtMs = null
        pausedForTransientFocus = false
        ambientSoundPlayer.stop()
        abandonAudioFocus()
        playbackController.markStopped()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCurrentPlayback()
        if (::serviceScope.isInitialized) {
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    private fun AmbientSoundStartRequest.timerMillisOrNull(): Long? = timerMinutes
            .takeIf { it > 0 }
            ?.let { it * MINUTE_MS }
}

internal data class AmbientSoundStartRequest(
    val preset: AmbientSoundPreset = AmbientSoundPolicy.DEFAULT_PRESET,
    val volume: Float = AmbientSoundPolicy.DEFAULT_VOLUME,
    val timerMinutes: Int = AmbientSoundPolicy.DEFAULT_TIMER_MINUTES,
    val requestedByUser: Boolean = false,
)

internal data object AmbientSoundStopRequest

internal object AmbientSoundPlaybackServicePolicy {
    val successStartResult: Int = Service.START_NOT_STICKY
    val DEFAULT_START_REQUEST = AmbientSoundStartRequest()

    fun canStartPlayback(
        isProUser: Boolean,
        requestedByUser: Boolean,
        notificationPermissionGranted: Boolean,
    ): Boolean = isProUser && requestedByUser && notificationPermissionGranted

    fun startRequest(intent: Intent?): AmbientSoundStartRequest = AmbientSoundStartRequest(
            preset =
                AmbientSoundPolicy.normalizePreset(
                    intent?.getStringExtra(AmbientSoundPlaybackService.EXTRA_PRESET),
                ),
            volume =
                AmbientSoundPolicy.normalizeVolume(
                    intent?.getFloatExtra(
                        AmbientSoundPlaybackService.EXTRA_VOLUME,
                        DEFAULT_START_REQUEST.volume,
                    ),
                ),
            timerMinutes =
                AmbientSoundPolicy.normalizeTimerMinutes(
                    intent?.getIntExtra(
                        AmbientSoundPlaybackService.EXTRA_TIMER_MINUTES,
                        DEFAULT_START_REQUEST.timerMinutes,
                    ),
                ),
            requestedByUser =
                intent?.getBooleanExtra(
                    AmbientSoundPlaybackService.EXTRA_REQUESTED_BY_USER,
                    false,
                ) ?: false,
        )

    fun stopRequest(intent: Intent?): AmbientSoundStopRequest? = intent
            ?.takeIf { it.action == AmbientSoundPlaybackService.ACTION_STOP_AMBIENT_SOUND }
            ?.let { AmbientSoundStopRequest }

    fun shouldStopForTimer(playbackStartedAtMs: Long?, nowMs: Long, timerMinutes: Int): Boolean =
        playbackStartedAtMs != null &&
            timerMinutes > 0 &&
            nowMs - playbackStartedAtMs >= timerMinutes * 60_000L

    @SuppressLint("InlinedApi")
    fun foregroundServiceType(sdkInt: Int): Int = if (sdkInt >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
}
