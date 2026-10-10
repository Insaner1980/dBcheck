package com.dbcheck.app.service

import android.app.Application
import android.app.Notification
import android.content.Intent
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class AmbientSoundPlaybackServiceStartTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun obsoleteCallbacksCannotStopNewPlaybackAndCurrentFailurePublishesStopped() = runTest {
        val prefs = MutableSharedFlow<UserPreferences>()
        val service = service(prefs)
        val player = service.ambientSoundPlayer
        val controller = service.playbackController
        val callbacks = mutableListOf<() -> Unit>()
        every { player.play(any(), any(), any()) } answers {
            callbacks += thirdArg<() -> Unit>()
            true
        }
        val focusField = AmbientSoundPlaybackService::class.java.getDeclaredField("audioFocusRequest")
            .apply { isAccessible = true }
        try {
            service.onStartCommand(startIntent(AmbientSoundPreset.BROWN_NOISE), 0, 1)
            runCurrent()
            prefs.emit(UserPreferences(isProUser = true))
            runCurrent()
            val oldFocus = focusField.get(service) as AudioFocusRequest
            val oldFocusListener = AudioFocusRequest::class.java.getDeclaredMethod("getOnAudioFocusChangeListener")
                .invoke(oldFocus) as AudioManager.OnAudioFocusChangeListener
            service.onStartCommand(startIntent(AmbientSoundPreset.WHITE_NOISE), 0, 2)
            runCurrent()
            prefs.emit(UserPreferences(isProUser = true))
            runCurrent()

            callbacks.first().invoke()
            oldFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
            runCurrent()
            verify(exactly = 0) { service.stopSelf(any()) }
            verify(exactly = 0) { service.stopSelf() }
            verify(exactly = 2) { controller.markStopped() }

            callbacks.last().invoke()
            runCurrent()
            verify(exactly = 1) { service.stopSelf(2) }
            verify(exactly = 3) { controller.markStopped() }
        } finally {
            service.onDestroy()
            unmockkStatic(ServiceCompat::class)
        }
    }

    @Test
    fun timerUsesElapsedTimeAndNewPlaybackCancelsTheOldDeadline() = runTest {
        var nowMs = 1_000L
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers { nowMs }
        val prefs = MutableSharedFlow<UserPreferences>()
        val service = service(prefs)
        val controller = service.playbackController
        try {
            service.onStartCommand(startIntent(AmbientSoundPreset.BROWN_NOISE, 15), 0, 1)
            runCurrent()
            prefs.emit(UserPreferences(isProUser = true))
            runCurrent()
            val startedAt = AmbientSoundPlaybackService::class.java.getDeclaredField("playbackStartedAtMs")
                .apply { isAccessible = true }
            assertEquals(nowMs, startedAt.get(service))

            nowMs += 14 * 60_000L
            service.onStartCommand(startIntent(AmbientSoundPreset.WHITE_NOISE, 30), 0, 2)
            runCurrent()
            prefs.emit(UserPreferences(isProUser = true))
            runCurrent()
            assertEquals(nowMs, startedAt.get(service))
            nowMs += 60_000L
            advanceTimeBy(1_000L)
            runCurrent()
            verify(exactly = 0) { service.stopSelf() }

            nowMs += 29 * 60_000L
            advanceTimeBy(1_000L)
            runCurrent()
            verify(exactly = 1) { service.stopSelf() }
            verify(exactly = 3) { controller.markStopped() }
        } finally {
            service.onDestroy()
            unmockkStatic(SystemClock::class)
            unmockkStatic(ServiceCompat::class)
        }
    }

    @Test
    fun permanentFocusLossClearsTransientResumeBeforeDestruction() = runTest {
        val service = service(MutableSharedFlow())
        val player = service.ambientSoundPlayer
        val controller = service.playbackController
        val focusChange = AmbientSoundPlaybackService::class.java.getDeclaredMethod(
            "onAudioFocusChange", Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }
        try {
            focusChange.invoke(service, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            focusChange.invoke(service, AudioManager.AUDIOFOCUS_LOSS)
            focusChange.invoke(service, AudioManager.AUDIOFOCUS_GAIN)

            verify(exactly = 1) { player.pause() }
            verify(exactly = 1) { player.stop() }
            verify(exactly = 0) { player.resume() }
            verify(exactly = 1) { controller.markStopped() }
        } finally {
            service.onDestroy()
            unmockkStatic(ServiceCompat::class)
        }
    }

    @Test
    fun foregroundStartsBeforePreferencesAndStopCancelsPendingPlayback() = runTest {
        val prefs = MutableSharedFlow<UserPreferences>()
        val service = service(prefs)
        val player = service.ambientSoundPlayer
        try {
            service.onStartCommand(startIntent(AmbientSoundPreset.BROWN_NOISE), 0, 1)
            verify(exactly = 1) { ServiceCompat.startForeground(service, any(), any(), any()) }
            runCurrent()
            service.onStartCommand(Intent().setAction(AmbientSoundPlaybackService.ACTION_STOP_AMBIENT_SOUND), 0, 2)
            prefs.emit(UserPreferences(isProUser = true))
            runCurrent()
            verify(exactly = 0) { player.play(any(), any(), any()) }
        } finally {
            service.onDestroy()
            unmockkStatic(ServiceCompat::class)
        }
    }

    @Test
    fun newStartSupersedesPendingRequest() = runTest {
        val prefs = MutableSharedFlow<UserPreferences>()
        val service = service(prefs)
        val player = service.ambientSoundPlayer
        try {
            service.onStartCommand(startIntent(AmbientSoundPreset.BROWN_NOISE), 0, 1)
            runCurrent()
            service.onStartCommand(startIntent(AmbientSoundPreset.WHITE_NOISE), 0, 2)
            runCurrent()
            prefs.emit(UserPreferences(isProUser = true))
            runCurrent()
            verify(exactly = 0) { player.play(AmbientSoundPreset.BROWN_NOISE, any(), any()) }
            verify(exactly = 1) { player.play(AmbientSoundPreset.WHITE_NOISE, any(), any()) }
        } finally {
            service.onDestroy()
            unmockkStatic(ServiceCompat::class)
        }
    }

    private fun service(prefs: MutableSharedFlow<UserPreferences>): AmbientSoundPlaybackService {
        mockkStatic(ServiceCompat::class)
        every { ServiceCompat.startForeground(any(), any(), any(), any()) } just runs
        every { ServiceCompat.stopForeground(any(), any()) } just runs
        val service = spyk(AmbientSoundPlaybackService())
        every { service.stopSelf(any()) } just runs
        every { service.stopSelf() } just runs
        service.notificationHelper = mockk {
            every { buildAmbientSoundNotification(any(), any(), any()) } returns mockk<Notification>()
            every { canPostPlaybackNotification() } returns true
            every { updateNotification(any(), any()) } just runs
        }
        service.preferencesRepository = mockk<PreferencesRepository> {
            every { userPreferences } returns prefs
        }
        service.ambientSoundPlayer = mockk(relaxed = true) {
            every { play(any(), any(), any()) } returns true
        }
        service.playbackController = mockk(relaxed = true)
        val manager = mockk<AudioManager>(relaxed = true) {
            every { requestAudioFocus(any<android.media.AudioFocusRequest>()) } returns
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        AmbientSoundPlaybackService::class.java.getDeclaredField("audioManager").apply {
            isAccessible = true
            set(service, manager)
        }
        AmbientSoundPlaybackService::class.java.getDeclaredField("serviceScope").apply {
            isAccessible = true
            set(service, CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.Main))
        }
        return service
    }

    private fun startIntent(preset: AmbientSoundPreset, timerMinutes: Int = 0): Intent = Intent()
        .setAction(AmbientSoundPlaybackService.ACTION_START_AMBIENT_SOUND)
        .putExtra(AmbientSoundPlaybackService.EXTRA_PRESET, preset.preferenceValue)
        .putExtra(AmbientSoundPlaybackService.EXTRA_REQUESTED_BY_USER, true)
        .putExtra(AmbientSoundPlaybackService.EXTRA_TIMER_MINUTES, timerMinutes)
}
