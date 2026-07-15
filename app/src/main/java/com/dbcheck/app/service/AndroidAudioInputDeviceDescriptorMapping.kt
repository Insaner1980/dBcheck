package com.dbcheck.app.service

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import com.dbcheck.app.domain.audio.AudioInputDeviceDescriptor
import com.dbcheck.app.domain.audio.AudioInputDeviceType

internal fun AudioDeviceInfo.toAudioInputDeviceDescriptor(): AudioInputDeviceDescriptor = AudioInputDeviceDescriptor(
        id = id,
        type = type.toAudioInputDeviceType(),
        productName = productName?.toString(),
        isSource = isSource,
        sampleRatesHz = sampleRates,
        channelCounts = channelCounts,
    )

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
        AudioDeviceInfo.TYPE_BLE_CENTRAL,
        AudioDeviceInfo.TYPE_HEARING_AID,
        -> AudioInputDeviceType.BLUETOOTH

        else -> AudioInputDeviceType.OTHER
    }
