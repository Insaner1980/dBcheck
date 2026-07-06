package com.dbcheck.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.sleep.SleepRecordingConfig
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
    fun sleepStartRequestParsesRecordingModeAndPreparedOptions() {
        val intent =
            mockk<Intent> {
                every { getStringExtra(MeasurementForegroundService.EXTRA_RECORDING_MODE) } returns
                    MeasurementRecordingMode.Sleep.intentValue
                every {
                    getIntExtra(
                        MeasurementForegroundService.EXTRA_SLEEP_TARGET_DURATION_MINUTES,
                        SleepRecordingConfig.DEFAULT_TARGET_DURATION_MINUTES,
                    )
                } returns 600
                every {
                    getBooleanExtra(
                        MeasurementForegroundService.EXTRA_SLEEP_KEEP_AWAKE_ENABLED,
                        SleepRecordingConfig.DEFAULT_KEEP_AWAKE_ENABLED,
                    )
                } returns true
            }

        val request = MeasurementForegroundServicePolicy.startRequest(intent)

        assertEquals(
            MeasurementStartRequest(
                recordingMode = MeasurementRecordingMode.Sleep,
                sleepRecordingConfig =
                    SleepRecordingConfig(
                        targetDurationMinutes = 600,
                        keepAwakeEnabled = true,
                    ),
            ),
            request,
        )
    }

    @Test
    fun passiveStartRequestParsesRecordingModeWithoutSessionOptions() {
        val intent =
            mockk<Intent> {
                every { getStringExtra(MeasurementForegroundService.EXTRA_RECORDING_MODE) } returns
                    MeasurementRecordingMode.Passive.intentValue
            }

        val request = MeasurementForegroundServicePolicy.startRequest(intent)

        assertEquals(
            MeasurementStartRequest(
                recordingMode = MeasurementRecordingMode.Passive,
                sleepRecordingConfig = null,
            ),
            request,
        )
    }

    @Test
    fun sleepTargetStopsOnlySleepModeAfterConfiguredDuration() {
        assertFalse(
            MeasurementForegroundServicePolicy.shouldStopForSleepTarget(
                recordingMode = MeasurementRecordingMode.Meter,
                elapsedMs = 600L * 60_000L,
                targetDurationMinutes = 600,
            ),
        )
        assertFalse(
            MeasurementForegroundServicePolicy.shouldStopForSleepTarget(
                recordingMode = MeasurementRecordingMode.Sleep,
                elapsedMs = 599L * 60_000L,
                targetDurationMinutes = 600,
            ),
        )
        assertTrue(
            MeasurementForegroundServicePolicy.shouldStopForSleepTarget(
                recordingMode = MeasurementRecordingMode.Sleep,
                elapsedMs = 600L * 60_000L,
                targetDurationMinutes = 600,
            ),
        )
    }

    @Test
    fun passiveTargetStopsOnlyPassiveModeAfterDefaultSampleDuration() {
        assertFalse(
            MeasurementForegroundServicePolicy.shouldStopForPassiveTarget(
                recordingMode = MeasurementRecordingMode.Meter,
                elapsedMs = 5L * 60_000L,
            ),
        )
        assertFalse(
            MeasurementForegroundServicePolicy.shouldStopForPassiveTarget(
                recordingMode = MeasurementRecordingMode.Passive,
                elapsedMs = 5L * 60_000L - 1L,
            ),
        )
        assertTrue(
            MeasurementForegroundServicePolicy.shouldStopForPassiveTarget(
                recordingMode = MeasurementRecordingMode.Passive,
                elapsedMs = 5L * 60_000L,
            ),
        )
    }

    @Test
    fun initialNotificationDurationUsesSharedClockFormatter() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/MeasurementForegroundService.kt").readText()

        assertFalse(source.contains("duration = \"00:00\""))
        assertTrue(source.contains("DurationFormatter.formatClockDuration(0L)"))
    }
}
