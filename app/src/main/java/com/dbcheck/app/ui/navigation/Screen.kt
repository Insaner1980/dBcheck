package com.dbcheck.app.ui.navigation

sealed class Screen(val route: String) {
    data object Meter : Screen("meter")

    data object Analytics : Screen("analytics")

    data object Hearing : Screen("hearing")

    data object History : Screen("history")

    data object SessionDetail : Screen("history/detail/{sessionId}") {
        const val ARG_SESSION_ID = "sessionId"

        fun createRoute(sessionId: Long) = "history/detail/$sessionId"
    }

    data object Settings : Screen("settings") {
        const val ARG_SHOW_PRO = "showPro"
        const val ROUTE_WITH_ARGS = "settings?$ARG_SHOW_PRO={$ARG_SHOW_PRO}"
        const val HOME_ROUTE = "settings/home"
        const val CALIBRATION_ROUTE = "settings/calibration"
        const val OCTAVE_CALIBRATION_ROUTE = "settings/calibration/octave"
        const val NOTIFICATIONS_ROUTE = "settings/notifications"
        const val DATA_PRIVACY_ROUTE = "settings/data_privacy"
        const val DISPLAY_ROUTE = "settings/display"
        const val PRO_ABOUT_ROUTE = "settings/pro_about"

        fun createRoute(showPro: Boolean = false) = if (showPro) PRO_ABOUT_ROUTE else HOME_ROUTE
    }

    data object CameraOverlay : Screen("camera_overlay")

    data object SleepSetup : Screen("sleep/setup")

    data object HearingTestSetup : Screen("hearing_test/setup")

    data object HearingRecoverySetup : Screen("hearing_test/recovery/setup")

    data object HearingTestActive : Screen("hearing_test/active")

    data object HearingRecoveryActive : Screen("hearing_test/recovery/active")

    data object TinnitusPitch : Screen("tinnitus/pitch")

    data object AmbientSoundPlayback : Screen("ambient/playback")

    data object HearingTestResults : Screen("hearing_test/results/{testId}") {
        const val ARG_TEST_ID = "testId"

        fun createRoute(testId: Long) = "hearing_test/results/$testId"
    }
}
