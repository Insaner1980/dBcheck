package com.dbcheck.app.ui.hearingtest.results

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.util.ShareResultsGenerator
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val isLoading: Boolean = true,
    val isProUser: Boolean = false,
    val isResultMissing: Boolean = false,
    val resultId: Long? = null,
    val overallScore: Int = 0,
    val rating: String = "",
    val leftEarThresholds: List<Pair<Float, Float>> = emptyList(),
    val rightEarThresholds: List<Pair<Float, Float>> = emptyList(),
    val avgThreshold: Float = 0f,
    val speechClarity: Float = 0f,
    val highFreqLimit: Float = 0f,
    val shareErrorMessage: String? = null,
)

@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val hearingTestRepository: HearingTestRepository,
        private val preferencesRepository: PreferencesRepository,
        private val shareResultsGenerator: ShareResultsGenerator,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ResultsUiState())
        val state: StateFlow<ResultsUiState> = _state
        private val _shareIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val shareIntents: SharedFlow<Intent> = _shareIntents.asSharedFlow()
        private val testId =
            savedStateHandle.get<Long>(Screen.HearingTestResults.ARG_TEST_ID)
                ?: savedStateHandle.get<String>(Screen.HearingTestResults.ARG_TEST_ID)?.toLongOrNull()

        init {
            loadResult()
        }

        private fun loadResult() {
            viewModelScope.launch {
                val resultFlow =
                    testId
                        ?.let(hearingTestRepository::getResultById)
                        ?: hearingTestRepository.getLatestResult()

                combine(resultFlow, preferencesRepository.userPreferences) { result, prefs ->
                    result to prefs.isProUser
                }.collect { (result, isProUser) ->
                    if (!isProUser) {
                        _state.value =
                            ResultsUiState(
                                isLoading = false,
                                isProUser = false,
                                shareErrorMessage = PRO_REQUIRED_MESSAGE,
                            )
                    } else if (result != null) {
                        _state.value =
                            ResultsUiState(
                                isLoading = false,
                                isProUser = true,
                                resultId = result.id,
                                overallScore = result.overallScore,
                                rating = result.rating,
                                leftEarThresholds = result.leftEarThresholds,
                                rightEarThresholds = result.rightEarThresholds,
                                avgThreshold = result.avgThreshold,
                                speechClarity = result.speechClarity,
                                highFreqLimit = result.highFreqLimit,
                                shareErrorMessage = null,
                            )
                    } else {
                        _state.value =
                            ResultsUiState(
                                isLoading = false,
                                isProUser = true,
                                isResultMissing = true,
                            )
                    }
                }
            }
        }

        fun createShareIntent() {
            val current = _state.value
            if (current.isLoading) {
                _state.value = current.copy(shareErrorMessage = "Hearing test result is still loading")
                return
            }
            if (!current.isProUser) {
                _state.value = current.copy(shareErrorMessage = PRO_REQUIRED_MESSAGE)
                return
            }
            if (current.rating.isBlank()) {
                _state.value = current.copy(shareErrorMessage = "No hearing test result to share")
                return
            }

            viewModelScope.launch {
                runCatching {
                    shareResultsGenerator.shareHearingTestResults(
                        score = current.overallScore,
                        rating = current.rating,
                    )
                }.onSuccess { intent ->
                    _state.value = _state.value.copy(shareErrorMessage = null)
                    _shareIntents.emit(intent)
                }.onFailure { error ->
                    _state.value =
                        _state.value.copy(
                            shareErrorMessage =
                                error.toUserFacingMessage("Unable to share hearing test results"),
                        )
                }
            }
        }

        fun onShareUnavailable() {
            _state.value = _state.value.copy(shareErrorMessage = "No app available to share results")
        }

        fun clearShareError() {
            _state.value = _state.value.copy(shareErrorMessage = null)
        }

        private companion object {
            const val PRO_REQUIRED_MESSAGE = "Hearing test requires dBcheck Pro"
        }
    }
