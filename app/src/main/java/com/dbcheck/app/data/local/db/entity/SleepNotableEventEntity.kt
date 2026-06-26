package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dbcheck.app.data.local.db.DbCheckSchema

@Entity(
    tableName = "sleep_notable_events",
    indices = [
        Index(
            value = ["sessionId", "timestamp"],
            name = DbCheckSchema.INDEX_SLEEP_NOTABLE_EVENTS_SESSION_ID_TIMESTAMP,
        ),
        Index(value = ["timestamp"], name = DbCheckSchema.INDEX_SLEEP_NOTABLE_EVENTS_TIMESTAMP),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SleepNotableEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val eventType: String,
    val levelDb: Float? = null,
    val durationMs: Long? = null,
)
