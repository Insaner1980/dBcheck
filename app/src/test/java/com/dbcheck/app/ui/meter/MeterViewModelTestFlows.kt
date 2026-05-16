package com.dbcheck.app.ui.meter

import android.content.Context
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.DecibelReading
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.service.SessionStats
import com.dbcheck.app.util.HapticFeedbackHelper
import com.dbcheck.app.util.ShareResultsGenerator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class MeterViewModelTestFlows(
    initialPreferences: UserPreferences = UserPreferences(),
) {
    val decibelReadings = MutableSharedFlow<DecibelReading>()
    val sessionStats = MutableStateFlow(SessionStats())
    val completedSessions = MutableSharedFlow<Long>()
    val healthConnectSyncFailures = MutableSharedFlow<String>()
    val isRecording = MutableStateFlow(false)
    val preferences = MutableStateFlow(initialPreferences)
}

class MeterViewModelTestDependencies(
    private val flows: MeterViewModelTestFlows,
) {
    val context = mockk<Context>(relaxed = true)
    val shareResultsGenerator = mockk<ShareResultsGenerator>()

    private val audioEngine =
        mockk<AudioEngine> {
            every { decibelFlow } returns flows.decibelReadings
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns flows.preferences
        }
    private val hapticHelper = mockk<HapticFeedbackHelper>(relaxed = true)

    fun createViewModel(audioSessionManager: AudioSessionManager): MeterViewModel =
        MeterViewModel(
            context = context,
            audioEngine = audioEngine,
            audioSessionManager = audioSessionManager,
            preferencesRepository = preferencesRepository,
            hapticHelper = hapticHelper,
            shareResultsGenerator = shareResultsGenerator,
        )
}
