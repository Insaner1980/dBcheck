package com.dbcheck.app.ui.hearingtest.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.db.dao.HearingTestDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val overallScore: Int = 0,
    val rating: String = "",
    val leftEarThresholds: List<Pair<Float, Float>> = emptyList(),
    val rightEarThresholds: List<Pair<Float, Float>> = emptyList(),
    val avgThreshold: Float = 0f,
    val speechClarity: Float = 0f,
    val highFreqLimit: Float = 0f,
)

@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        private val hearingTestDao: HearingTestDao,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ResultsUiState())
        val state: StateFlow<ResultsUiState> = _state

        init {
            loadLatestResult()
        }

        private fun loadLatestResult() {
            viewModelScope.launch {
                hearingTestDao.getLatestResult().collect { result ->
                    if (result != null) {
                        val leftData = parseEarData(result.leftEarData)
                        val rightData = parseEarData(result.rightEarData)

                        _state.value =
                            ResultsUiState(
                                overallScore = result.overallScore,
                                rating = result.rating,
                                leftEarThresholds = leftData,
                                rightEarThresholds = rightData,
                                avgThreshold = result.avgThreshold,
                                speechClarity = result.speechClarity,
                                highFreqLimit = result.highFreqLimit,
                            )
                    }
                }
            }
        }

        private fun parseEarData(data: String): List<Pair<Float, Float>> =
            data
                .split(",")
                .filter { it.contains(":") }
                .map { entry ->
                    val (freq, threshold) = entry.split(":")
                    freq.toFloat() to threshold.toFloat()
                }.sortedBy { it.first }
    }
