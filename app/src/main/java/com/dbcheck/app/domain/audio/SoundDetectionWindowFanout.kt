package com.dbcheck.app.domain.audio

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class SoundDetectionWindowFanout
    @Inject
    constructor() {
        private val adapter = YamnetAudioWindowAdapter()
        private val _windows =
            MutableSharedFlow<FloatArray>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        val windows: SharedFlow<FloatArray> = _windows.asSharedFlow()

        @Volatile
        private var enabled = false

        fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
            if (!enabled) {
                adapter.reset()
            }
        }

        fun processPcm16(buffer: ShortArray, size: Int) {
            if (!enabled) return
            adapter.appendPcm16(buffer, size)?.let { window ->
                _windows.tryEmit(window)
            }
        }
    }
