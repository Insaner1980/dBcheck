package com.dbcheck.app.billing

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProFeatureManagerStartupTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val billingPurchaseState = MutableStateFlow<Boolean?>(null)
    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = true))
    private val billingManager =
        mockk<BillingManager> {
            every { isPurchased } returns billingPurchaseState
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
            coEvery { updateProUser(any()) } just runs
        }

    @Test
    fun startupDoesNotOverwriteStoredProStateBeforeBillingQueryCompletes() =
        runTest {
            ProFeatureManager(
                billingManager = billingManager,
                preferencesRepository = preferencesRepository,
                mainDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
            advanceUntilIdle()

            coVerify(exactly = 0) { preferencesRepository.updateProUser(any()) }
        }

    @Test
    fun completedBillingQuerySyncsPurchaseStateToPreferences() =
        runTest {
            ProFeatureManager(
                billingManager = billingManager,
                preferencesRepository = preferencesRepository,
                mainDispatcher = UnconfinedTestDispatcher(testScheduler),
            )

            billingPurchaseState.value = false
            advanceUntilIdle()

            coVerify { preferencesRepository.updateProUser(false) }
        }
}
