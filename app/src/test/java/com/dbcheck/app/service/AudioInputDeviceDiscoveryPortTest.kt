package com.dbcheck.app.service

import android.media.AudioDeviceInfo
import com.dbcheck.app.domain.audio.AudioInputDeviceDescriptor
import com.dbcheck.app.domain.audio.AudioInputDeviceMapper
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioInputDeviceDiscoveryPortTest {
    @Test
    fun mapperKeepsOnlyInputSourcesAndNormalizesDeviceMetadata() {
        val devices =
            AudioInputDeviceMapper.map(
                listOf(
                    AudioInputDeviceDescriptor(
                        id = USB_DEVICE_ID,
                        type = AudioDeviceInfo.TYPE_USB_DEVICE,
                        productName = "  USB-C mic  ",
                        isSource = true,
                        sampleRatesHz = intArrayOf(48_000, 0, 44_100, 48_000),
                        channelCounts = intArrayOf(2, 1, 0, 2),
                    ),
                    AudioInputDeviceDescriptor(
                        id = BUILT_IN_DEVICE_ID,
                        type = AudioDeviceInfo.TYPE_BUILTIN_MIC,
                        productName = "",
                        isSource = true,
                    ),
                    AudioInputDeviceDescriptor(
                        id = SPEAKER_DEVICE_ID,
                        type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                        productName = "Speaker",
                        isSource = false,
                    ),
                ),
            )

        assertEquals(listOf(USB_DEVICE_ID, BUILT_IN_DEVICE_ID), devices.map { it.id })
        assertEquals("USB-C mic", devices.first().displayName)
        assertEquals(AudioInputDeviceType.USB, devices.first().type)
        assertTrue(devices.first().isExternal)
        assertEquals(listOf(44_100, 48_000), devices.first().sampleRatesHz)
        assertEquals(listOf(1, 2), devices.first().channelCounts)
        assertEquals("Built-in microphone", devices.last().displayName)
        assertEquals(AudioInputDeviceType.BUILT_IN_MIC, devices.last().type)
        assertFalse(devices.last().isExternal)
    }

    @Test
    fun mapperClassifiesBluetoothAndWiredInputDevicesAsExternal() {
        val devices =
            AudioInputDeviceMapper.map(
                listOf(
                    AudioInputDeviceDescriptor(
                        id = BLUETOOTH_DEVICE_ID,
                        type = AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        productName = "Headset mic",
                        isSource = true,
                    ),
                    AudioInputDeviceDescriptor(
                        id = WIRED_DEVICE_ID,
                        type = AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        productName = "Wired headset",
                        isSource = true,
                    ),
                ),
            )

        assertEquals(
            listOf(AudioInputDeviceType.BLUETOOTH, AudioInputDeviceType.WIRED_HEADSET),
            devices.map { it.type },
        )
        assertTrue(devices.all { it.isExternal })
    }

    private companion object {
        const val BUILT_IN_DEVICE_ID = 1
        const val USB_DEVICE_ID = 2
        const val SPEAKER_DEVICE_ID = 3
        const val BLUETOOTH_DEVICE_ID = 4
        const val WIRED_DEVICE_ID = 5
    }
}
