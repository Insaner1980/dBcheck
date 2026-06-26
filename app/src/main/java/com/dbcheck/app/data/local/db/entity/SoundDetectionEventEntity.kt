package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dbcheck.app.data.local.db.DbCheckSchema

@Entity(
    tableName = "sound_detection_events",
    indices = [
        Index(
            value = ["sessionId", "timestamp"],
            name = DbCheckSchema.INDEX_SOUND_DETECTION_EVENTS_SESSION_ID_TIMESTAMP,
        ),
        Index(value = ["timestamp"], name = DbCheckSchema.INDEX_SOUND_DETECTION_EVENTS_TIMESTAMP),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SoundDetectionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val label: String,
    val confidence: Float,
)
