package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dbcheck.app.data.local.db.DbCheckSchema
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["activeSlot"], name = DbCheckSchema.INDEX_SESSIONS_ACTIVE_SLOT, unique = true),
        Index(value = ["isActive", "startTime"], name = DbCheckSchema.INDEX_SESSIONS_IS_ACTIVE_START_TIME),
        Index(value = ["startTime"], name = DbCheckSchema.INDEX_SESSIONS_START_TIME),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val minDb: Float = 0f,
    val avgDb: Float = 0f,
    val maxDb: Float = 0f,
    val peakDb: Float = 0f,
    val name: String? = null,
    val emoji: String? = null,
    val tags: String? = null,
    val isActive: Boolean = false,
    val activeSlot: Int? = if (isActive) DbCheckSchema.ACTIVE_SESSION_SLOT else null,
    val frequencyWeighting: String = UserPreferenceDefaults.FREQUENCY_WEIGHTING,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val locationCapturedAt: Long? = null,
)
