package com.dbcheck.app.domain.audio

data class SoundDetectionEvent(val sessionId: Long, val timestamp: Long, val label: String, val confidence: Float)
