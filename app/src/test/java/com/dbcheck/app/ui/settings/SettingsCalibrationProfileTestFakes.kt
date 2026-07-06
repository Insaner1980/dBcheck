package com.dbcheck.app.ui.settings

import com.dbcheck.app.data.repository.CalibrationProfileDeleteResult
import com.dbcheck.app.data.repository.CalibrationProfileRepository
import com.dbcheck.app.domain.calibration.CalibrationProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow

internal fun testCalibrationProfileRepository(
    profiles: List<CalibrationProfile> = listOf(testCalibrationProfile()),
): CalibrationProfileRepository = mockk {
        every { observeProfiles() } returns MutableStateFlow(profiles)
        coEvery { createProfile(any(), any(), any(), any()) } returns TEST_CREATED_CALIBRATION_PROFILE_ID
        coEvery { renameProfile(any(), any(), any()) } just runs
        coEvery { deleteProfile(any()) } returns CalibrationProfileDeleteResult.Deleted
    }

private fun testCalibrationProfile(): CalibrationProfile = CalibrationProfile(
        id = TEST_DEFAULT_CALIBRATION_PROFILE_ID,
        name = "Device default",
        micSensitivityOffset = 0f,
        isDefault = true,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

private const val TEST_DEFAULT_CALIBRATION_PROFILE_ID = 1L
private const val TEST_CREATED_CALIBRATION_PROFILE_ID = 2L
