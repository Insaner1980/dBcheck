package com.dbcheck.app.service

import android.media.AudioDeviceInfo
import android.media.AudioRecord
import com.dbcheck.app.domain.audio.AndroidAudioInputDeviceRouter
import com.dbcheck.app.domain.audio.AndroidAudioInputRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAudioInputDeviceRouterTest {
    @Test
    fun applyPreferredDeviceCallsAudioRecordSetPreferredDevice() {
        val audioRecord = mockk<AudioRecord>()
        val router = AndroidAudioInputDeviceRouter(context = mockk(relaxed = true))
        val deviceInfo =
            mockk<AudioDeviceInfo> {
                every { id } returns USB_DEVICE_ID
                every { productName } returns "USB-C microphone"
            }
        every { audioRecord.setPreferredDevice(deviceInfo) } returns true

        val applied =
            router.applyPreferredDevice(
                audioRecord = audioRecord,
                preferredDevice = AndroidAudioInputRoute(deviceInfo),
            )

        assertTrue(applied)
        verify(exactly = 1) { audioRecord.setPreferredDevice(deviceInfo) }
    }

    private companion object {
        const val USB_DEVICE_ID = 12
    }
}
