package com.dbcheck.app.ui.hearingtest.active

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.audio.ToneGenerator
import com.dbcheck.app.domain.audio.ToneOutputChannel
import com.dbcheck.app.service.HearingTestService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActiveTestViewModelCompletionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toneGenerator = mockk<ToneGenerator>(relaxed = true)
    private val hearingTestService =
        mockk<HearingTestService> {
            coEvery { saveCompletedTest(any(), any()) } returns SAVED_TEST_ID
        }
    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = true))
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun completedStateContainsSavedResultId() = runTest {
            val viewModel = createViewModel()

            viewModel.startTest()
            repeat(REQUIRED_PHASES) {
                repeat(NOT_HEARD_RESPONSES_TO_COMPLETE_PHASE) {
                    advanceToToneStart()
                    viewModel.onNotHeard()
                }
            }
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isComplete)
            assertEquals(SAVED_TEST_ID, viewModel.state.value.completedTestId)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun repeatedStartCallDoesNotResetActiveHearingTest() = runTest {
            val viewModel = createViewModel()

            viewModel.startTest()
            repeat(NOT_HEARD_RESPONSES_TO_COMPLETE_PHASE) {
                advanceToToneStart()
                viewModel.onNotHeard()
            }
            runCurrent()

            assertEquals(2, viewModel.state.value.currentPhase)

            viewModel.startTest()
            runCurrent()

            assertEquals(2, viewModel.state.value.currentPhase)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun freeUserCannotStartOrSaveHearingTest() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val viewModel = createViewModel()

            viewModel.startTest()
            advanceUntilIdle()
            viewModel.onNotHeard()
            advanceUntilIdle()

            assertEquals("Hearing test requires dBcheck Pro", viewModel.state.value.errorMessage)
            coVerify(exactly = 0) { hearingTestService.saveCompletedTest(any(), any()) }
            verify(exactly = 0) {
                toneGenerator.playTone(
                    frequencyHz = any(),
                    amplitudeDb = any(),
                    outputChannel = any(),
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun responseBeforeToneStartsDoesNotScheduleStaleTone() = runTest {
            val viewModel = createViewModel()

            viewModel.startTest()
            runCurrent()
            viewModel.onNotHeard()

            advanceToToneStart()

            verify(exactly = 1) {
                toneGenerator.playTone(
                    frequencyHz = 250f,
                    amplitudeDb = -30f,
                    outputChannel = ToneOutputChannel.LEFT,
                )
            }
            verify(exactly = 0) {
                toneGenerator.playTone(
                    frequencyHz = 250f,
                    amplitudeDb = -25f,
                    outputChannel = any(),
                )
            }
            assertEquals(1, viewModel.state.value.currentPhase)
            assertTrue(viewModel.state.value.canRespond)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun leftAndRightEarPhasesPlayThroughMatchingOutputChannels() = runTest {
            val viewModel = createViewModel()

            viewModel.startTest()
            repeat(FREQUENCIES_PER_EAR) {
                repeat(NOT_HEARD_RESPONSES_TO_COMPLETE_PHASE) {
                    advanceToToneStart()
                    viewModel.onNotHeard()
                }
            }
            advanceToToneStart()

            verify(atLeast = 1) {
                toneGenerator.playTone(
                    frequencyHz = 250f,
                    amplitudeDb = -30f,
                    outputChannel = ToneOutputChannel.LEFT,
                )
            }
            verify(atLeast = 1) {
                toneGenerator.playTone(
                    frequencyHz = 500f,
                    amplitudeDb = -30f,
                    outputChannel = ToneOutputChannel.LEFT,
                )
            }
            verify(atLeast = 1) {
                toneGenerator.playTone(
                    frequencyHz = 250f,
                    amplitudeDb = -30f,
                    outputChannel = ToneOutputChannel.RIGHT,
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun tonePlaybackFailureClosesResponseWindow() = runTest {
            every {
                toneGenerator.playTone(
                    frequencyHz = any(),
                    amplitudeDb = any(),
                    outputChannel = any(),
                )
            } throws IllegalStateException("Playback failed")
            val viewModel = createViewModel()

            viewModel.startTest()
            advanceToToneStart()

            assertFalse(viewModel.state.value.isPlayingTone)
            assertFalse(viewModel.state.value.canRespond)
            assertEquals("Unable to play hearing test tone", viewModel.state.value.errorMessage)
        }

    private fun createViewModel(): ActiveTestViewModel = ActiveTestViewModel(
            toneGenerator = toneGenerator,
            hearingTestService = hearingTestService,
            preferencesRepository = preferencesRepository,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun kotlinx.coroutines.test.TestScope.advanceToToneStart() {
        runCurrent()
        advanceTimeBy(TONE_START_DELAY_MS)
        runCurrent()
    }

    private companion object {
        const val SAVED_TEST_ID = 42L
        const val REQUIRED_PHASES = 12
        const val FREQUENCIES_PER_EAR = 6
        const val NOT_HEARD_RESPONSES_TO_COMPLETE_PHASE = 7
        const val TONE_START_DELAY_MS = 500L
    }
}
