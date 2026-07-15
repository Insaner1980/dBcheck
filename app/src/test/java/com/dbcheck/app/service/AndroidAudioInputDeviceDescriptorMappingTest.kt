package com.dbcheck.app.service

import android.media.AudioDeviceInfo
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidAudioInputDeviceDescriptorMappingTest {
    @Test
    fun mapsBleCentralInputDeviceToBluetooth() {
        val device = mockk<AudioDeviceInfo>(relaxed = true)
        every { device.type } returns AudioDeviceInfo.TYPE_BLE_CENTRAL

        val descriptor = device.toAudioInputDeviceDescriptor()

        assertEquals(AudioInputDeviceType.BLUETOOTH, descriptor.type)
    }
}
