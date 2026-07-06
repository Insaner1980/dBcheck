package com.dbcheck.app.ui.tinnitus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.tinnitus.TinnitusPitchPolicy
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.service.ToneGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TinnitusPitchMatcherUiState(
    val selectedEar: Ear = Ear.LEFT,
    val currentFrequencyHz: Float = TinnitusPitchPolicy.DEFAULT_FREQUENCY_HZ,
    val leftFrequencyHz: Float? = null,
    val rightFrequencyHz: Float? = null,
    val updatedAtMs: Long? = null,
    val isProUser: Boolean = false,
    val errorMessage: String? = null,
    val saveMessage: String? = null,
    val title: String = "",
    val description: String = "",
    val disclaimer: String = "",
) {
    val isLocked: Boolean
        get() = !isProUser

    val hasSavedProfile: Boolean
        get() = leftFrequencyHz != null || rightFrequencyHz != null
}

@HiltViewModel
class TinnitusPitchMatcherViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val preferencesRepository: PreferencesRepository,
        private val toneGenerator: ToneGenerator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(baseState())
        val uiState: StateFlow<TinnitusPitchMatcherUiState> = _uiState

        init {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { prefs ->
                    val visibleProfile =
                        if (prefs.isProUser) {
                            prefs.tinnitusPitchProfile
                        } else {
                            toneGenerator.stop()
                            TinnitusPitchProfile()
                        }
                    _uiState.update { state ->
                        val selectedFrequency =
                            visibleProfile.frequencyFor(state.selectedEar)
                                ?: state.currentFrequencyHz
                        state.copy(
                            currentFrequencyHz = TinnitusPitchPolicy.normalizeFrequencyHz(selectedFrequency),
                            leftFrequencyHz = visibleProfile.leftFrequencyHz,
                            rightFrequencyHz = visibleProfile.rightFrequencyHz,
                            updatedAtMs = visibleProfile.updatedAtMs,
                            isProUser = prefs.isProUser,
                        )
                    }
                }
            }
        }

        fun selectEar(ear: Ear) {
            _uiState.update { state ->
                state.copy(
                    selectedEar = ear,
                    currentFrequencyHz =
                        state.profile().frequencyFor(ear)
                            ?: state.currentFrequencyHz,
                    errorMessage = null,
                    saveMessage = null,
                )
            }
        }

        fun updateFrequency(frequencyHz: Float) {
            _uiState.update {
                it.copy(
                    currentFrequencyHz = TinnitusPitchPolicy.normalizeFrequencyHz(frequencyHz),
                    errorMessage = null,
                    saveMessage = null,
                )
            }
        }

        fun playPreview() {
            if (!ensureProUser()) return

            runCatching {
                toneGenerator.playTone(
                    frequencyHz = _uiState.value.currentFrequencyHz,
                    amplitudeDb = TinnitusPitchPolicy.PREVIEW_AMPLITUDE_DB,
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(errorMessage = context.getString(R.string.tinnitus_pitch_playback_failed))
                }
            }
        }

        fun saveProfile() {
            if (!ensureProUser()) return

            val state = _uiState.value
            val profile =
                state.profile()
                    .withFrequency(
                        ear = state.selectedEar,
                        frequencyHz = state.currentFrequencyHz,
                        updatedAtMs = System.currentTimeMillis(),
                    )
            viewModelScope.launch {
                runCatching { preferencesRepository.updateTinnitusPitchProfile(profile) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                leftFrequencyHz = profile.leftFrequencyHz,
                                rightFrequencyHz = profile.rightFrequencyHz,
                                updatedAtMs = profile.updatedAtMs,
                                errorMessage = null,
                                saveMessage = context.getString(R.string.tinnitus_pitch_saved),
                            )
                        }
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        _uiState.update {
                            it.copy(errorMessage = context.getString(R.string.tinnitus_pitch_save_failed))
                        }
                    }
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(errorMessage = null, saveMessage = null) }
        }

        override fun onCleared() {
            toneGenerator.stop()
            super.onCleared()
        }

        private fun ensureProUser(): Boolean {
            if (_uiState.value.isProUser) return true

            toneGenerator.stop()
            _uiState.update {
                it.copy(errorMessage = context.getString(R.string.tinnitus_pitch_pro_required))
            }
            return false
        }

        private fun TinnitusPitchMatcherUiState.profile(): TinnitusPitchProfile = TinnitusPitchProfile(
            leftFrequencyHz = leftFrequencyHz,
            rightFrequencyHz = rightFrequencyHz,
            updatedAtMs = updatedAtMs,
        )

        private fun baseState(): TinnitusPitchMatcherUiState = TinnitusPitchMatcherUiState(
            title = context.getString(R.string.tinnitus_pitch_title),
            description = context.getString(R.string.tinnitus_pitch_description),
            disclaimer = context.getString(R.string.tinnitus_pitch_disclaimer),
        )
    }
