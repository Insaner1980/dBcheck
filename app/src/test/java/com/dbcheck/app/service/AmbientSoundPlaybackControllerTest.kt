package com.dbcheck.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientSoundPlaybackControllerTest {
    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkObject(AmbientSoundPlaybackService.Companion)
    }

    @Test
    fun rejectedStartDoesNotPublishPlayingState() {
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        mockkStatic(ContextCompat::class)
        mockkObject(AmbientSoundPlaybackService.Companion)
        every { AmbientSoundPlaybackService.startIntent(context, any(), any(), any(), any()) } returns intent
        every { ContextCompat.startForegroundService(context, intent) } throws SecurityException("denied")
        val controller = AmbientSoundPlaybackController(context)

        assertThrows(SecurityException::class.java) {
            controller.startPlayback(AmbientSoundPlaybackRequest(AmbientSoundPreset.BROWN_NOISE, 0.5f, 30))
        }

        assertFalse(controller.isPlaying.value)
    }

    @Test
    fun rejectedStopDoesNotClaimPlaybackHasStopped() {
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        mockkObject(AmbientSoundPlaybackService.Companion)
        every { AmbientSoundPlaybackService.stopIntent(context) } returns intent
        every { context.startService(intent) } throws IllegalStateException("background")
        val controller = AmbientSoundPlaybackController(context)
        controller.markPlaying()

        assertThrows(IllegalStateException::class.java) { controller.stopPlayback() }

        assertTrue(controller.isPlaying.value)
    }
}
