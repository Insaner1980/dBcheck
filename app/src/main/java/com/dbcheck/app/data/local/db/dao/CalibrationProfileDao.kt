package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.CalibrationProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalibrationProfileDao {
    @Insert
    suspend fun insertProfile(profile: CalibrationProfileEntity): Long

    @Query("SELECT * FROM calibration_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfile(profileId: Long): CalibrationProfileEntity?

    @Query("SELECT COUNT(*) FROM calibration_profiles WHERE isDefault = 1")
    suspend fun getDefaultProfileCount(): Int

    @Query("UPDATE calibration_profiles SET name = :name, updatedAt = :updatedAt WHERE id = :profileId")
    suspend fun renameProfile(profileId: Long, name: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE calibration_profiles
        SET octaveBandOffsets = :octaveBandOffsets, updatedAt = :updatedAt
        WHERE id = :profileId
        """,
    )
    suspend fun updateOctaveBandOffsets(profileId: Long, octaveBandOffsets: String, updatedAt: Long): Int

    @Query("DELETE FROM calibration_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: Long): Int

    @Query(
        """
        SELECT * FROM calibration_profiles
        ORDER BY isDefault DESC, updatedAt DESC, id DESC
        """,
    )
    fun observeProfiles(): Flow<List<CalibrationProfileEntity>>
}
