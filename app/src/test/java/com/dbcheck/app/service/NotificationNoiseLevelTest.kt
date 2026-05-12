package com.dbcheck.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationNoiseLevelTest {
    @Test
    fun fromDbUsesLockscreenMeterThresholds() {
        assertEquals(NotificationNoiseLevel.SAFE, NotificationNoiseLevel.fromDb(69.9f))
        assertEquals(NotificationNoiseLevel.ELEVATED, NotificationNoiseLevel.fromDb(70f))
        assertEquals(NotificationNoiseLevel.ELEVATED, NotificationNoiseLevel.fromDb(84.9f))
        assertEquals(NotificationNoiseLevel.DANGEROUS, NotificationNoiseLevel.fromDb(85f))
    }
}
