package com.dbcheck.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.dao.SoundDetectionEventDao
import com.dbcheck.app.data.local.db.entity.HearingTestResultEntity
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity

@Database(
    entities = [
        SessionEntity::class,
        MeasurementEntity::class,
        HearingTestResultEntity::class,
        SoundDetectionEventEntity::class,
    ],
    version = DbCheckDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
abstract class DbCheckDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun measurementDao(): MeasurementDao

    abstract fun hearingTestDao(): HearingTestDao

    abstract fun soundDetectionEventDao(): SoundDetectionEventDao

    companion object {
        const val DATABASE_NAME = "dbcheck.db"
        const val SCHEMA_VERSION = 6
    }
}
