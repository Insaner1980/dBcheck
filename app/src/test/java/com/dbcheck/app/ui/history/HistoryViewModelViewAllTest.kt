package com.dbcheck.app.ui.history

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.MeasurementRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.data.repository.SessionRepository
import com.dbcheck.app.domain.analytics.HourlyExposureAverage
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionHistoryPolicy
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.history.state.HistoryUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelViewAllTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hourlyAverages = MutableStateFlow(listOf(HourlyExposureAverage(10, 70f, 80f)))
    private val recentSessions = MutableStateFlow(sessions(20))
    private val allSessions = MutableStateFlow(sessions(25))
    private val preferences = MutableStateFlow(UserPreferences(isProUser = true))
    private val measurementRepository =
        mockk<MeasurementRepository> {
            every { getHourlyAveragesLast24H() } returns hourlyAverages
        }
    private val sessionRepository =
        mockk<SessionRepository> {
            every { getRecentSessions(20) } returns recentSessions
            every { getAllCompletedSessions() } returns allSessions
            coEvery { updateSessionMetadata(any(), any(), any(), any()) } just runs
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }

    @Test
    fun viewAllShowsAllAvailableSessions() = runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(20, successState(viewModel).recentSessions.size)

            viewModel.showAllSessions()
            advanceUntilIdle()

            assertEquals(25, successState(viewModel).recentSessions.size)
            assertEquals(true, successState(viewModel).isShowingAllSessions)
        }

    @Test
    fun freeUserCannotSaveSessionMetadata() = runTest {
            preferences.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.saveSessionMetadata(
                sessionId = 1L,
                name = "Workshop",
                emoji = "",
                tags = listOf("Work"),
            )
            advanceUntilIdle()

            coVerify(exactly = 0) {
                sessionRepository.updateSessionMetadata(any(), any(), any(), any())
            }
        }

    @Test
    fun freeUserWithOnlyOldSessionsAndNoHourlyDataSeesEmptyState() = runTest {
            preferences.value = UserPreferences(isProUser = false)
            hourlyAverages.value = emptyList()
            recentSessions.value =
                listOf(
                    session(
                        id = 1L,
                        startTime = System.currentTimeMillis() -
                            SessionHistoryPolicy.FREE_HISTORY_WINDOW_MILLIS -
                            1_000L,
                    ),
                )
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(HistoryUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun safeHoursUsesBucketDurationInsteadOfBucketCount() = runTest {
            hourlyAverages.value =
                listOf(
                    HourlyExposureAverage(
                        hour = 10,
                        avgDb = 60f,
                        maxDb = 62f,
                        durationMs = 5 * 60_000L,
                    ),
                    HourlyExposureAverage(
                        hour = 11,
                        avgDb = 90f,
                        maxDb = 92f,
                        durationMs = 60 * 60_000L,
                    ),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(5f / 60f, successState(viewModel).safeHours, 0.001f)
        }

    @Test
    fun historyLoadFailureShowsErrorState() = runTest {
            every { measurementRepository.getHourlyAveragesLast24H() } returns
                flow { throw IllegalStateException("db") }

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(
                HistoryUiState.Error("Unable to load history"),
                viewModel.uiState.value,
            )
        }

    @Test
    fun sessionMetadataSaveFailureShowsUserFacingError() = runTest {
            coEvery { sessionRepository.updateSessionMetadata(any(), any(), any(), any()) } throws
                IllegalStateException("db")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.saveSessionMetadata(
                sessionId = 1L,
                name = "Workshop",
                emoji = "",
                tags = listOf("Work"),
            )
            advanceUntilIdle()

            assertEquals("Unable to update session", successState(viewModel).metadataErrorMessage)
        }

    private fun createViewModel(): HistoryViewModel = HistoryViewModel(
            context = testStringContext(),
            sessionRepository = sessionRepository,
            measurementRepository = measurementRepository,
            preferencesRepository = preferencesRepository,
        )

    private fun successState(viewModel: HistoryViewModel): HistoryUiState.Success =
        viewModel.uiState.value as HistoryUiState.Success

    private companion object {
        fun sessions(count: Int): List<Session> = (1..count).map { index ->
            session(id = index.toLong(), startTime = 1_700_000_000_000L - index)
        }

        fun session(id: Long, startTime: Long): Session = Session(
            id = id,
            startTime = startTime,
            endTime = startTime + 60_000L,
            minDb = 50f,
            avgDb = 65f,
            maxDb = 80f,
            peakDb = 90f,
            name = null,
            emoji = null,
            tags = emptyList(),
            isActive = false,
            frequencyWeighting = "A",
        )
    }
}
