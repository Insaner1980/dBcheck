package com.dbcheck.app.di

import android.content.Context
import androidx.room.Room
import com.dbcheck.app.data.local.db.DbCheckDatabase
import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
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
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): DbCheckDatabase =
        Room
            .databaseBuilder(
                context,
                DbCheckDatabase::class.java,
                "dbcheck.db",
            ).build()

    @Provides
    fun provideSessionDao(db: DbCheckDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMeasurementDao(db: DbCheckDatabase): MeasurementDao = db.measurementDao()

    @Provides
    fun provideHearingTestDao(db: DbCheckDatabase): HearingTestDao = db.hearingTestDao()
}
