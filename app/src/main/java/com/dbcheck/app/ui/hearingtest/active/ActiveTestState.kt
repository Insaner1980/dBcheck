package com.dbcheck.app.ui.hearingtest.active

data class ActiveTestState(
    val currentPhase: Int = 1,
    val totalPhases: Int = 12,
    val currentEar: Ear = Ear.LEFT,
    val currentFrequency: Float = 1000f,
    val isPlayingTone: Boolean = false,
    val isComplete: Boolean = false,
    val thresholds: Map<TestKey, Float> = emptyMap(),
)

enum class Ear(
    val label: String,
) {
    LEFT("LEFT EAR ONLY"),
    RIGHT("RIGHT EAR ONLY"),
}

data class TestKey(
    val ear: Ear,
    val frequencyHz: Float,
)

val TEST_FREQUENCIES = listOf(250f, 500f, 1000f, 2000f, 4000f, 8000f)
