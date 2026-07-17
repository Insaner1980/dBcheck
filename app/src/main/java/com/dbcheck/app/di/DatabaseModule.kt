package com.dbcheck.app.di

import android.content.Context
import androidx.room.Room
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.data.local.db.DbCheckMigrations
import com.dbcheck.app.data.local.db.dao.CalibrationProfileDao
import com.dbcheck.app.data.local.db.dao.HearingRecoveryDao
import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.PassiveMonitoringDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.dao.SleepSessionDao
import com.dbcheck.app.data.local.db.dao.SoundDetectionEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DbCheckDatabase = Room
            .databaseBuilder(
                context,
                DbCheckDatabase::class.java,
                DbCheckDatabase.DATABASE_NAME,
            ).addMigrations(
                DbCheckMigrations.MIGRATION_1_2,
                DbCheckMigrations.MIGRATION_2_3,
                DbCheckMigrations.MIGRATION_3_4,
                DbCheckMigrations.MIGRATION_4_5,
                DbCheckMigrations.MIGRATION_5_6,
                DbCheckMigrations.MIGRATION_6_7,
                DbCheckMigrations.MIGRATION_7_8,
                DbCheckMigrations.MIGRATION_8_9,
                DbCheckMigrations.MIGRATION_9_10,
                DbCheckMigrations.MIGRATION_10_11,
                DbCheckMigrations.MIGRATION_11_12,
                DbCheckMigrations.MIGRATION_12_13,
            )
            .build()

    @Provides
    fun provideSessionDao(db: DbCheckDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMeasurementDao(db: DbCheckDatabase): MeasurementDao = db.measurementDao()

    @Provides
    fun provideHearingTestDao(db: DbCheckDatabase): HearingTestDao = db.hearingTestDao()

    @Provides
    fun provideHearingRecoveryDao(db: DbCheckDatabase): HearingRecoveryDao = db.hearingRecoveryDao()

    @Provides
    fun provideSoundDetectionEventDao(db: DbCheckDatabase): SoundDetectionEventDao = db.soundDetectionEventDao()

    @Provides
    fun provideCalibrationProfileDao(db: DbCheckDatabase): CalibrationProfileDao = db.calibrationProfileDao()

    @Provides
    fun provideSleepSessionDao(db: DbCheckDatabase): SleepSessionDao = db.sleepSessionDao()

    @Provides
    fun providePassiveMonitoringDao(db: DbCheckDatabase): PassiveMonitoringDao = db.passiveMonitoringDao()
}
