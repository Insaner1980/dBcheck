package com.dbcheck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dbcheck.app.data.local.db.entity.HearingTestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HearingTestDao {

    @Insert
    suspend fun insertResult(result: HearingTestResultEntity): Long

    @Query("SELECT * FROM hearing_test_results WHERE id = :id")
    fun getResultById(id: Long): Flow<HearingTestResultEntity?>

    @Query("SELECT * FROM hearing_test_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<HearingTestResultEntity>>

    @Query("SELECT * FROM hearing_test_results ORDER BY timestamp DESC LIMIT 1")
    fun getLatestResult(): Flow<HearingTestResultEntity?>
}
