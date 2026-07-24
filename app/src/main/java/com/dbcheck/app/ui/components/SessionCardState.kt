package com.dbcheck.app.ui.components

import androidx.compose.runtime.Immutable

@Immutable
data class SessionCardState(
    val emoji: String,
    val title: String,
    val metadata: String,
    val peakDb: Float,
    val avgDb: Float,
    val tags: List<String> = emptyList(),
    val isSleepSession: Boolean = false,
)
