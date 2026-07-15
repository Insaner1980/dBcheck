package com.dbcheck.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
internal fun ProRouteAccessGate(
    onNavigateToUpgrade: () -> Unit,
    viewModel: ProRouteAccessViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle(initialValue = null)
    val currentOnNavigateToUpgrade by rememberUpdatedState(onNavigateToUpgrade)

    LaunchedEffect(isProUser) {
        if (isProUser == false) {
            currentOnNavigateToUpgrade()
        }
    }

    if (isProUser == true) {
        content()
    }
}
