package com.dbcheck.app.ui.hearingtest.results

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.testStringContext
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.util.ShareResultsGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelShareTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val latestResult = MutableStateFlow<HearingTestResult?>(hearingResult())
    private val routedResult = MutableStateFlow<HearingTestResult?>(hearingResult(id = 42L))
    private val preferences = MutableStateFlow(UserPreferences(isProUser = true))
    private val hearingTestRepository =
        mockk<HearingTestRepository> {
            every { getLatestResult() } returns latestResult
            every { getResultById(7L) } returns latestResult
            every { getResultById(42L) } returns routedResult
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferences
        }
    private val shareResultsGenerator = mockk<ShareResultsGenerator>()

    @Test
    fun loadedResultCreatesShareIntentWithScoreAndRating() = runTest {
            val intent = Intent(Intent.ACTION_SEND)
            coEvery {
                shareResultsGenerator.shareHearingTestResults(score = 86, rating = "Good")
            } returns intent
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                assertSame(intent, awaitItem())
            }
            assertNull(viewModel.state.value.shareErrorMessage)
            coVerify(exactly = 1) {
                shareResultsGenerator.shareHearingTestResults(score = 86, rating = "Good")
            }
        }

    @Test
    fun missingResultReturnsNullAndShowsError() = runTest {
            latestResult.value = null
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                expectNoEvents()
            }
            assertEquals("No hearing test result to share", viewModel.state.value.shareErrorMessage)
            coVerify(exactly = 0) {
                shareResultsGenerator.shareHearingTestResults(any(), any())
            }
        }

    @Test
    fun missingRouteResultIsExplicitResultState() = runTest {
            latestResult.value = null

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isResultMissing)
            assertNull(viewModel.state.value.resultId)
        }

    @Test
    fun shareGeneratorFailureReturnsNullAndShowsError() = runTest {
            coEvery { shareResultsGenerator.shareHearingTestResults(any(), any()) } throws
                IllegalStateException("Disk full")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                advanceUntilIdle()
                expectNoEvents()
            }
            assertEquals("Unable to share hearing test results", viewModel.state.value.shareErrorMessage)
        }

    @Test
    fun loadsResultForRouteArgument() = runTest {
            val viewModel = createViewModel(testId = 42L)

            assertEquals(42L, viewModel.state.value.resultId)
        }

    @Test
    fun freeUserCannotLoadOrShareHearingTestResult() = runTest {
            preferences.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel(testId = 42L)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoading)
            assertNull(viewModel.state.value.resultId)
            assertEquals("Hearing test requires dBcheck Pro", viewModel.state.value.shareErrorMessage)
            viewModel.shareIntents.test {
                viewModel.createShareIntent()
                expectNoEvents()
            }
            coVerify(exactly = 0) { shareResultsGenerator.shareHearingTestResults(any(), any()) }
        }

    @Test
    fun defaultStateRendersLoadingInsteadOfBlankResult() {
            assertEquals(ResultsContentMode.LOADING, resultsContentMode(ResultsUiState()))
        }

    @Test
    fun freeUserStateRendersLockedContentInsteadOfBlankResult() {
            val state =
                ResultsUiState(
                    isLoading = false,
                    isProUser = false,
                    shareErrorMessage = "Hearing test requires dBcheck Pro",
                )

            assertEquals(ResultsContentMode.LOCKED, resultsContentMode(state))
        }

    private fun createViewModel(testId: Long = 7L): ResultsViewModel = ResultsViewModel(
            context = testStringContext(),
            savedStateHandle = SavedStateHandle(mapOf(Screen.HearingTestResults.ARG_TEST_ID to testId)),
            hearingTestRepository = hearingTestRepository,
            preferencesRepository = preferencesRepository,
            shareResultsGenerator = shareResultsGenerator,
        )

    private companion object {
        fun hearingResult(id: Long = 7L) = HearingTestResult(
                id = id,
                timestamp = 1_700_000_000_000L,
                overallScore = 86,
                rating = "Good",
                leftEarThresholds = listOf(250f to -45f, 500f to -50f),
                rightEarThresholds = listOf(250f to -40f, 500f to -48f),
                speechClarity = 84.2f,
                highFreqLimit = 16_800f,
                avgThreshold = -47f,
            )
    }
}
