package com.dbcheck.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.HearingTestResultEntity
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        MeasurementEntity::class,
        HearingTestResultEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DbCheckDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun measurementDao(): MeasurementDao

    abstract fun hearingTestDao(): HearingTestDao
}
