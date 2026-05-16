package com.dbcheck.app.ui.hearingtest.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.ToneGenerator
import com.dbcheck.app.domain.audio.ToneOutputChannel
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.HearingTestProcedure
import com.dbcheck.app.domain.hearingtest.HearingTestProgress
import com.dbcheck.app.domain.hearingtest.HearingTestStepResult
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.service.HearingTestService
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
        private var tonePlaybackJob: Job? = null
        private var startRequested = false

        fun startTest() {
            if (startRequested) return
            startRequested = true
            viewModelScope.launch {
                cancelTonePlayback()
                if (!preferencesRepository.userPreferences.first().isProUser) {
                    startRequested = false
                    _state.update {
                        it.copy(
                            isPlayingTone = false,
                            canRespond = false,
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
            if (!canAcceptResponse()) return

            cancelTonePlayback()
            handleStep(procedure.onHeard())
        }

        fun onNotHeard() {
            if (!canAcceptResponse()) return

            cancelTonePlayback()
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
                            canRespond = false,
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
            cancelTonePlayback()
            tonePlaybackJob =
                viewModelScope.launch {
                    delay(TONE_START_DELAY_MS)
                    runCatching {
                        toneGenerator.playTone(
                            frequencyHz = progress.currentFrequency,
                            amplitudeDb = progress.amplitudeDb,
                            outputChannel = progress.currentEar.toToneOutputChannel(),
                        )
                    }.onSuccess {
                        _state.update {
                            it.copy(
                                isPlayingTone = true,
                                canRespond = true,
                                errorMessage = null,
                            )
                        }
                        delay(TONE_DURATION_MS)
                        _state.update { it.copy(isPlayingTone = false) }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                isPlayingTone = false,
                                canRespond = false,
                                errorMessage = error.toUserFacingMessage(TONE_PLAYBACK_ERROR_MESSAGE),
                            )
                        }
                    }
                }
        }

        private fun canAcceptResponse(): Boolean {
            val state = _state.value
            return state.canRespond && !state.isLocked && !state.isSavingResult && !state.isComplete
        }

        private fun cancelTonePlayback() {
            tonePlaybackJob?.cancel()
            tonePlaybackJob = null
            toneGenerator.stop()
            _state.update {
                it.copy(
                    isPlayingTone = false,
                    canRespond = false,
                )
            }
        }

        private suspend fun saveResults(thresholds: Map<TestKey, Float>): Long = hearingTestService.run {
            saveCompletedTest(thresholds)
        }

        private fun Ear.toToneOutputChannel(): ToneOutputChannel = when (this) {
            Ear.LEFT -> ToneOutputChannel.LEFT
            Ear.RIGHT -> ToneOutputChannel.RIGHT
        }

        override fun onCleared() {
            super.onCleared()
            tonePlaybackJob?.cancel()
            tonePlaybackJob = null
            toneGenerator.stop()
        }

        private companion object {
            const val PRO_REQUIRED_MESSAGE = "Hearing test requires dBcheck Pro"
            const val TONE_PLAYBACK_ERROR_MESSAGE = "Unable to play hearing test tone"
            const val TONE_START_DELAY_MS = 500L
            const val TONE_DURATION_MS = 1500L
        }
    }
