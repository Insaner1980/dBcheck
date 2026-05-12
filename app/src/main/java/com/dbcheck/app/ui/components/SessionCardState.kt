package com.dbcheck.app.ui.components

data class SessionCardState(
    val emoji: String,
    val title: String,
    val metadata: String,
    val peakDb: Float,
    val avgDb: Float,
    val tags: List<String> = emptyList(),
)
