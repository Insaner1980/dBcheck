package com.dbcheck.app.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

internal fun createInMemoryDbCheckDatabase(): DbCheckDatabase = Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DbCheckDatabase::class.java,
        )
        .allowMainThreadQueries()
        .build()
