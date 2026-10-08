package com.dbcheck.app.service

import android.app.Application
import android.app.Notification
import android.content.Intent
import android.media.AudioManager
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
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
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
            verify(exactly = 0) { player.play(any(), any()) }
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
            verify(exactly = 0) { player.play(AmbientSoundPreset.BROWN_NOISE, any()) }
            verify(exactly = 1) { player.play(AmbientSoundPreset.WHITE_NOISE, any()) }
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
        service.notificationHelper = mockk {
            every { buildAmbientSoundNotification(any(), any(), any()) } returns mockk<Notification>()
            every { canPostPlaybackNotification() } returns true
        }
        service.preferencesRepository = mockk<PreferencesRepository> {
            every { userPreferences } returns prefs
        }
        service.ambientSoundPlayer = mockk(relaxed = true) {
            every { play(any(), any()) } returns true
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

    private fun startIntent(preset: AmbientSoundPreset): Intent = Intent()
        .setAction(AmbientSoundPlaybackService.ACTION_START_AMBIENT_SOUND)
        .putExtra(AmbientSoundPlaybackService.EXTRA_PRESET, preset.preferenceValue)
        .putExtra(AmbientSoundPlaybackService.EXTRA_REQUESTED_BY_USER, true)
}
