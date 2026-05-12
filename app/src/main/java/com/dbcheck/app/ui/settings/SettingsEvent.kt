package com.dbcheck.app.ui.settings

sealed interface SettingsEvent {
    data object RestartAfterRestore : SettingsEvent
}
