package com.dbcheck.app.ui.analytics

data class AnalyticsScreenActions(
    val onNavigateToMeter: () -> Unit = {},
    val onNavigateToHearing: () -> Unit = {},
    val onNavigateToUpgrade: () -> Unit = {},
)
