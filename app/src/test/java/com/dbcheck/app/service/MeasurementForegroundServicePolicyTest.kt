package com.dbcheck.app.service

import android.app.Service
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementForegroundServicePolicyTest {
    @Test
    fun audioSessionStartsOnlyAfterForegroundPromotionSucceeds() {
        assertFalse(MeasurementForegroundServicePolicy.shouldStartAudioSession(foregroundStarted = false))
        assertTrue(MeasurementForegroundServicePolicy.shouldStartAudioSession(foregroundStarted = true))
    }

    @Test
    fun successfulForegroundServiceStartIsNotSticky() {
        assertEquals(Service.START_NOT_STICKY, MeasurementForegroundServicePolicy.successStartResult)
    }
}
