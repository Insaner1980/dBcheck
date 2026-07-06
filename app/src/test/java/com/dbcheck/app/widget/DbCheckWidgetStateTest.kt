package com.dbcheck.app.widget

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DbCheckWidgetStateTest {
    @Test
    fun freeUserSeesProLockedWidgetStateEvenWhenSessionExists() {
        assertEquals(
            WidgetContentMode.PRO_LOCKED,
            widgetContentMode(
                isPro = false,
                lastSession = session(avgDb = 72f),
            ),
        )
    }

    @Test
    fun proUserSeesSessionWidgetOnlyWhenLatestSessionHasAverageDb() {
        assertEquals(
            WidgetContentMode.SESSION,
            widgetContentMode(
                isPro = true,
                lastSession = session(avgDb = 72f),
            ),
        )
        assertEquals(
            WidgetContentMode.EMPTY,
            widgetContentMode(
                isPro = true,
                lastSession = session(avgDb = 0f),
            ),
        )
    }

    @Test
    fun proUserWithoutSessionSeesEmptyWidgetState() {
        assertEquals(
            WidgetContentMode.EMPTY,
            widgetContentMode(
                isPro = true,
                lastSession = null,
            ),
        )
    }

    @Test
    fun widgetDataLoadFailureShowsErrorState() = runTest {
        val data =
            loadWidgetData(userPreferences = MutableStateFlow(UserPreferences(isProUser = true))) {
                throw IllegalStateException("db")
            }

        assertEquals(WidgetContentMode.ERROR, widgetContentMode(data))
    }

    private fun session(avgDb: Float): Session = Session(
        id = 7L,
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_060_000L,
        avgDb = avgDb,
        minDb = avgDb,
        maxDb = avgDb,
        peakDb = avgDb,
        name = null,
        emoji = null,
        tags = emptyList(),
        isActive = false,
        frequencyWeighting = "A",
    )
}
