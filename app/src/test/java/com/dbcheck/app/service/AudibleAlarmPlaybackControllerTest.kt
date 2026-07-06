package com.dbcheck.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AudibleAlarmPlaybackControllerTest {
    private val player = FakeAudibleAlarmPlayer()
    private val guard = FakeAudibleAlarmPlaybackGuard()
    private val controller =
        AudibleAlarmPlaybackController(
            player = player,
            playbackGuard = guard,
            thresholdDb = 90f,
            requiredDurationMs = 10_000L,
            cooldownMs = 60_000L,
        )

    @Test
    fun freeUserDoesNotPlayAlarmEvenWhenEnabled() {
        assertEquals(
            AudibleAlarmPlaybackResult.SkippedFreeUser,
            controller.onReading(
                weightedDb = 95f,
                timestampMs = 20_000L,
                isEnabled = true,
                isProUser = false,
            ),
        )

        assertEquals(0, player.playCount)
    }

    @Test
    fun disabledAlarmDoesNotPlayForProUser() {
        assertEquals(
            AudibleAlarmPlaybackResult.SkippedDisabled,
            controller.onReading(
                weightedDb = 95f,
                timestampMs = 20_000L,
                isEnabled = false,
                isProUser = true,
            ),
        )

        assertEquals(0, player.playCount)
    }

    @Test
    fun sustainedThresholdPlaysWhenProEnabledAndGuardAllowsPlayback() {
        assertEquals(
            AudibleAlarmPlaybackResult.Waiting,
            controller.onReading(
                weightedDb = 95f,
                timestampMs = 1_000L,
                isEnabled = true,
                isProUser = true,
            ),
        )

        assertEquals(
            AudibleAlarmPlaybackResult.Played,
            controller.onReading(
                weightedDb = 95f,
                timestampMs = 11_000L,
                isEnabled = true,
                isProUser = true,
            ),
        )

        assertEquals(1, player.playCount)
    }

    @Test
    fun screenOrPocketGuardSuppressesTriggeredPlayback() {
        guard.canPlay = false

        controller.onReading(
            weightedDb = 95f,
            timestampMs = 1_000L,
            isEnabled = true,
            isProUser = true,
        )

        assertEquals(
            AudibleAlarmPlaybackResult.SkippedGuard,
            controller.onReading(
                weightedDb = 95f,
                timestampMs = 11_000L,
                isEnabled = true,
                isProUser = true,
            ),
        )

        assertEquals(0, player.playCount)
    }

    @Test
    fun previewRequiresProButDoesNotUsePocketGuard() {
        guard.canPlay = false

        assertEquals(AudibleAlarmPlaybackResult.SkippedFreeUser, controller.preview(isProUser = false))
        assertEquals(AudibleAlarmPlaybackResult.Played, controller.preview(isProUser = true))

        assertEquals(1, player.previewCount)
    }

    @Test
    fun monitoringLifecycleDelegatesToGuard() {
        controller.startMonitoring()
        controller.stopMonitoring()

        assertEquals(1, guard.startCount)
        assertEquals(1, guard.stopCount)
    }
}

private class FakeAudibleAlarmPlayer : AudibleAlarmPlayer {
    var playCount = 0
    var previewCount = 0

    override fun playAlarm(): Boolean {
        playCount += 1
        return true
    }

    override fun previewAlarm(): Boolean {
        previewCount += 1
        return true
    }
}

private class FakeAudibleAlarmPlaybackGuard : AudibleAlarmPlaybackGuard {
    var canPlay = true
    var startCount = 0
    var stopCount = 0

    override fun startMonitoring() {
        startCount += 1
    }

    override fun stopMonitoring() {
        stopCount += 1
    }

    override fun canPlayAudibleAlarm(): Boolean = canPlay
}
