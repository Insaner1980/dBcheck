package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dbcheck.app.data.local.db.DbCheckSchema

@Entity(
    tableName = "hearing_test_results",
    indices = [
        Index(value = ["timestamp"], name = DbCheckSchema.INDEX_HEARING_TEST_RESULTS_TIMESTAMP),
    ],
)
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
