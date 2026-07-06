package com.dbcheck.app.service

import android.content.Context
import android.media.AudioManager
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.audio.AudioInputDevice
import com.dbcheck.app.domain.audio.AudioInputDeviceMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

fun interface AudioInputDeviceDiscoveryPort {
    suspend fun listInputDevices(): List<AudioInputDevice>
}

@Singleton
class AndroidAudioInputDeviceDiscoveryPort
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AudioInputDeviceDiscoveryPort {
        override suspend fun listInputDevices(): List<AudioInputDevice> = withContext(ioDispatcher) {
            val audioManager = context.getSystemService(AudioManager::class.java)
                ?: return@withContext emptyList()
            val descriptors =
                audioManager
                    .getDevices(AudioManager.GET_DEVICES_INPUTS)
                    .map { it.toAudioInputDeviceDescriptor() }

            AudioInputDeviceMapper.map(descriptors)
        }
    }
