package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hearing_test_results")
data class HearingTestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val overallScore: Int,
    val rating: String,
    val leftEarData: String,
    val rightEarData: String,
    val speechClarity: Float,
    val highFreqLimit: Float,
    val avgThreshold: Float,
)
