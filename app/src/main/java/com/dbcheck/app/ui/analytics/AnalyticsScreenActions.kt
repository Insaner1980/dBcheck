package com.dbcheck.app.ui.analytics

data class AnalyticsScreenActions(
    val onNavigateToMeter: () -> Unit = {},
    val onNavigateToSettings: () -> Unit = {},
    val onNavigateToHearingTest: () -> Unit = {},
    val onNavigateToHearingRecoveryCheck: () -> Unit = {},
    val onNavigateToTinnitusPitch: () -> Unit = {},
    val onNavigateToAmbientSound: () -> Unit = {},
    val onNavigateToSleepSetup: () -> Unit = {},
    val onNavigateToUpgrade: () -> Unit = {},
)
