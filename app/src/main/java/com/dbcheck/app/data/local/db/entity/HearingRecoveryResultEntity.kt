package com.dbcheck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dbcheck.app.data.local.db.DbCheckSchema

@Entity(
    tableName = "hearing_recovery_results",
    foreignKeys = [
        ForeignKey(
            entity = HearingTestResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["baselineTestId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["timestamp"], name = DbCheckSchema.INDEX_HEARING_RECOVERY_RESULTS_TIMESTAMP),
        Index(value = ["baselineTestId"], name = DbCheckSchema.INDEX_HEARING_RECOVERY_RESULTS_BASELINE_TEST_ID),
    ],
)
data class HearingRecoveryResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val baselineTestId: Long,
    val timestamp: Long,
    val testedFrequencyCount: Int,
    val averageShiftDb: Float,
    val maxShiftDb: Float,
    val status: String,
    val leftEarShiftData: String,
    val rightEarShiftData: String,
)
