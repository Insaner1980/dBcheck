package com.dbcheck.app.ui.meter

import android.content.Context
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.SessionStats
import com.dbcheck.app.util.HapticFeedbackHelper
import com.dbcheck.app.util.ShareResultsGenerator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

internal class MeterViewModelTestHarness(
    initialPreferences: UserPreferences = UserPreferences(),
    relaxedAudioSessionManager: Boolean = true,
) {
    val decibelReadings = MutableSharedFlow<DecibelReading>()
    val sessionStats = MutableStateFlow(SessionStats())
    val completedSessions = MutableSharedFlow<Long>()
    val healthConnectSyncFailures = MutableSharedFlow<String>()
    val recordingFailures = MutableSharedFlow<AudioRecordingFailure>()
    val isRecording = MutableStateFlow(false)
    val preferencesFlow = MutableStateFlow(initialPreferences)
    val context = mockk<Context>(relaxed = true)
    val audioEngine =
        mockk<AudioEngine> {
            every { decibelFlow } returns decibelReadings
        }
    val audioSessionManager = mockk<AudioSessionManager>(relaxed = relaxedAudioSessionManager)
    val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }
    val hapticHelper = mockk<HapticFeedbackHelper>(relaxed = true)
    val shareResultsGenerator = mockk<ShareResultsGenerator>()

    fun createViewModel(): MeterViewModel {
        stubSessionManagerFlows()
        return MeterViewModel(
            context = context,
            audioEngine = audioEngine,
            audioSessionManager = audioSessionManager,
            preferencesRepository = preferencesRepository,
            hapticHelper = hapticHelper,
            shareResultsGenerator = shareResultsGenerator,
        )
    }

    private fun stubSessionManagerFlows() {
        every { audioSessionManager.sessionStats } returns sessionStats
        every { audioSessionManager.completedSessionIds } returns completedSessions
        every { audioSessionManager.healthConnectSyncFailures } returns healthConnectSyncFailures
        every { audioSessionManager.recordingFailures } returns recordingFailures
        every { audioSessionManager.isRecording } returns isRecording
    }
}
