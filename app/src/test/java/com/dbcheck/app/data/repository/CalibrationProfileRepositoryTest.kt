package com.dbcheck.app.data.repository

import android.app.Application
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.data.local.db.createInMemoryDbCheckDatabase
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.calibration.CalibrationOffsetPolicy
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalibrationProfileRepositoryTest {
    private lateinit var database: DbCheckDatabase
    private lateinit var repository: CalibrationProfileRepository

    @Before
    fun setUp() {
        database = createInMemoryDbCheckDatabase()
        repository = CalibrationProfileRepository(database.calibrationProfileDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createProfilePersistsCalibrationProfileWithoutUi() = runTest {
        val profileId = repository.createProfile(
            name = "Reference mic",
            micSensitivityOffset = 2.5f,
            isDefault = true,
            timestampMillis = 1_700_000_000_000L,
        )

        val profiles = repository.observeProfiles().first()

        assertEquals(1, profiles.size)
        assertEquals(profileId, profiles.single().id)
        assertEquals("Reference mic", profiles.single().name)
        assertEquals(2.5f, profiles.single().micSensitivityOffset, 0f)
        assertTrue(profiles.single().octaveCalibrationOffsets.isZero)
        assertEquals(true, profiles.single().isDefault)
        assertEquals(1_700_000_000_000L, profiles.single().createdAt)
        assertEquals(1_700_000_000_000L, profiles.single().updatedAt)
    }

    @Test
    fun createProfileNormalizesMicSensitivityOffsetWithExistingPreferencePolicy() = runTest {
        repository.createProfile(
            name = "High offset",
            micSensitivityOffset = 25f,
            timestampMillis = 1_700_000_000_000L,
        )

        val profile = repository.observeProfiles().first().single()

        assertEquals(UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET_MAX, profile.micSensitivityOffset, 0f)
    }

    @Test
    fun getProfileReturnsOctaveOffsetsForRtaAndPdfConsumers() = runTest {
        val profileId = repository.createProfile(
            name = "Field mic",
            micSensitivityOffset = 0f,
            timestampMillis = 1_700_000_000_000L,
        )
        val offsets =
            OctaveCalibrationOffsets.zero()
                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 3f)

        repository.updateOctaveBandOffsets(
            profileId = profileId,
            offsets = offsets,
            timestampMillis = 1_700_000_060_000L,
        )

        val profile = repository.getProfile(profileId)

        checkNotNull(profile)
        assertEquals(3f, profile.octaveCalibrationOffsets.offsetFor(1_000f), 0f)
        assertEquals(1_700_000_060_000L, profile.updatedAt)
    }

    @Test
    fun updateOctaveBandOffsetsPersistsClampedBandOffsetsAndTimestamp() = runTest {
        val profileId = repository.createProfile(
            name = "Field mic",
            micSensitivityOffset = 0f,
            timestampMillis = 1_700_000_000_000L,
        )
        val offsets =
            OctaveCalibrationOffsets.zero()
                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 25f)

        repository.updateOctaveBandOffsets(
            profileId = profileId,
            offsets = offsets,
            timestampMillis = 1_700_000_060_000L,
        )

        val profile = repository.observeProfiles().first().single()
        assertEquals(CalibrationOffsetPolicy.MAX_OFFSET_DB, profile.octaveCalibrationOffsets.offsetFor(1_000f), 0f)
        assertEquals(1_700_000_060_000L, profile.updatedAt)
    }

    @Test
    fun resetOctaveBandOffsetsClearsBandOffsets() = runTest {
        val profileId = repository.createProfile(
            name = "Field mic",
            micSensitivityOffset = 0f,
            timestampMillis = 1_700_000_000_000L,
        )
        repository.updateOctaveBandOffsets(
            profileId = profileId,
            offsets =
                OctaveCalibrationOffsets.zero()
                    .withOffset(centerFrequencyHz = 1_000f, offsetDb = 4f),
            timestampMillis = 1_700_000_060_000L,
        )

        repository.resetOctaveBandOffsets(
            profileId = profileId,
            timestampMillis = 1_700_000_120_000L,
        )

        val profile = repository.observeProfiles().first().single()
        assertTrue(profile.octaveCalibrationOffsets.isZero)
        assertEquals(1_700_000_120_000L, profile.updatedAt)
    }

    @Test
    fun renameProfileUpdatesNameAndTimestamp() = runTest {
        val profileId = repository.createProfile(
            name = "Original",
            micSensitivityOffset = 1f,
            timestampMillis = 1_700_000_000_000L,
        )

        repository.renameProfile(
            profileId = profileId,
            name = "  Field mic  ",
            timestampMillis = 1_700_000_060_000L,
        )

        val profile = repository.observeProfiles().first().single()
        assertEquals("Field mic", profile.name)
        assertEquals(1_700_000_000_000L, profile.createdAt)
        assertEquals(1_700_000_060_000L, profile.updatedAt)
    }

    @Test
    fun deleteProfileDeletesNonDefaultProfile() = runTest {
        repository.createProfile(
            name = "Device default",
            micSensitivityOffset = 0f,
            isDefault = true,
            timestampMillis = 1_700_000_000_000L,
        )
        val fieldProfileId = repository.createProfile(
            name = "Field mic",
            micSensitivityOffset = 1f,
            timestampMillis = 1_700_000_060_000L,
        )

        val result = repository.deleteProfile(fieldProfileId)

        assertEquals(CalibrationProfileDeleteResult.Deleted, result)
        assertEquals(listOf("Device default"), repository.observeProfiles().first().map { it.name })
    }

    @Test
    fun deleteProfileBlocksLastDefaultProfile() = runTest {
        val defaultProfileId = repository.createProfile(
            name = "Device default",
            micSensitivityOffset = 0f,
            isDefault = true,
            timestampMillis = 1_700_000_000_000L,
        )

        val result = repository.deleteProfile(defaultProfileId)

        assertEquals(CalibrationProfileDeleteResult.BlockedLastDefault, result)
        assertEquals(listOf("Device default"), repository.observeProfiles().first().map { it.name })
    }

    @Test
    fun concurrentDeleteProfileCallsPreserveOneDefaultProfile() = runTest {
        val defaultProfileIds = listOf("Device default", "Backup default").mapIndexed { index, name ->
            repository.createProfile(
                name = name,
                micSensitivityOffset = 0f,
                isDefault = true,
                timestampMillis = 1_700_000_000_000L + index,
            )
        }

        val results = defaultProfileIds.map { profileId ->
            async { repository.deleteProfile(profileId) }
        }.awaitAll()

        assertEquals(1, results.count { it == CalibrationProfileDeleteResult.Deleted })
        assertEquals(1, results.count { it == CalibrationProfileDeleteResult.BlockedLastDefault })
        assertEquals(1, repository.observeProfiles().first().count { it.isDefault })
    }
}
