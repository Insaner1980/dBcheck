package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.clearForTest
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.CalibrationProfileDeleteResult
import com.dbcheck.app.data.repository.CalibrationProfileRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.calibration.CalibrationProfile
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.sync.HealthConnectStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelCalibrationProfileTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesFlow =
        MutableStateFlow(
            UserPreferences(
                isProUser = true,
                micSensitivityOffset = 2.5f,
                selectedCalibrationProfileId = FIELD_PROFILE_ID,
            ),
        )
    private val profileFlow =
        MutableStateFlow(
            listOf(
                calibrationProfile(id = DEFAULT_PROFILE_ID, name = "Device default", isDefault = true),
                calibrationProfile(id = FIELD_PROFILE_ID, name = "Field mic", offset = 2.5f),
            ),
        )
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
            coEvery { updateSelectedCalibrationProfileId(any()) } just runs
        }
    private val calibrationProfileRepository =
        mockk<CalibrationProfileRepository> {
            every { observeProfiles() } returns profileFlow
            coEvery { createProfile(any(), any(), any(), any()) } returns CREATED_PROFILE_ID
            coEvery { renameProfile(any(), any(), any()) } just runs
            coEvery { updateOctaveBandOffsets(any(), any(), any()) } just runs
            coEvery { resetOctaveBandOffsets(any(), any()) } just runs
            coEvery { deleteProfile(any()) } returns CalibrationProfileDeleteResult.Deleted
        }
    private val healthConnectManager =
        mockk<HealthConnectManager> {
            coEvery { getStatus() } returns HealthConnectStatus()
        }
    private val audioSessionManager =
        mockk<AudioSessionManager> {
            every { isRecording } returns MutableStateFlow(false)
        }

    @Test
    fun calibrationProfilesAreMappedIntoSettingsUiState() = runTest {
        val viewModel = createViewModel()
        try {
            runCurrent()

            assertEquals(FIELD_PROFILE_ID, viewModel.uiState.value.selectedCalibrationProfileId)
            assertEquals(
                listOf("Device default", "Field mic"),
                viewModel.uiState.value.calibrationProfiles.map { it.name },
            )
            assertEquals(false, viewModel.uiState.value.calibrationProfiles.first().canDelete)
            assertEquals(true, viewModel.uiState.value.calibrationProfiles.last().isSelected)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun calibrationProfilesExposeOctaveBandOffsetsInSettingsUiState() = runTest {
        profileFlow.value =
            listOf(
                calibrationProfile(id = DEFAULT_PROFILE_ID, name = "Device default", isDefault = true),
                calibrationProfile(
                    id = FIELD_PROFILE_ID,
                    name = "Field mic",
                    offset = 2.5f,
                    octaveOffsets = OctaveCalibrationOffsets.zero().withOffset(1_000f, 3.5f),
                ),
            )
        val viewModel = createViewModel()
        try {
            runCurrent()

            val fieldProfile = viewModel.uiState.value.calibrationProfiles.last()
            assertEquals(
                OctaveCalibrationOffsets.supportedCenterFrequenciesHz,
                fieldProfile.octaveBandOffsets.map { it.centerFrequencyHz },
            )
            assertEquals(
                3.5f,
                fieldProfile.octaveBandOffsets.first { it.centerFrequencyHz == 1_000f }.offsetDb,
                0f,
            )
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun createCalibrationProfilePersistsProfileAndSelectsIt() = runTest {
        val viewModel = createViewModel()
        try {
            runCurrent()

            viewModel.createCalibrationProfile("  New mic  ")
            runCurrent()

            coVerify {
                calibrationProfileRepository.createProfile(
                    name = "New mic",
                    micSensitivityOffset = 2.5f,
                    isDefault = false,
                    timestampMillis = any(),
                )
            }
            coVerify { preferencesRepository.updateSelectedCalibrationProfileId(CREATED_PROFILE_ID) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun selectCalibrationProfilePersistsSelectedProfileId() = runTest {
        val viewModel = createViewModel()
        try {

            viewModel.selectCalibrationProfile(DEFAULT_PROFILE_ID)
            runCurrent()

            coVerify { preferencesRepository.updateSelectedCalibrationProfileId(DEFAULT_PROFILE_ID) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun renameCalibrationProfilePersistsTrimmedName() = runTest {
        val viewModel = createViewModel()
        try {

            viewModel.renameCalibrationProfile(FIELD_PROFILE_ID, "  Outdoor mic  ")
            runCurrent()

            coVerify {
                calibrationProfileRepository.renameProfile(
                    profileId = FIELD_PROFILE_ID,
                    name = "Outdoor mic",
                    timestampMillis = any(),
                )
            }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun updateOctaveBandOffsetPersistsProfileBandOffsets() = runTest {
        val viewModel = createViewModel()
        try {
            runCurrent()

            viewModel.updateOctaveBandOffset(
                profileId = FIELD_PROFILE_ID,
                centerFrequencyHz = 1_000f,
                offsetDb = 4f,
            )
            runCurrent()

            coVerify {
                calibrationProfileRepository.updateOctaveBandOffsets(
                    profileId = FIELD_PROFILE_ID,
                    offsets = match { it.offsetFor(1_000f) == 4f },
                    timestampMillis = any(),
                )
            }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun resetOctaveBandOffsetsPersistsZeroProfileOffsets() = runTest {
        val viewModel = createViewModel()
        try {
            runCurrent()

            viewModel.resetOctaveBandOffsets(FIELD_PROFILE_ID)
            runCurrent()

            coVerify {
                calibrationProfileRepository.resetOctaveBandOffsets(
                    profileId = FIELD_PROFILE_ID,
                    timestampMillis = any(),
                )
            }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun deleteSelectedProfileSelectsFallbackProfile() = runTest {
        val viewModel = createViewModel()
        try {

            viewModel.deleteCalibrationProfile(FIELD_PROFILE_ID)
            runCurrent()

            coVerify { calibrationProfileRepository.deleteProfile(FIELD_PROFILE_ID) }
            coVerify { preferencesRepository.updateSelectedCalibrationProfileId(DEFAULT_PROFILE_ID) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun deleteLastDefaultProfileShowsErrorWithoutChangingSelection() = runTest {
        profileFlow.value =
            listOf(
                calibrationProfile(id = DEFAULT_PROFILE_ID, name = "Device default", isDefault = true),
            )
        preferencesFlow.value = UserPreferences(isProUser = true, selectedCalibrationProfileId = DEFAULT_PROFILE_ID)
        coEvery { calibrationProfileRepository.deleteProfile(DEFAULT_PROFILE_ID) } returns
            CalibrationProfileDeleteResult.BlockedLastDefault
        val viewModel = createViewModel()
        try {
            runCurrent()

            viewModel.deleteCalibrationProfile(DEFAULT_PROFILE_ID)
            runCurrent()

            assertEquals(
                "Keep one default calibration profile",
                viewModel.uiState.value.calibrationProfileErrorMessage,
            )
            coVerify(exactly = 0) { preferencesRepository.updateSelectedCalibrationProfileId(any()) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun freeUserCannotMutateCalibrationProfiles() = runTest {
        preferencesFlow.value = UserPreferences(isProUser = false, selectedCalibrationProfileId = FIELD_PROFILE_ID)
        val viewModel = createViewModel()
        try {

            viewModel.createCalibrationProfile("New")
            viewModel.selectCalibrationProfile(DEFAULT_PROFILE_ID)
            viewModel.renameCalibrationProfile(FIELD_PROFILE_ID, "Renamed")
            viewModel.updateOctaveBandOffset(FIELD_PROFILE_ID, centerFrequencyHz = 1_000f, offsetDb = 3f)
            viewModel.resetOctaveBandOffsets(FIELD_PROFILE_ID)
            viewModel.deleteCalibrationProfile(FIELD_PROFILE_ID)
            runCurrent()

            coVerify(exactly = 0) { calibrationProfileRepository.createProfile(any(), any(), any(), any()) }
            coVerify(exactly = 0) { calibrationProfileRepository.renameProfile(any(), any(), any()) }
            coVerify(exactly = 0) { calibrationProfileRepository.updateOctaveBandOffsets(any(), any(), any()) }
            coVerify(exactly = 0) { calibrationProfileRepository.resetOctaveBandOffsets(any(), any()) }
            coVerify(exactly = 0) { calibrationProfileRepository.deleteProfile(any()) }
            coVerify(exactly = 0) { preferencesRepository.updateSelectedCalibrationProfileId(any()) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun freeUserSettingsUsesEmptyEffectiveCalibrationProfileState() = runTest {
        preferencesFlow.value = UserPreferences(isProUser = false, selectedCalibrationProfileId = FIELD_PROFILE_ID)

        val viewModel = createViewModel()
        try {
            runCurrent()

            assertEquals(emptyList<Any>(), viewModel.uiState.value.calibrationProfiles)
            assertEquals(null, viewModel.uiState.value.selectedCalibrationProfileId)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun proUserSettingsEnsuresDefaultProfileWhenNoProfilesExist() = runTest {
        profileFlow.value = emptyList()
        val viewModel = createViewModel()
        try {
            runCurrent()

            coVerify {
                calibrationProfileRepository.createProfile(
                    name = "Device default",
                    micSensitivityOffset = 2.5f,
                    isDefault = true,
                    timestampMillis = any(),
                )
            }
            assertEquals(emptyList<Any>(), viewModel.uiState.value.calibrationProfiles)
        } finally {
            viewModel.clearForTest()
        }
    }

    private fun createViewModel(): SettingsViewModel = settingsViewModelForTest(
        preferencesRepository = preferencesRepository,
        healthConnectManager = healthConnectManager,
        audioSessionManager = audioSessionManager,
        calibrationProfileRepository = calibrationProfileRepository,
    )

    private fun calibrationProfile(
        id: Long,
        name: String,
        offset: Float = 0f,
        octaveOffsets: OctaveCalibrationOffsets = OctaveCalibrationOffsets.zero(),
        isDefault: Boolean = false,
    ): CalibrationProfile = CalibrationProfile(
        id = id,
        name = name,
        micSensitivityOffset = offset,
        octaveCalibrationOffsets = octaveOffsets,
        isDefault = isDefault,
        createdAt = 1_700_000_000_000L + id,
        updatedAt = 1_700_000_060_000L + id,
    )

    private companion object {
        const val DEFAULT_PROFILE_ID = 1L
        const val FIELD_PROFILE_ID = 2L
        const val CREATED_PROFILE_ID = 3L
    }
}
