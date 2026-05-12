package com.dbcheck.app.ui.hearingtest.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.ToneGenerator
import com.dbcheck.app.domain.hearingtest.HearingTestProcedure
import com.dbcheck.app.domain.hearingtest.HearingTestProgress
import com.dbcheck.app.domain.hearingtest.HearingTestStepResult
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.service.HearingTestService
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveTestViewModel
    @Inject
    constructor(
        private val toneGenerator: ToneGenerator,
        private val hearingTestService: HearingTestService,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ActiveTestState())
        val state: StateFlow<ActiveTestState> = _state

        private val procedure = HearingTestProcedure()

        fun startTest() {
            viewModelScope.launch {
                if (!preferencesRepository.userPreferences.first().isProUser) {
                    _state.update {
                        it.copy(
                            isPlayingTone = false,
                            isSavingResult = false,
                            isLocked = true,
                            errorMessage = PRO_REQUIRED_MESSAGE,
                        )
                    }
                    return@launch
                }

                _state.update { it.copy(isLocked = false, errorMessage = null) }
                val progress = procedure.start()
                updatePhaseState(progress)
                playCurrentTone(progress)
            }
        }

        fun onHeard() {
            if (_state.value.isLocked || _state.value.isSavingResult || _state.value.isComplete) return

            toneGenerator.stop()
            handleStep(procedure.onHeard())
        }

        fun onNotHeard() {
            if (_state.value.isLocked || _state.value.isSavingResult || _state.value.isComplete) return

            toneGenerator.stop()
            handleStep(procedure.onNotHeard())
        }

        private fun handleStep(result: HearingTestStepResult) {
            when (result) {
                is HearingTestStepResult.Continue -> {
                    updatePhaseState(result.progress)
                    playCurrentTone(result.progress)
                }

                is HearingTestStepResult.Completed -> {
                    _state.update {
                        it.copy(
                            isPlayingTone = false,
                            isSavingResult = true,
                            thresholds = result.thresholds,
                        )
                    }
                    viewModelScope.launch {
                        runCatching { saveResults(result.thresholds) }
                            .onSuccess { testId ->
                                _state.update {
                                    it.copy(
                                        isSavingResult = false,
                                        isComplete = true,
                                        completedTestId = testId,
                                        errorMessage = null,
                                    )
                                }
                            }.onFailure { error ->
                                _state.update {
                                    it.copy(
                                        isSavingResult = false,
                                        errorMessage =
                                            error.toUserFacingMessage("Unable to save hearing test result"),
                                    )
                                }
                            }
                    }
                }
            }
        }

        private fun updatePhaseState(progress: HearingTestProgress) {
            _state.update {
                it.copy(
                    currentPhase = progress.currentPhase,
                    totalPhases = progress.totalPhases,
                    currentEar = progress.currentEar,
                    currentFrequency = progress.currentFrequency,
                )
            }
        }

        private fun playCurrentTone(progress: HearingTestProgress) {
            _state.update { it.copy(isPlayingTone = true) }
            viewModelScope.launch {
                delay(500) // Brief pause before tone
                toneGenerator.playTone(
                    frequencyHz = progress.currentFrequency,
                    amplitudeDb = progress.amplitudeDb,
                )
                delay(1500) // Tone duration
                _state.update { it.copy(isPlayingTone = false) }
            }
        }

        private suspend fun saveResults(thresholds: Map<TestKey, Float>): Long =
            hearingTestService.saveCompletedTest(thresholds)

        override fun onCleared() {
            super.onCleared()
            toneGenerator.stop()
        }

        private companion object {
            const val PRO_REQUIRED_MESSAGE = "Hearing test requires dBcheck Pro"
        }
    }
