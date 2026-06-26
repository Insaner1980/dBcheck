package com.dbcheck.app.service

import com.dbcheck.app.domain.noise.AudibleAlarmEvaluation
import com.dbcheck.app.domain.noise.AudibleAlarmEvaluator
import com.dbcheck.app.domain.noise.AudibleAlarmPolicy
import javax.inject.Inject
import javax.inject.Singleton

interface AudibleAlarmPlayer {
    fun playAlarm(): Boolean

    fun previewAlarm(): Boolean
}

interface AudibleAlarmPlaybackGuard {
    fun startMonitoring()

    fun stopMonitoring()

    fun canPlayAudibleAlarm(): Boolean
}

enum class AudibleAlarmPlaybackResult {
    BelowThreshold,
    Waiting,
    CoolingDown,
    SkippedDisabled,
    SkippedFreeUser,
    SkippedGuard,
    PlaybackFailed,
    Played,
}

@Singleton
class AudibleAlarmPlaybackController(
    private val player: AudibleAlarmPlayer,
    private val playbackGuard: AudibleAlarmPlaybackGuard,
    private val policy: AudibleAlarmPolicy,
) {
    @Inject
    constructor(
        player: AudibleAlarmPlayer,
        playbackGuard: AudibleAlarmPlaybackGuard,
    ) : this(
        player = player,
        playbackGuard = playbackGuard,
        policy = AudibleAlarmPolicy.Default,
    )

    constructor(
        player: AudibleAlarmPlayer,
        playbackGuard: AudibleAlarmPlaybackGuard,
        thresholdDb: Float,
        requiredDurationMs: Long,
        cooldownMs: Long,
    ) : this(
        player = player,
        playbackGuard = playbackGuard,
        policy =
            AudibleAlarmPolicy(
                thresholdDb = thresholdDb,
                requiredDurationMs = requiredDurationMs,
                cooldownMs = cooldownMs,
            ),
    )

    private val evaluator = AudibleAlarmEvaluator()

    fun startMonitoring() {
        playbackGuard.startMonitoring()
    }

    fun stopMonitoring() {
        playbackGuard.stopMonitoring()
        reset()
    }

    fun reset() {
        evaluator.reset()
    }

    fun onReading(
        weightedDb: Float,
        timestampMs: Long,
        isEnabled: Boolean,
        isProUser: Boolean,
    ): AudibleAlarmPlaybackResult = disabledPlaybackResult(
            isEnabled = isEnabled,
            isProUser = isProUser,
        ) ?: when (
            evaluator.evaluate(
                weightedDb = weightedDb,
                timestampMs = timestampMs,
                policy = policy,
            )
        ) {
            is AudibleAlarmEvaluation.BelowThreshold -> AudibleAlarmPlaybackResult.BelowThreshold
            is AudibleAlarmEvaluation.Waiting -> AudibleAlarmPlaybackResult.Waiting
            is AudibleAlarmEvaluation.CoolingDown -> AudibleAlarmPlaybackResult.CoolingDown
            is AudibleAlarmEvaluation.Trigger -> playIfGuardAllows()
        }

    private fun disabledPlaybackResult(isEnabled: Boolean, isProUser: Boolean): AudibleAlarmPlaybackResult? {
        val result =
            when {
                !isProUser -> AudibleAlarmPlaybackResult.SkippedFreeUser
                !isEnabled -> AudibleAlarmPlaybackResult.SkippedDisabled
                else -> null
            }
        if (result != null) {
            evaluator.reset()
        }
        return result
    }

    fun preview(isProUser: Boolean): AudibleAlarmPlaybackResult {
        if (!isProUser) return AudibleAlarmPlaybackResult.SkippedFreeUser
        return if (player.previewAlarm()) {
            AudibleAlarmPlaybackResult.Played
        } else {
            AudibleAlarmPlaybackResult.PlaybackFailed
        }
    }

    private fun playIfGuardAllows(): AudibleAlarmPlaybackResult {
        if (!playbackGuard.canPlayAudibleAlarm()) return AudibleAlarmPlaybackResult.SkippedGuard
        return if (player.playAlarm()) {
            AudibleAlarmPlaybackResult.Played
        } else {
            AudibleAlarmPlaybackResult.PlaybackFailed
        }
    }
}
