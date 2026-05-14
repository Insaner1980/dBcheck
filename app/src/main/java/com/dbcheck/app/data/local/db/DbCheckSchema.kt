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
}
