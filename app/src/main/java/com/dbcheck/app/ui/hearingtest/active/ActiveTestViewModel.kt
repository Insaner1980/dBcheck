package com.dbcheck.app.ui.hearingtest.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.db.entity.HearingTestResultEntity
import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.domain.audio.ToneGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveTestViewModel @Inject constructor(
    private val toneGenerator: ToneGenerator,
    private val hearingTestDao: HearingTestDao,
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveTestState())
    val state: StateFlow<ActiveTestState> = _state

    // Modified Hughson-Westlake: start at moderate, decrease until unheard, increase until heard
    private var currentAmplitudeDb = -30f // Starting level (moderate)
    private var stepSize = 10f
    private var ascendingCount = 0
    private var lastResponseHeard = false
    private val thresholds = mutableMapOf<TestKey, Float>()

    // Test sequence: 6 frequencies x 2 ears = 12 phases
    private val testSequence: List<Pair<Ear, Float>> = buildList {
        TEST_FREQUENCIES.forEach { freq ->
            add(Ear.LEFT to freq)
        }
        TEST_FREQUENCIES.forEach { freq ->
            add(Ear.RIGHT to freq)
        }
    }

    private var currentIndex = 0

    fun startTest() {
        currentIndex = 0
        resetForNewFrequency()
        updatePhaseState()
        playCurrentTone()
    }

    fun onHeard() {
        toneGenerator.stop()

        if (!lastResponseHeard && currentAmplitudeDb <= -20f) {
            ascendingCount++
        }
        lastResponseHeard = true

        if (ascendingCount >= 2) {
            // Threshold found
            val key = TestKey(testSequence[currentIndex].first, testSequence[currentIndex].second)
            thresholds[key] = currentAmplitudeDb
            advanceToNextPhase()
        } else {
            // Decrease level (make quieter)
            currentAmplitudeDb -= stepSize
            if (stepSize > 5f) stepSize = 5f
            playCurrentTone()
        }
    }

    fun onNotHeard() {
        toneGenerator.stop()
        lastResponseHeard = false

        // Increase level (make louder)
        currentAmplitudeDb += stepSize
        if (currentAmplitudeDb > 0f) {
            // Can't hear even at max - record threshold as 0 dB
            val key = TestKey(testSequence[currentIndex].first, testSequence[currentIndex].second)
            thresholds[key] = 0f
            advanceToNextPhase()
        } else {
            playCurrentTone()
        }
    }

    private fun advanceToNextPhase() {
        currentIndex++
        if (currentIndex >= testSequence.size) {
            // Test complete
            _state.update { it.copy(isComplete = true, thresholds = thresholds.toMap()) }
            viewModelScope.launch { saveResults() }
        } else {
            resetForNewFrequency()
            updatePhaseState()
            playCurrentTone()
        }
    }

    private fun resetForNewFrequency() {
        currentAmplitudeDb = -30f
        stepSize = 10f
        ascendingCount = 0
        lastResponseHeard = false
    }

    private fun updatePhaseState() {
        val (ear, freq) = testSequence[currentIndex]
        _state.update {
            it.copy(
                currentPhase = currentIndex + 1,
                currentEar = ear,
                currentFrequency = freq,
            )
        }
    }

    private fun playCurrentTone() {
        _state.update { it.copy(isPlayingTone = true) }
        viewModelScope.launch {
            delay(500) // Brief pause before tone
            toneGenerator.playTone(
                frequencyHz = testSequence[currentIndex].second,
                amplitudeDb = currentAmplitudeDb,
            )
            delay(1500) // Tone duration
            _state.update { it.copy(isPlayingTone = false) }
        }
    }

    private suspend fun saveResults(): Long {
        val leftData = thresholds.filter { it.key.ear == Ear.LEFT }
            .entries.joinToString(",") { "${it.key.frequencyHz}:${it.value}" }
        val rightData = thresholds.filter { it.key.ear == Ear.RIGHT }
            .entries.joinToString(",") { "${it.key.frequencyHz}:${it.value}" }

        val allThresholds = thresholds.values.toList()
        val avgThreshold = if (allThresholds.isNotEmpty()) allThresholds.average().toFloat() else 0f
        val overallScore = ((1f - (avgThreshold / -60f).coerceIn(0f, 1f)) * 100).toInt()

        val rating = when {
            overallScore >= 90 -> "Excellent"
            overallScore >= 75 -> "Good"
            overallScore >= 50 -> "Fair"
            else -> "Poor"
        }

        val result = HearingTestResultEntity(
            timestamp = System.currentTimeMillis(),
            overallScore = overallScore,
            rating = rating,
            leftEarData = leftData,
            rightEarData = rightData,
            speechClarity = (overallScore.toFloat() / 100f * 98f).coerceIn(0f, 100f),
            highFreqLimit = 17400f, // Simplified - would need actual measurement
            avgThreshold = avgThreshold,
        )

        return hearingTestDao.insertResult(result)
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator.stop()
    }
}
