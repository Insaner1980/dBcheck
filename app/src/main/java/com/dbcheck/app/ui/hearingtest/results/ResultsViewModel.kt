package com.dbcheck.app.ui.hearingtest.results

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.util.ShareResultsGenerator
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
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
    val loadErrorMessage: String? = null,
    val shareErrorMessage: String? = null,
)

@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
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
            savedStateHandle
                .get<Any?>(Screen.HearingTestResults.ARG_TEST_ID)
                .toHearingTestRouteId()

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
                }.catch { error ->
                    if (error is CancellationException) throw error
                    _state.value =
                        ResultsUiState(
                            isLoading = false,
                            isProUser = true,
                            loadErrorMessage =
                                error.toUserFacingMessage(
                                    context.getString(R.string.hearing_error_load_failed),
                                ),
                        )
                }.collect { (result, isProUser) ->
                    if (!isProUser) {
                        _state.value =
                            ResultsUiState(
                                isLoading = false,
                                isProUser = false,
                                shareErrorMessage = context.getString(R.string.hearing_test_pro_required),
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
            val validationError =
                when {
                    current.isLoading -> context.getString(R.string.hearing_error_result_loading)
                    !current.isProUser -> context.getString(R.string.hearing_test_pro_required)
                    current.rating.isBlank() -> context.getString(R.string.hearing_error_no_result_to_share)
                    else -> null
                }

            if (validationError != null) {
                _state.value = current.copy(shareErrorMessage = validationError)
            } else {
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
                        if (error is CancellationException) throw error
                        _state.value =
                            _state.value.copy(
                                shareErrorMessage =
                                    error.toUserFacingMessage(
                                        context.getString(R.string.hearing_error_share_failed),
                                    ),
                            )
                    }
                }
            }
        }

        fun onShareUnavailable() {
            _state.value = _state.value.copy(
                shareErrorMessage = context.getString(R.string.hearing_error_no_share_app),
            )
        }

        fun clearShareError() {
            _state.value = _state.value.copy(shareErrorMessage = null)
        }
    }

private fun Any?.toHearingTestRouteId(): Long? = when (this) {
    is Long -> this
    is Int -> toLong()
    is String -> toLongOrNull()
    else -> null
}
