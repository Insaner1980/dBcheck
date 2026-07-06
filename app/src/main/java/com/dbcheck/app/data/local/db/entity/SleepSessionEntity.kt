package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SleepSessionEntity(
    @PrimaryKey val sessionId: Long,
    val targetDurationMinutes: Int,
    val keepAwakeEnabled: Boolean,
    val createdAt: Long,
)
