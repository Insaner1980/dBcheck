package com.dbcheck.app.ui.analytics

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
import com.dbcheck.app.testHearingRecoveryResult
import com.dbcheck.app.testHearingResult
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import com.dbcheck.app.ui.analytics.state.HearingRecoveryUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelRecoveryTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fixture =
        AnalyticsViewModelTestFixture(
            defaultDispatcher = testDispatcher,
            latestBaseline =
                MutableStateFlow(
                    testHearingResult(
                        id = 1L,
                        timestamp = 1_000L,
                        leftEarThresholds = emptyList(),
                        rightEarThresholds = emptyList(),
                        highFreqLimit = 8000f,
                        avgThreshold = -30f,
                    ),
                ),
            latestRecovery = MutableStateFlow(testHearingRecoveryResult()),
        )

    @Test
    fun proUserSeesLatestRecoveryResultEvenWithoutExposureMeasurements() = runTest(testDispatcher.scheduler) {
        val viewModel = fixture.createViewModel()
        try {
            runCurrent()

            val state = viewModel.uiState.value as AnalyticsUiState.Success
            assertEquals(
                HearingRecoveryUiState.Result(
                    averageShiftDb = 6f,
                    maxShiftDb = 12f,
                    status = HearingRecoveryStatus.SMALL_SHIFT,
                    timestamp = 2_000L,
                ),
                state.hearingRecovery,
            )
        } finally {
            viewModel.clearForTest()
            runCurrent()
        }
    }

    @Test
    fun proUserWithoutBaselineSeesRecoveryBaselinePrompt() = runTest(testDispatcher.scheduler) {
        fixture.latestBaseline.value = null
        fixture.latestRecovery.value = null
        val viewModel = fixture.createViewModel()
        try {
            runCurrent()

            val state = viewModel.uiState.value as AnalyticsUiState.Success
            assertEquals(HearingRecoveryUiState.MissingBaseline, state.hearingRecovery)
        } finally {
            viewModel.clearForTest()
            runCurrent()
        }
    }

    @Test
    fun freeUserSeesLockedRecoveryPreview() = runTest(testDispatcher.scheduler) {
        fixture.preferences.value = UserPreferences(isProUser = false)
        val viewModel = fixture.createViewModel()
        try {
            runCurrent()

            val state = viewModel.uiState.value as AnalyticsUiState.Success
            assertEquals(HearingRecoveryUiState.LockedPreview, state.hearingRecovery)
        } finally {
            viewModel.clearForTest()
            runCurrent()
        }
    }
}
