package com.dbcheck.app.ui.hearing

data class HearingScreenActions(
    val onNavigateToHearingTest: () -> Unit = {},
    val onNavigateToHearingRecovery: () -> Unit = {},
    val onNavigateToTinnitusPitch: () -> Unit = {},
    val onNavigateToAmbientSounds: () -> Unit = {},
    val onNavigateToSleepMonitor: () -> Unit = {},
    val onNavigateToUpgrade: () -> Unit = {},
)
