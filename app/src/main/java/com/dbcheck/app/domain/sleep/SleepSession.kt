package com.dbcheck.app.domain.sleep

data class SleepSession(
    val sessionId: Long,
    val targetDurationMinutes: Int,
    val keepAwakeEnabled: Boolean,
    val createdAt: Long,
)
