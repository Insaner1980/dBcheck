package com.dbcheck.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientSoundPlaybackServicePolicyTest {
    @Test
    fun startRequiresProUserActionAndNotificationPermission() {
        assertFalse(
            AmbientSoundPlaybackServicePolicy.canStartPlayback(
                isProUser = false,
                requestedByUser = true,
                notificationPermissionGranted = true,
            ),
        )
        assertFalse(
            AmbientSoundPlaybackServicePolicy.canStartPlayback(
                isProUser = true,
                requestedByUser = false,
                notificationPermissionGranted = true,
            ),
        )
        assertFalse(
            AmbientSoundPlaybackServicePolicy.canStartPlayback(
                isProUser = true,
                requestedByUser = true,
                notificationPermissionGranted = false,
            ),
        )
        assertTrue(
            AmbientSoundPlaybackServicePolicy.canStartPlayback(
                isProUser = true,
                requestedByUser = true,
                notificationPermissionGranted = true,
            ),
        )
    }

    @Test
    fun startRequestNormalizesPlaybackOptions() {
        val intent =
            mockk<Intent> {
                every { action } returns AmbientSoundPlaybackService.ACTION_START_AMBIENT_SOUND
                every {
                    getStringExtra(AmbientSoundPlaybackService.EXTRA_PRESET)
                } returns AmbientSoundPreset.BROWN_NOISE.preferenceValue
                every {
                    getFloatExtra(
                        AmbientSoundPlaybackService.EXTRA_VOLUME,
                        AmbientSoundPlaybackServicePolicy.DEFAULT_START_REQUEST.volume,
                    )
                } returns 2f
                every {
                    getIntExtra(
                        AmbientSoundPlaybackService.EXTRA_TIMER_MINUTES,
                        AmbientSoundPlaybackServicePolicy.DEFAULT_START_REQUEST.timerMinutes,
                    )
                } returns 120
                every {
                    getBooleanExtra(
                        AmbientSoundPlaybackService.EXTRA_REQUESTED_BY_USER,
                        false,
                    )
                } returns true
            }

        assertEquals(
            AmbientSoundStartRequest(
                preset = AmbientSoundPreset.BROWN_NOISE,
                volume = 1f,
                timerMinutes = 120,
                requestedByUser = true,
            ),
            AmbientSoundPlaybackServicePolicy.startRequest(intent),
        )
    }

    @Test
    fun stopRequestParsesOnlyStopAction() {
        val stopIntent =
            mockk<Intent> {
                every { action } returns AmbientSoundPlaybackService.ACTION_STOP_AMBIENT_SOUND
            }
        val startIntent =
            mockk<Intent> {
                every { action } returns AmbientSoundPlaybackService.ACTION_START_AMBIENT_SOUND
            }

        assertEquals(AmbientSoundStopRequest, AmbientSoundPlaybackServicePolicy.stopRequest(stopIntent))
        assertNull(AmbientSoundPlaybackServicePolicy.stopRequest(startIntent))
    }

    @Test
    fun timerOnlyStopsAlreadyStartedPlayback() {
        assertFalse(
            AmbientSoundPlaybackServicePolicy.shouldStopForTimer(
                playbackStartedAtMs = null,
                nowMs = 120L * 60_000L,
                timerMinutes = 120,
            ),
        )
        assertFalse(
            AmbientSoundPlaybackServicePolicy.shouldStopForTimer(
                playbackStartedAtMs = 0L,
                nowMs = 10L * 60_000L,
                timerMinutes = 0,
            ),
        )
        assertFalse(
            AmbientSoundPlaybackServicePolicy.shouldStopForTimer(
                playbackStartedAtMs = 0L,
                nowMs = 119L * 60_000L,
                timerMinutes = 120,
            ),
        )
        assertTrue(
            AmbientSoundPlaybackServicePolicy.shouldStopForTimer(
                playbackStartedAtMs = 0L,
                nowMs = 120L * 60_000L,
                timerMinutes = 120,
            ),
        )
    }

    @Test
    fun notificationLoopRunsOnlyWhenTimerCanChangeItsContent() {
        assertFalse(AmbientSoundPlaybackServicePolicy.shouldUpdateNotification(timerMinutes = 0))
        assertTrue(AmbientSoundPlaybackServicePolicy.shouldUpdateNotification(timerMinutes = 30))
    }

    @Test
    fun mediaPlaybackForegroundTypeIsUsedOnlyFromAndroid11() {
        assertEquals(0, AmbientSoundPlaybackServicePolicy.foregroundServiceType(Build.VERSION_CODES.Q))
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            AmbientSoundPlaybackServicePolicy.foregroundServiceType(Build.VERSION_CODES.R),
        )
    }

    @Test
    fun successfulStartResultIsNotSticky() {
        assertEquals(Service.START_NOT_STICKY, AmbientSoundPlaybackServicePolicy.successStartResult)
    }
}
