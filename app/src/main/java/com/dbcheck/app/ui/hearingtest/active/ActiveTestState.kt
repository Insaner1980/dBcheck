package com.dbcheck.app.ui.hearingtest.active

import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.TestKey

data class ActiveTestState(
    val currentPhase: Int = 1,
    val totalPhases: Int = 12,
    val currentEar: Ear = Ear.LEFT,
    val currentFrequency: Float = 1000f,
    val isPlayingTone: Boolean = false,
    val isSavingResult: Boolean = false,
    val isLocked: Boolean = false,
    val isComplete: Boolean = false,
    val completedTestId: Long? = null,
    val errorMessage: String? = null,
    val thresholds: Map<TestKey, Float> = emptyMap(),
)
