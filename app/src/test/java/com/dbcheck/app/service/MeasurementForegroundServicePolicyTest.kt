package com.dbcheck.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun stopRequestParsesCompletionModeFromStopCommand() {
        val intent =
            mockk<Intent> {
                every { action } returns MeasurementForegroundService.ACTION_STOP_MEASUREMENT
                every {
                    getBooleanExtra(MeasurementForegroundService.EXTRA_EMIT_COMPLETED, true)
                } returns false
            }

        val request = MeasurementForegroundServicePolicy.stopRequest(intent)

        assertEquals(false, request?.emitCompleted)
    }

    @Test
    fun regularStartCommandIsNotAStopRequest() {
        val intent =
            mockk<Intent> {
                every { action } returns null
            }

        assertNull(MeasurementForegroundServicePolicy.stopRequest(intent))
    }
}
