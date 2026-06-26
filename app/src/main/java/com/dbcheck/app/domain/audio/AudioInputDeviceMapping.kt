package com.dbcheck.app.domain.audio

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import java.util.Locale

data class AudioInputDeviceDescriptor(
    val id: Int,
    val type: Int,
    val productName: String?,
    val isSource: Boolean,
    val sampleRatesHz: IntArray = intArrayOf(),
    val channelCounts: IntArray = intArrayOf(),
)

data class AudioInputRouteResolution(
    val preferredDevice: AudioInputDeviceDescriptor?,
    val selectedDeviceId: Int?,
    val selectedDeviceName: String?,
)

object AudioInputDeviceMapper {
    fun map(descriptors: List<AudioInputDeviceDescriptor>): List<AudioInputDevice> = descriptors
            .asSequence()
            .filter { it.isSource }
            .map { it.toAudioInputDevice() }
            .sortedWith(audioInputDeviceComparator)
            .toList()
}

object AudioInputDeviceRouteResolver {
    fun resolve(preferredDeviceId: Int?, descriptors: List<AudioInputDeviceDescriptor>): AudioInputRouteResolution {
        val inputDescriptors = descriptors.filter { it.isSource }
        if (preferredDeviceId == null) {
            return AudioInputRouteResolution(
                preferredDevice = null,
                selectedDeviceId = null,
                selectedDeviceName = null,
            )
        }

        val builtInMicDescriptor =
            inputDescriptors.firstOrNull {
                it.type.toAudioInputDeviceType() == AudioInputDeviceType.BUILT_IN_MIC
            }
        val preferredDevice =
            inputDescriptors.firstOrNull { it.id == preferredDeviceId }
                ?: builtInMicDescriptor

        return AudioInputRouteResolution(
            preferredDevice = preferredDevice,
            selectedDeviceId = preferredDevice?.id,
            selectedDeviceName = preferredDevice?.displayName,
        )
    }
}

internal fun AudioDeviceInfo.toAudioInputDeviceDescriptor(): AudioInputDeviceDescriptor = AudioInputDeviceDescriptor(
        id = id,
        type = type,
        productName = productName?.toString(),
        isSource = isSource,
        sampleRatesHz = sampleRates,
        channelCounts = channelCounts,
    )

private val audioInputDeviceComparator =
    compareBy<AudioInputDevice> { it.type.sortPriority() }
        .thenBy { it.displayName.lowercase(Locale.ROOT) }
        .thenBy { it.id }

private fun AudioInputDeviceDescriptor.toAudioInputDevice(): AudioInputDevice {
    val deviceType = type.toAudioInputDeviceType()
    return AudioInputDevice(
        id = id,
        displayName = displayName,
        type = deviceType,
        isExternal = deviceType.isExternal,
        sampleRatesHz = sampleRatesHz.normalizedPositiveValues(),
        channelCounts = channelCounts.normalizedPositiveValues(),
    )
}

internal val AudioInputDeviceDescriptor.displayName: String
    get() = productName.normalizedDeviceName() ?: type.toAudioInputDeviceType().defaultDisplayName()

@SuppressLint("InlinedApi")
private fun Int.toAudioInputDeviceType(): AudioInputDeviceType = when (this) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> AudioInputDeviceType.BUILT_IN_MIC

        AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioInputDeviceType.WIRED_HEADSET

        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        -> AudioInputDeviceType.USB

        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_HEARING_AID,
        -> AudioInputDeviceType.BLUETOOTH

        else -> AudioInputDeviceType.OTHER
    }

private val AudioInputDeviceType.isExternal: Boolean
    get() = this != AudioInputDeviceType.BUILT_IN_MIC

private fun AudioInputDeviceType.defaultDisplayName(): String = when (this) {
        AudioInputDeviceType.BUILT_IN_MIC -> "Built-in microphone"
        AudioInputDeviceType.WIRED_HEADSET -> "Wired headset microphone"
        AudioInputDeviceType.USB -> "USB microphone"
        AudioInputDeviceType.BLUETOOTH -> "Bluetooth microphone"
        AudioInputDeviceType.OTHER -> "Audio input"
    }

private fun AudioInputDeviceType.sortPriority(): Int = when (this) {
        AudioInputDeviceType.USB -> 0
        AudioInputDeviceType.BLUETOOTH -> 1
        AudioInputDeviceType.WIRED_HEADSET -> 2
        AudioInputDeviceType.OTHER -> 3
        AudioInputDeviceType.BUILT_IN_MIC -> 4
    }

private fun String?.normalizedDeviceName(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

private fun IntArray.normalizedPositiveValues(): List<Int> = asSequence()
        .filter { it > 0 }
        .distinct()
        .sorted()
        .toList()
