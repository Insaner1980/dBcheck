package com.dbcheck.app.ui.hearingtest.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.data.local.db.entity.HearingTestResultEntity
import com.dbcheck.app.domain.audio.ToneGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveTestViewModel
    @Inject
    constructor(
        private val toneGenerator: ToneGenerator,
        private val hearingTestDao: HearingTestDao,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ActiveTestState())
        val state: StateFlow<ActiveTestState> = _state

        // Modified Hughson-Westlake adaptive procedure
        // Rule: descend 10 dB when heard, ascend 5 dB when not heard
        // Threshold: quietest level heard on 2 ascending trials
        private var currentAmplitudeDb = -30f // Starting level (dBFS)
        private var ascendingCount = 0 // Ascending "heard" responses
        private var lastResponseHeard = false
        private val thresholds = mutableMapOf<TestKey, Float>()

        // Test sequence: 6 frequencies x 2 ears = 12 phases
        private val testSequence: List<Pair<Ear, Float>> =
            buildList {
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

            // An ascending response = "heard" after one or more "not heard" responses
            if (!lastResponseHeard) {
                ascendingCount++
            }
            lastResponseHeard = true

            if (ascendingCount >= 2) {
                // Threshold found: quietest level heard on 2 ascending trials
                val key = TestKey(testSequence[currentIndex].first, testSequence[currentIndex].second)
                thresholds[key] = currentAmplitudeDb
                advanceToNextPhase()
            } else {
                // Descend: make quieter by 10 dB
                currentAmplitudeDb -= 10f
                if (currentAmplitudeDb < -60f) {
                    // Floor reached — excellent hearing, record this threshold
                    val key = TestKey(testSequence[currentIndex].first, testSequence[currentIndex].second)
                    thresholds[key] = -60f
                    advanceToNextPhase()
                } else {
                    playCurrentTone()
                }
            }
        }

        fun onNotHeard() {
            toneGenerator.stop()
            lastResponseHeard = false

            // Ascend: make louder by 5 dB
            currentAmplitudeDb += 5f
            if (currentAmplitudeDb > 0f) {
                // Can't hear even at max — record 0 dBFS as threshold (significant hearing loss)
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
            val leftData =
                thresholds
                    .filter { it.key.ear == Ear.LEFT }
                    .entries
                    .joinToString(",") { "${it.key.frequencyHz}:${it.value}" }
            val rightData =
                thresholds
                    .filter { it.key.ear == Ear.RIGHT }
                    .entries
                    .joinToString(",") { "${it.key.frequencyHz}:${it.value}" }

            val allThresholds = thresholds.values.toList()
            val avgThreshold = if (allThresholds.isNotEmpty()) allThresholds.average().toFloat() else 0f

            // Score: more negative threshold = better hearing
            // -60 dBFS (excellent) → 100, 0 dBFS (can't hear) → 0
            val normalizedThreshold = (-avgThreshold).coerceIn(0f, 60f)
            val overallScore = ((normalizedThreshold / 60f) * 100).toInt()

            val rating =
                when {
                    overallScore >= 90 -> "Excellent"
                    overallScore >= 75 -> "Good"
                    overallScore >= 50 -> "Fair"
                    else -> "Poor"
                }

            val result =
                HearingTestResultEntity(
                    timestamp = System.currentTimeMillis(),
                    overallScore = overallScore,
                    rating = rating,
                    leftEarData = leftData,
                    rightEarData = rightData,
                    speechClarity = (overallScore.toFloat() / 100f * 98f).coerceIn(0f, 100f),
                    highFreqLimit = 17400f, // Simplified — would need actual measurement
                    avgThreshold = avgThreshold,
                )

            return hearingTestDao.insertResult(result)
        }

        override fun onCleared() {
            super.onCleared()
            toneGenerator.stop()
        }
    }
