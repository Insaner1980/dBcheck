package com.dbcheck.app.service

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import com.dbcheck.app.domain.audio.AudioInputDeviceRouteResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AudioInputDeviceRouter {
    fun resolvePreferredDevice(preferredDeviceId: Int?): ResolvedAudioInputDeviceRoute

    fun applyPreferredDevice(audioRecord: AudioRecord, preferredDevice: AudioInputRoute?): Boolean

    fun routedDeviceName(audioRecord: AudioRecord): String?
}

data class ResolvedAudioInputDeviceRoute(
    val preferredDevice: AudioInputRoute?,
    val selectedDeviceId: Int?,
    val selectedDeviceName: String?,
)

interface AudioInputRoute {
    val id: Int
    val displayName: String
}

data class AndroidAudioInputRoute(val deviceInfo: AudioDeviceInfo) : AudioInputRoute {
    override val id: Int = deviceInfo.id
    override val displayName: String =
        deviceInfo.productName
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Audio input"
}

@Singleton
class AndroidAudioInputDeviceRouter
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : AudioInputDeviceRouter {
        override fun resolvePreferredDevice(preferredDeviceId: Int?): ResolvedAudioInputDeviceRoute {
            val devices = inputDevices()
            val resolution =
                AudioInputDeviceRouteResolver.resolve(
                    preferredDeviceId = preferredDeviceId,
                    descriptors = devices.map { it.toAudioInputDeviceDescriptor() },
                )
            val route =
                resolution.preferredDevice
                    ?.let { descriptor -> devices.firstOrNull { it.id == descriptor.id } }
                    ?.let(::AndroidAudioInputRoute)

            return ResolvedAudioInputDeviceRoute(
                preferredDevice = route,
                selectedDeviceId = resolution.selectedDeviceId,
                selectedDeviceName = resolution.selectedDeviceName,
            )
        }

        override fun applyPreferredDevice(audioRecord: AudioRecord, preferredDevice: AudioInputRoute?): Boolean =
            preferredDevice == null ||
                (
                    preferredDevice is AndroidAudioInputRoute &&
                        runCatching {
                            audioRecord.setPreferredDevice(preferredDevice.deviceInfo)
                        }.getOrDefault(false)
                )

        override fun routedDeviceName(audioRecord: AudioRecord): String? = runCatching {
            audioRecord.routedDevice
                ?.productName
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()

        private fun inputDevices(): List<AudioDeviceInfo> = context
                .getSystemService(AudioManager::class.java)
                ?.getDevices(AudioManager.GET_DEVICES_INPUTS)
                ?.filter { it.isSource }
                .orEmpty()
    }
