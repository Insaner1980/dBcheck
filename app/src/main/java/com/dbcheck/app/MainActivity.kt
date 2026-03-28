package com.dbcheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.ui.navigation.DbCheckNavHost
import com.dbcheck.app.ui.theme.DbCheckTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by preferencesRepository.userPreferences
                .collectAsStateWithLifecycle(initialValue = null)
            val darkTheme =
                when (prefs?.themeMode) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
            DbCheckTheme(darkTheme = darkTheme) {
                DbCheckNavHost()
            }
        }
    }
}
