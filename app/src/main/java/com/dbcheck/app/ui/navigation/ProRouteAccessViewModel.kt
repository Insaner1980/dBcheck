package com.dbcheck.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.dbcheck.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
internal class ProRouteAccessViewModel
    @Inject
    constructor(preferencesRepository: PreferencesRepository) :
    ViewModel() {
        val isProUser: Flow<Boolean> = preferencesRepository.userPreferences.map { it.isProUser }
    }
