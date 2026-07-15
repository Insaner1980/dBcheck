package com.dbcheck.app.ui.navigation

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProRouteAccessViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun entitlementFlowStartsCollectingOnlyWhenObserved() = runTest {
        var subscriptionCount = 0
        val preferences =
            flow {
                subscriptionCount += 1
                emit(UserPreferences(isProUser = false))
            }
        val repository =
            mockk<PreferencesRepository> {
                every { userPreferences } returns preferences
            }
        val viewModel = ProRouteAccessViewModel(repository)
        advanceUntilIdle()

        assertEquals(0, subscriptionCount)

        assertEquals(false, viewModel.isProUser.first())

        assertEquals(1, subscriptionCount)
    }
}
