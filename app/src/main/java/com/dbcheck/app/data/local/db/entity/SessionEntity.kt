package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
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
    val frequencyWeighting: String = "A",
)
