package com.dbcheck.app.ui.camera

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dbcheck.app.data.local.preferences.model.ProAudioPreferencePolicy
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.domain.report.equivalentLevelLabelForWeighting
import com.dbcheck.app.service.AudioEngine
import com.dbcheck.app.service.AudioSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class CameraOverlayReadoutStatus { READY, LIVE }

data class CameraOverlayUiState(
    val currentDb: Float? = null,
    val status: CameraOverlayReadoutStatus = CameraOverlayReadoutStatus.READY,
    val levelLabel: String = equivalentLevelLabelForWeighting(UserPreferenceDefaults.FREQUENCY_WEIGHTING),
    val timestampMs: Long? = null,
    val isCapturingPhoto: Boolean = false,
    val captureFailed: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val videoCaptureFailed: Boolean = false,
)

@HiltViewModel
class CameraOverlayViewModel
    @Inject
    constructor(
        private val audioEngine: AudioEngine,
        private val audioSessionManager: AudioSessionManager,
        private val preferencesRepository: PreferencesRepository,
        private val shareGenerator: CameraOverlayShareGenerator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CameraOverlayUiState())
        val uiState: StateFlow<CameraOverlayUiState> = _uiState.asStateFlow()
        private val _photoShareIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val photoShareIntents: SharedFlow<Intent> = _photoShareIntents.asSharedFlow()

        init {
            observePreferences()
            observeRecordingState()
            observeDecibelReadings()
        }

        private fun observePreferences() {
            viewModelScope.launch {
                preferencesRepository.userPreferences.collect { preferences ->
                    val effectiveWeighting = ProAudioPreferencePolicy.weighting(preferences)
                    _uiState.update {
                        it.copy(levelLabel = equivalentLevelLabelForWeighting(effectiveWeighting))
                    }
                }
            }
        }

        private fun observeRecordingState() {
            viewModelScope.launch {
                audioSessionManager.isRecording.collect { recording ->
                    if (!recording) {
                        clearLiveReadout()
                    }
                }
            }
        }

        private fun observeDecibelReadings() {
            viewModelScope.launch {
                audioEngine.decibelFlow.collect { reading ->
                    if (shouldShowReading(reading)) {
                        _uiState.update {
                            it.copy(
                                currentDb = reading.weightedDb,
                                status = CameraOverlayReadoutStatus.LIVE,
                                timestampMs = reading.timestamp,
                            )
                        }
                    }
                }
            }
        }

        private fun shouldShowReading(reading: DecibelReading): Boolean {
            val sessionStartTimeMs = audioSessionManager.activeSessionStartTimeMs.value
            return audioSessionManager.isRecording.value &&
                sessionStartTimeMs != null &&
                reading.timestamp >= sessionStartTimeMs
        }

        private fun clearLiveReadout() {
            _uiState.update {
                it.copy(
                    currentDb = null,
                    status = CameraOverlayReadoutStatus.READY,
                    timestampMs = null,
                )
            }
        }

        fun createPhotoCaptureFile(onFileReady: (File) -> Unit) {
            createCameraOutputFile(
                createFile = shareGenerator::createRawCaptureFile,
                onFileReady = onFileReady,
                onFailure = ::onPhotoCaptureFailed,
            )
        }

        fun createSilentVideoFile(onFileReady: (File) -> Unit) {
            createCameraOutputFile(
                createFile = shareGenerator::createSilentVideoFile,
                onFileReady = onFileReady,
                onFailure = ::onVideoRecordingFailed,
            )
        }

        private fun createCameraOutputFile(
            createFile: suspend () -> File,
            onFileReady: (File) -> Unit,
            onFailure: () -> Unit,
        ) {
            viewModelScope.launch {
                runCatching {
                    createFile()
                }.onSuccess(onFileReady)
                    .onFailure { onFailure() }
            }
        }

        fun onPhotoCaptureStarted() {
            _uiState.update { it.copy(isCapturingPhoto = true, captureFailed = false) }
        }

        fun onPhotoCaptured(photoFile: File) {
            val readout = _uiState.value
            viewModelScope.launch {
                runCatching {
                    shareGenerator.createPhotoShareIntent(photoFile, readout)
                }.onSuccess { intent ->
                    _uiState.update { it.copy(isCapturingPhoto = false, captureFailed = false) }
                    _photoShareIntents.emit(intent)
                }.onFailure {
                    onPhotoCaptureFailed()
                }
            }
        }

        fun onPhotoCaptureFailed() {
            _uiState.update { it.copy(isCapturingPhoto = false, captureFailed = true) }
        }

        fun onVideoRecordingStarted() {
            _uiState.update {
                it.copy(
                    isRecordingVideo = true,
                    videoCaptureFailed = false,
                )
            }
        }

        fun onVideoRecordingFinished() {
            _uiState.update {
                it.copy(
                    isRecordingVideo = false,
                    videoCaptureFailed = false,
                )
            }
        }

        fun onVideoRecordingFailed() {
            _uiState.update {
                it.copy(
                    isRecordingVideo = false,
                    videoCaptureFailed = true,
                )
            }
        }
    }
