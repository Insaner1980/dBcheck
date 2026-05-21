package com.dbcheck.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.projectFile
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
    fun foregroundPromotionFailureMapsToRecordingStartFailure() {
        assertEquals(
            AudioRecordingFailure.StartFailed,
            MeasurementForegroundServicePolicy.recordingFailureForForegroundStart(foregroundStarted = false),
        )
        assertNull(MeasurementForegroundServicePolicy.recordingFailureForForegroundStart(foregroundStarted = true))
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

    @Test
    fun notificationReadingsAreScopedToCurrentServiceStart() {
        val serviceStartTimeMs = 10_000L

        assertFalse(
            MeasurementForegroundServicePolicy.shouldUseReadingForNotification(
                readingTimestamp = serviceStartTimeMs - 1,
                serviceStartTimeMs = serviceStartTimeMs,
            ),
        )
        assertTrue(
            MeasurementForegroundServicePolicy.shouldUseReadingForNotification(
                readingTimestamp = serviceStartTimeMs,
                serviceStartTimeMs = serviceStartTimeMs,
            ),
        )
    }

    @Test
    fun duplicateStartCommandDoesNotResetActiveNotificationLoop() {
        assertTrue(
            MeasurementForegroundServicePolicy.shouldIgnoreDuplicateStart(updateLoopActive = true),
        )
        assertFalse(
            MeasurementForegroundServicePolicy.shouldIgnoreDuplicateStart(updateLoopActive = false),
        )
    }

    @Test
    fun initialNotificationDurationUsesSharedClockFormatter() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/MeasurementForegroundService.kt").readText()

        assertFalse(source.contains("duration = \"00:00\""))
        assertTrue(source.contains("DurationFormatter.formatClockDuration(0L)"))
    }
}
