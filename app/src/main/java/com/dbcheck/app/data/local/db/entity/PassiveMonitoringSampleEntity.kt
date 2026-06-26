package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "passive_monitoring_samples",
    indices = [
        Index(value = ["startedAtMs"]),
        Index(value = ["endedAtMs"]),
    ],
)
data class PassiveMonitoringSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val readingCount: Int,
    val minDb: Float,
    val averageDb: Float,
    val maxDb: Float,
    val peakDb: Float,
    val totalEnergy: Double,
)
