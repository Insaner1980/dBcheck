package com.dbcheck.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.ui.settings.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState

        init {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            themeMode = prefs.themeMode,
                            exposureAlertsEnabled = prefs.exposureAlertsEnabled,
                            peakWarningsEnabled = prefs.peakWarningsEnabled,
                            notificationThreshold = prefs.notificationThreshold,
                            micSensitivityOffset = prefs.micSensitivityOffset,
                            frequencyWeighting = prefs.frequencyWeighting,
                            isProUser = prefs.isProUser,
                        )
                    }
                }
            }
        }

        fun updateExposureAlerts(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.updateExposureAlerts(enabled) }
        }

        fun updatePeakWarnings(enabled: Boolean) {
            viewModelScope.launch { preferencesRepository.updatePeakWarnings(enabled) }
        }

        fun updateNotificationThreshold(threshold: Int) {
            viewModelScope.launch { preferencesRepository.updateNotificationThreshold(threshold) }
        }

        fun updateMicSensitivity(offset: Float) {
            val clamped = offset.coerceIn(-10f, 10f)
            viewModelScope.launch { preferencesRepository.updateMicSensitivityOffset(clamped) }
        }

        fun updateFrequencyWeighting(weighting: String) {
            viewModelScope.launch { preferencesRepository.updateFrequencyWeighting(weighting) }
        }

        fun updateThemeMode(mode: String) {
            viewModelScope.launch { preferencesRepository.updateThemeMode(mode) }
        }
    }
