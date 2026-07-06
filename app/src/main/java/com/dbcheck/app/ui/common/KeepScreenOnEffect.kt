package com.dbcheck.app.ui.common

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun KeepScreenOnEffect(enabled: Boolean, window: Window?) {
    DisposableEffect(enabled, window) {
        val controller = window?.let(::KeepScreenOnController)
        controller?.update(enabled)
        onDispose {
            controller?.release()
        }
    }
}

internal class KeepScreenOnController(private val window: Window) {
    private var isKeepingScreenOn = false

    fun update(enabled: Boolean) {
        if (enabled == isKeepingScreenOn) return

        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        isKeepingScreenOn = enabled
    }

    fun release() {
        if (!isKeepingScreenOn) return

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isKeepingScreenOn = false
    }
}
