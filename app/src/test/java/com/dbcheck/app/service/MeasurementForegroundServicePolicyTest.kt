package com.dbcheck.app.service

import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
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

    @Test
    fun microphoneForegroundServiceTypeIsUsedOnlyFromAndroid11() {
        assertEquals(0, MeasurementForegroundServicePolicy.foregroundServiceType(Build.VERSION_CODES.Q))
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            MeasurementForegroundServicePolicy.foregroundServiceType(Build.VERSION_CODES.R),
        )
    }
}
