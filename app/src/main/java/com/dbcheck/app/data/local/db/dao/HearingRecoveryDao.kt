package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.HearingRecoveryResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HearingRecoveryDao {
    @Insert
    suspend fun insertResult(result: HearingRecoveryResultEntity): Long

    @Query("SELECT * FROM hearing_recovery_results ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun getLatestResult(): Flow<HearingRecoveryResultEntity?>
}
