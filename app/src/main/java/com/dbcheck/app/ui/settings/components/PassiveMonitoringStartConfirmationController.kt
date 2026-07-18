package com.dbcheck.app.ui.settings.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class PassiveMonitoringStartConfirmationController {
    var isOpen by mutableStateOf(false)
        private set
    private var confirmationPending = false

    fun request() {
        confirmationPending = true
        isOpen = true
    }

    fun confirm(onConfirmedStart: () -> Unit) {
        if (!confirmationPending) return
        close()
        onConfirmedStart()
    }

    fun cancel() {
        close()
    }

    fun dismiss() {
        close()
    }

    private fun close() {
        confirmationPending = false
        isOpen = false
    }
}
