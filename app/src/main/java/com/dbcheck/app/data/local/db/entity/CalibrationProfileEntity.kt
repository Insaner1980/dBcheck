package com.dbcheck.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dbcheck.app.data.local.db.DbCheckSchema
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.calibration.OctaveCalibrationOffsets

@Entity(
    tableName = "calibration_profiles",
    indices = [
        Index(value = ["name"], name = DbCheckSchema.INDEX_CALIBRATION_PROFILES_NAME),
    ],
)
data class CalibrationProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val micSensitivityOffset: Float = UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET,
    @ColumnInfo(defaultValue = "''")
    val octaveBandOffsets: String = OctaveCalibrationOffsets.STORAGE_ZERO,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
