package com.dbcheck.app.ui.navigation

sealed class Screen(val route: String) {
    data object Meter : Screen("meter")
    data object Analytics : Screen("analytics")
    data object History : Screen("history")
    data object Settings : Screen("settings") {
        const val ROUTE_WITH_ARGS = "settings?showPro={showPro}"
        fun createRoute(showPro: Boolean = false) = "settings?showPro=$showPro"
    }

    // Phase 2
    data object HearingTestSetup : Screen("hearing_test/setup")
    data object HearingTestActive : Screen("hearing_test/active")
    data object HearingTestResults : Screen("hearing_test/results/{testId}") {
        fun createRoute(testId: Long) = "hearing_test/results/$testId"
    }
}
