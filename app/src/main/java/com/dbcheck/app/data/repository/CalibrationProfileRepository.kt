package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.CalibrationProfileDao
import com.dbcheck.app.data.local.db.entity.CalibrationProfileEntity
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.calibration.CalibrationProfile
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class CalibrationProfileDeleteResult {
    Deleted,
    BlockedLastDefault,
    NotFound,
}

@Singleton
class CalibrationProfileRepository
    @Inject
    constructor(
        private val calibrationProfileDao: CalibrationProfileDao,
    ) {
        fun observeProfiles(): Flow<List<CalibrationProfile>> = calibrationProfileDao.observeProfiles()
                .map { profiles -> profiles.map { it.toDomain() } }

        suspend fun getProfile(profileId: Long): CalibrationProfile? =
            calibrationProfileDao.getProfile(profileId)?.toDomain()

        suspend fun createProfile(
            name: String,
            micSensitivityOffset: Float,
            isDefault: Boolean = false,
            timestampMillis: Long,
        ): Long = calibrationProfileDao.insertProfile(
                CalibrationProfileEntity(
                    name = name,
                    micSensitivityOffset = UserPreferenceDefaults.normalizeMicSensitivityOffset(micSensitivityOffset),
                    octaveBandOffsets = OctaveCalibrationOffsets.STORAGE_ZERO,
                    isDefault = isDefault,
                    createdAt = timestampMillis,
                    updatedAt = timestampMillis,
                ),
            )

        suspend fun renameProfile(profileId: Long, name: String, timestampMillis: Long) {
            calibrationProfileDao.renameProfile(
                profileId = profileId,
                name = normalizeProfileName(name),
                updatedAt = timestampMillis,
            )
        }

        suspend fun updateOctaveBandOffsets(
            profileId: Long,
            offsets: OctaveCalibrationOffsets,
            timestampMillis: Long,
        ) {
            calibrationProfileDao.updateOctaveBandOffsets(
                profileId = profileId,
                octaveBandOffsets = offsets.toStorageString(),
                updatedAt = timestampMillis,
            )
        }

        suspend fun resetOctaveBandOffsets(profileId: Long, timestampMillis: Long) {
            updateOctaveBandOffsets(
                profileId = profileId,
                offsets = OctaveCalibrationOffsets.zero(),
                timestampMillis = timestampMillis,
            )
    }

    suspend fun deleteProfile(profileId: Long): CalibrationProfileDeleteResult {
        val profile = calibrationProfileDao.getProfile(profileId)
        return when {
            profile == null -> CalibrationProfileDeleteResult.NotFound

            profile.isDefault && calibrationProfileDao.getDefaultProfileCount() <= 1 ->
                CalibrationProfileDeleteResult.BlockedLastDefault

            calibrationProfileDao.deleteProfile(profileId) > 0 -> CalibrationProfileDeleteResult.Deleted

            else -> CalibrationProfileDeleteResult.NotFound
        }
    }
}

private fun normalizeProfileName(name: String): String = name.trim().ifBlank { "Calibration profile" }

private fun CalibrationProfileEntity.toDomain(): CalibrationProfile = CalibrationProfile(
    id = id,
    name = name,
    micSensitivityOffset = micSensitivityOffset,
    octaveCalibrationOffsets = OctaveCalibrationOffsets.fromStorageString(octaveBandOffsets),
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
