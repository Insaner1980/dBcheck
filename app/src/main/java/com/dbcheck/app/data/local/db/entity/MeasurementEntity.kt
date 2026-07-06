package com.dbcheck.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dbcheck.app.data.local.db.DbCheckSchema
import com.dbcheck.app.domain.audio.ResponseTime

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId", "timestamp"], name = DbCheckSchema.INDEX_MEASUREMENTS_SESSION_ID_TIMESTAMP),
        Index(value = ["timestamp"], name = DbCheckSchema.INDEX_MEASUREMENTS_TIMESTAMP),
    ],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val dbValue: Float,
    val dbWeighted: Float,
    @ColumnInfo(defaultValue = "0") val peakDb: Float = dbWeighted,
    @ColumnInfo(defaultValue = "0") val aWeightedDb: Float = dbWeighted,
    @ColumnInfo(defaultValue = "'FAST'") val responseTime: String = ResponseTime.FAST.name,
    val frequencyData: String? = null,
)
