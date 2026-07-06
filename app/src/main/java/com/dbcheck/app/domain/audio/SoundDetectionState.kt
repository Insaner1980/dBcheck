package com.dbcheck.app.domain.audio

data class SoundDetection(val label: String, val confidence: Float, val timestamp: Long)

data class SoundDetectionState(
    val isEnabled: Boolean = false,
    val current: SoundDetection? = null,
    val recentDetections: List<SoundDetection> = emptyList(),
    val error: SoundDetectionError? = null,
)

enum class SoundDetectionError {
    CLASSIFICATION_UNAVAILABLE,
}

fun SoundDetectionState.withClassification(
    classification: SoundClassification?,
    timestamp: Long,
    maxRecentDetections: Int = MAX_RECENT_SOUND_DETECTIONS,
): SoundDetectionState {
    if (classification == null) {
        return copy(isEnabled = true, current = null, error = null)
    }
    val detection =
        SoundDetection(
            label = classification.label,
            confidence = classification.confidence,
            timestamp = timestamp,
        )
    return SoundDetectionState(
        isEnabled = true,
        current = detection,
        recentDetections = (listOf(detection) + recentDetections).take(maxRecentDetections),
        error = null,
    )
}

fun SoundDetectionState.withError(error: SoundDetectionError): SoundDetectionState = copy(
        isEnabled = true,
        current = null,
        error = error,
    )

const val MAX_RECENT_SOUND_DETECTIONS = 5
