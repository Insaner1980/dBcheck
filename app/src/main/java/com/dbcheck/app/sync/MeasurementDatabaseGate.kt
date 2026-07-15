package com.dbcheck.app.sync

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementDatabaseGate
    @Inject
    constructor() {
        private val mutex = Mutex()

        fun tryAcquire(owner: Any): Boolean = mutex.tryLock(owner)

        fun release(owner: Any) {
            mutex.unlock(owner)
        }
    }
