package com.dbcheck.app.ui.analytics

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.ui.analytics.state.AnalyticsUiState
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelRollingWindowTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fixture = AnalyticsViewModelTestFixture(defaultDispatcher = testDispatcher)

    @Test
    fun proExposureQueriesRefreshRollingWindowsWhileCollected() = runTest(testDispatcher.scheduler) {
        val viewModel = fixture.createViewModel()
        try {
            runCurrent()

            verify(exactly = 2) { fixture.measurementRepository.getWeightedMeasurementsInRange(any(), any()) }
            verify(exactly = 1) { fixture.sessionRepository.getCompletedSessionCountInRange(any(), any()) }

            advanceTimeBy(ROLLING_WINDOW_REFRESH_MS)
            runCurrent()

            verify(exactly = 4) { fixture.measurementRepository.getWeightedMeasurementsInRange(any(), any()) }
            verify(exactly = 2) { fixture.sessionRepository.getCompletedSessionCountInRange(any(), any()) }
        } finally {
            viewModel.clearForTest()
            runCurrent()
        }
    }

    @Test
    fun analyticsLoadFailureShowsErrorState() = runTest(testDispatcher.scheduler) {
        every { fixture.measurementRepository.getDailyAveragesLast7Days() } returns
            flow { throw IllegalStateException("db") }

        val viewModel = fixture.createViewModel()
        try {
            runCurrent()

            assertEquals(
                AnalyticsUiState.Error("Unable to load analytics"),
                viewModel.uiState.value,
            )
        } finally {
            viewModel.clearForTest()
            runCurrent()
        }
    }

    private companion object {
        const val ROLLING_WINDOW_REFRESH_MS = 60_000L
    }
}
