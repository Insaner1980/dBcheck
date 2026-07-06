package com.dbcheck.app.domain.sleep

data class SleepRecordingConfig(
    val targetDurationMinutes: Int = DEFAULT_TARGET_DURATION_MINUTES,
    val keepAwakeEnabled: Boolean = DEFAULT_KEEP_AWAKE_ENABLED,
) {
    val targetDurationMs: Long
        get() = targetDurationMinutes * 60_000L

    companion object {
        const val DEFAULT_TARGET_DURATION_MINUTES = 480
        const val DEFAULT_KEEP_AWAKE_ENABLED = false
        val TARGET_DURATION_OPTIONS_MINUTES = listOf(360, DEFAULT_TARGET_DURATION_MINUTES, 600)

        fun fromPreparedOptions(targetDurationMinutes: Int, keepAwakeEnabled: Boolean): SleepRecordingConfig =
            SleepRecordingConfig(
                targetDurationMinutes =
                    targetDurationMinutes.takeIf { it in TARGET_DURATION_OPTIONS_MINUTES }
                        ?: DEFAULT_TARGET_DURATION_MINUTES,
                keepAwakeEnabled = keepAwakeEnabled,
            )
    }
}
