package com.dbcheck.app.ui.tinnitus

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.tinnitus.TinnitusPitchPolicy
import com.dbcheck.app.domain.tinnitus.TinnitusPitchProfile
import com.dbcheck.app.service.ToneGenerator
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TinnitusPitchMatcherViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = true))
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
            coEvery { updateTinnitusPitchProfile(any()) } just runs
        }
    private val toneGenerator = mockk<ToneGenerator>(relaxed = true)

    @Test
    fun mapsStoredPitchProfileIntoState() = runTest {
        preferencesFlow.value =
            UserPreferences(
                isProUser = true,
                tinnitusPitchProfile =
                    TinnitusPitchProfile(
                        leftFrequencyHz = 1_000f,
                        rightFrequencyHz = 4_000f,
                        updatedAtMs = 1_700_000_000_000L,
                    ),
            )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1_000f, viewModel.uiState.value.leftFrequencyHz ?: 0f, 0f)
        assertEquals(4_000f, viewModel.uiState.value.rightFrequencyHz ?: 0f, 0f)
        assertTrue(viewModel.uiState.value.hasSavedProfile)
    }

    @Test
    fun previewClampsFrequencyAndUsesFixedAmplitude() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateFrequency(12_500f)
        viewModel.playPreview()

        verify {
            toneGenerator.playTone(
                TinnitusPitchPolicy.MAX_FREQUENCY_HZ,
                TinnitusPitchPolicy.PREVIEW_AMPLITUDE_DB,
            )
        }
    }

    @Test
    fun saveProfilePersistsSelectedEarOnly() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectEar(Ear.RIGHT)
        viewModel.updateFrequency(4_050f)
        viewModel.saveProfile()
        advanceUntilIdle()

        coVerify {
            preferencesRepository.updateTinnitusPitchProfile(
                match { profile ->
                    profile.leftFrequencyHz == null &&
                        profile.rightFrequencyHz == 4_050f &&
                        profile.updatedAtMs != null
                },
            )
        }
    }

    @Test
    fun freeUserCannotPreviewOrPersistPitchProfile() = runTest {
        preferencesFlow.value = UserPreferences(isProUser = false)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.playPreview()
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLocked)
        assertEquals("Tinnitus pitch profile requires dBcheck Pro", viewModel.uiState.value.errorMessage)
        verify(exactly = 0) { toneGenerator.playTone(any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.updateTinnitusPitchProfile(any()) }
    }

    @Test
    fun losingProStopsPreviewPlaybackAndLocksProfile() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        preferencesFlow.value = UserPreferences(isProUser = false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLocked)
        verify { toneGenerator.stop() }
    }

    @Test
    fun stateCopyStaysPersonalTrackingOnly() = runTest {
        val viewModel = createViewModel()

        val lowerCaseCopy =
            listOf(
                viewModel.uiState.value.title,
                viewModel.uiState.value.description,
                viewModel.uiState.value.disclaimer,
            ).joinToString(separator = " ")
                .lowercase()

        assertTrue(lowerCaseCopy.contains("personal tracking"))
        assertFalse(lowerCaseCopy.contains("diagnosis"))
        assertFalse(lowerCaseCopy.contains("treatment"))
        assertFalse(lowerCaseCopy.contains("therapy"))
        assertFalse(lowerCaseCopy.contains("cure"))
    }

    private fun createViewModel(): TinnitusPitchMatcherViewModel = TinnitusPitchMatcherViewModel(
            context = testStringContext(),
            preferencesRepository = preferencesRepository,
            toneGenerator = toneGenerator,
        )
}
