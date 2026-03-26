package com.dbcheck.app.billing

import com.dbcheck.app.billing.model.ProFeature
import com.dbcheck.app.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProFeatureManager @Inject constructor(
    private val billingManager: BillingManager,
    private val preferencesRepository: PreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val isProUser: StateFlow<Boolean> = billingManager.isPurchased

    init {
        // Sync billing state with preferences
        scope.launch {
            billingManager.isPurchased.collect { isPro ->
                preferencesRepository.updateProUser(isPro)
            }
        }
    }

    fun isFeatureUnlocked(feature: ProFeature): Boolean =
        isProUser.value
}
