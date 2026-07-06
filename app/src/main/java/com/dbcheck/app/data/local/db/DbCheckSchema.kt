package com.dbcheck.app.data.local.db

object DbCheckSchema {
    const val ACTIVE_SESSION_SLOT = 1

    const val INDEX_SESSIONS_ACTIVE_SLOT = "index_sessions_activeSlot"
    const val INDEX_SESSIONS_IS_ACTIVE_START_TIME = "index_sessions_isActive_startTime"
    const val INDEX_SESSIONS_START_TIME = "index_sessions_startTime"
    const val INDEX_MEASUREMENTS_SESSION_ID = "index_measurements_sessionId"
    const val INDEX_MEASUREMENTS_SESSION_ID_TIMESTAMP = "index_measurements_sessionId_timestamp"
    const val INDEX_MEASUREMENTS_TIMESTAMP = "index_measurements_timestamp"
    const val INDEX_HEARING_TEST_RESULTS_TIMESTAMP = "index_hearing_test_results_timestamp"
    const val INDEX_HEARING_RECOVERY_RESULTS_TIMESTAMP = "index_hearing_recovery_results_timestamp"
    const val INDEX_HEARING_RECOVERY_RESULTS_BASELINE_TEST_ID =
        "index_hearing_recovery_results_baselineTestId"
    const val INDEX_SOUND_DETECTION_EVENTS_SESSION_ID_TIMESTAMP =
        "index_sound_detection_events_sessionId_timestamp"
    const val INDEX_SOUND_DETECTION_EVENTS_TIMESTAMP = "index_sound_detection_events_timestamp"
    const val INDEX_CALIBRATION_PROFILES_NAME = "index_calibration_profiles_name"
    const val INDEX_SLEEP_NOTABLE_EVENTS_SESSION_ID_TIMESTAMP =
        "index_sleep_notable_events_sessionId_timestamp"
    const val INDEX_SLEEP_NOTABLE_EVENTS_TIMESTAMP = "index_sleep_notable_events_timestamp"
    const val INDEX_PASSIVE_MONITORING_SAMPLES_STARTED_AT_MS =
        "index_passive_monitoring_samples_startedAtMs"
    const val INDEX_PASSIVE_MONITORING_SAMPLES_ENDED_AT_MS =
        "index_passive_monitoring_samples_endedAtMs"
}
