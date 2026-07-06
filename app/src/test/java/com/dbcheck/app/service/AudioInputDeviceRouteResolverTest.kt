package com.dbcheck.app.service

import com.dbcheck.app.domain.audio.AudioInputDeviceDescriptor
import com.dbcheck.app.domain.audio.AudioInputDeviceRouteResolver
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioInputDeviceRouteResolverTest {
    @Test
    fun selectedExternalDeviceIsResolvedWhenConnected() {
        val route =
            AudioInputDeviceRouteResolver.resolve(
                preferredDeviceId = USB_DEVICE_ID,
                descriptors =
                    listOf(
                        inputDescriptor(BUILT_IN_DEVICE_ID, AudioInputDeviceType.BUILT_IN_MIC, "Phone microphone"),
                        inputDescriptor(USB_DEVICE_ID, AudioInputDeviceType.USB, "USB-C microphone"),
                    ),
            )

        assertEquals(USB_DEVICE_ID, route.preferredDevice?.id)
        assertEquals(USB_DEVICE_ID, route.selectedDeviceId)
        assertEquals("USB-C microphone", route.selectedDeviceName)
    }

    @Test
    fun missingSelectedExternalDeviceFallsBackToBuiltInMicrophone() {
        val route =
            AudioInputDeviceRouteResolver.resolve(
                preferredDeviceId = USB_DEVICE_ID,
                descriptors =
                    listOf(
                        inputDescriptor(BUILT_IN_DEVICE_ID, AudioInputDeviceType.BUILT_IN_MIC, "Phone microphone"),
                    ),
            )

        assertEquals(BUILT_IN_DEVICE_ID, route.preferredDevice?.id)
        assertEquals(BUILT_IN_DEVICE_ID, route.selectedDeviceId)
        assertEquals("Phone microphone", route.selectedDeviceName)
    }

    @Test
    fun noSelectedDeviceLeavesRoutingToSystemDefault() {
        val route =
            AudioInputDeviceRouteResolver.resolve(
                preferredDeviceId = null,
                descriptors =
                    listOf(
                        inputDescriptor(BUILT_IN_DEVICE_ID, AudioInputDeviceType.BUILT_IN_MIC, "Phone microphone"),
                    ),
            )

        assertNull(route.preferredDevice)
        assertNull(route.selectedDeviceId)
        assertNull(route.selectedDeviceName)
    }

    private fun inputDescriptor(id: Int, type: AudioInputDeviceType, productName: String) = AudioInputDeviceDescriptor(
            id = id,
            type = type,
            productName = productName,
            isSource = true,
        )

    private companion object {
        const val BUILT_IN_DEVICE_ID = 1
        const val USB_DEVICE_ID = 12
    }
}
