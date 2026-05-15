package com.dbcheck.app.di

import com.dbcheck.app.sync.BackupGateway
import com.dbcheck.app.sync.LocalBackupManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    @Provides
    @Singleton
    fun provideBackupGateway(manager: LocalBackupManager): BackupGateway = manager
}
