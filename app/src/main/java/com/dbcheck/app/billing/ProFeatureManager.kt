package com.dbcheck.app.billing

import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.di.MainDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProFeatureManager
    @Inject
    constructor(
        private val billingManager: BillingManager,
        private val preferencesRepository: PreferencesRepository,
        @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

        val isProUser: StateFlow<Boolean> =
            preferencesRepository.userPreferences
                .map { it.isProUser }
                .stateIn(scope, SharingStarted.Eagerly, false)

        init {
            // Synkkaa vain Play Billingin varmistama tila, ei tuntematonta alkutilaa.
            scope.launch {
                billingManager.isPurchased.collect { isPro ->
                    isPro?.let { preferencesRepository.updateProUser(it) }
                }
            }
        }
    }
