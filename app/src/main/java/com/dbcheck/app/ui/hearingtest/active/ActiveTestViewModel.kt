package com.dbcheck.app.ui.hearingtest.active

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.HearingTestMode
import com.dbcheck.app.domain.hearingtest.HearingTestPolicy
import com.dbcheck.app.domain.hearingtest.HearingTestProcedure
import com.dbcheck.app.domain.hearingtest.HearingTestProgress
import com.dbcheck.app.domain.hearingtest.HearingTestStepResult
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.service.HearingRecoveryService
import com.dbcheck.app.service.HearingTestService
import com.dbcheck.app.service.ToneGenerator
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
        @param:ApplicationContext private val context: Context,
        private val toneGenerator: ToneGenerator,
        private val hearingTestService: HearingTestService,
        private val hearingRecoveryService: HearingRecoveryService,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ActiveTestState())
        val state: StateFlow<ActiveTestState> = _state

        private var procedure: HearingTestProcedure? = null
        private var activeMode = HearingTestMode.FULL
        private var hasStarted = false
        private var toneJob: Job? = null

        init {
            observeProEntitlement()
        }

        private fun observeProEntitlement() {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { prefs ->
                    if (!prefs.isProUser) {
                        lockStartedTest()
                    }
                }
            }
        }

        private fun lockStartedTest() {
            if (!hasStarted || _state.value.isLocked) return

            cancelTonePlayback()
            toneGenerator.stop()
            _state.update {
                it.copy(
                    isPlayingTone = false,
                    isSavingResult = false,
                    isLocked = true,
                    errorMessage = context.getString(R.string.hearing_test_pro_required),
                )
            }
        }

        fun startTest(mode: HearingTestMode = HearingTestMode.FULL) {
            if (hasStarted) return

            viewModelScope.launch {
                if (!preferencesRepository.userPreferences.first().isProUser) {
                    _state.update {
                        it.copy(
                            isPlayingTone = false,
                            isSavingResult = false,
                            isLocked = true,
                            errorMessage = context.getString(R.string.hearing_test_pro_required),
                        )
                    }
                    return@launch
                }
                if (hasStarted) return@launch

                hasStarted = true
                activeMode = mode
                procedure = HearingTestProcedure(frequencies = mode.frequencies)
                _state.update { it.copy(isLocked = false, errorMessage = null) }
                val progress = requireNotNull(procedure).start()
                updatePhaseState(progress)
                playCurrentTone(progress)
            }
        }

        fun onHeard() {
            if (_state.value.isLocked || _state.value.isSavingResult || _state.value.isComplete) return

            cancelTonePlayback()
            toneGenerator.stop()
            handleStep(procedure?.onHeard() ?: return)
        }

        fun onNotHeard() {
            if (_state.value.isLocked || _state.value.isSavingResult || _state.value.isComplete) return

            cancelTonePlayback()
            toneGenerator.stop()
            handleStep(procedure?.onNotHeard() ?: return)
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
                            isComplete = true,
                            thresholds = result.thresholds,
                            errorMessage = null,
                        )
                    }
                    saveCompletedThresholds(result.thresholds)
                }
            }
        }

        fun retrySaveResult() {
            val current = _state.value
            if (current.isLocked || !current.canRetrySave) return

            saveCompletedThresholds(current.thresholds)
        }

        private fun saveCompletedThresholds(thresholds: Map<TestKey, Float>) {
            viewModelScope.launch {
                _state.update { it.copy(isSavingResult = true, errorMessage = null) }
                runCatching { saveResults(thresholds) }
                    .onSuccess { testId ->
                        _state.update {
                            it.copy(
                                isSavingResult = false,
                                completedTestId = testId,
                                errorMessage = null,
                            )
                        }
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        _state.update {
                            it.copy(
                                isSavingResult = false,
                                errorMessage =
                                    error.toUserFacingMessage(
                                        context.getString(R.string.hearing_error_save_failed),
                                    ),
                            )
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
            _state.update { it.copy(isPlayingTone = true) }
            toneJob = viewModelScope.launch {
                runCatching {
                    delay(HearingTestPolicy.TONE_START_DELAY_MS)
                    toneGenerator.playTone(
                        frequencyHz = progress.currentFrequency,
                        amplitudeDb = progress.amplitudeDb,
                    )
                    delay(HearingTestPolicy.TONE_DURATION_MS)
                }.onSuccess {
                    _state.update { it.copy(isPlayingTone = false) }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    _state.update {
                        it.copy(
                            isPlayingTone = false,
                            errorMessage =
                                error.toUserFacingMessage(
                                    context.getString(R.string.hearing_error_tone_playback_failed),
                                ),
                        )
                    }
                }
            }
        }

        private fun cancelTonePlayback() {
            toneJob?.cancel()
            toneJob = null
            _state.update { it.copy(isPlayingTone = false) }
        }

        private suspend fun saveResults(thresholds: Map<TestKey, Float>): Long = when (activeMode) {
                HearingTestMode.FULL -> hearingTestService.saveCompletedTest(thresholds)
                HearingTestMode.RECOVERY -> hearingRecoveryService.saveCompletedRecoveryCheck(thresholds)
            }

        override fun onCleared() {
            toneJob?.cancel()
            toneGenerator.stop()
        }
    }
