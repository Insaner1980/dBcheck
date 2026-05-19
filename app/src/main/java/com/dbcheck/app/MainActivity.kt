package com.dbcheck.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.dbcheck.app.billing.BillingManager
import com.dbcheck.app.data.local.preferences.model.ThemeMode
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.ui.navigation.DbCheckNavHost
import com.dbcheck.app.ui.theme.DbCheckTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by preferencesRepository.userPreferences
                .collectAsStateWithLifecycle(initialValue = null)
            when (val themeState = resolveStartupThemeState(prefs, isSystemInDarkTheme())) {
                StartupThemeState.Loading -> Unit

                is StartupThemeState.Resolved -> {
                    DbCheckTheme(darkTheme = themeState.darkTheme) {
                        if (isHealthConnectPermissionDisclosureAction(intent?.action)) {
                            HealthConnectPermissionDisclosureScreen()
                        } else {
                            DbCheckNavHost(onRestartAfterRestore = ::restartApplication)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            billingManager.refreshPurchases()
        }
    }

    private fun restartApplication() {
        val restartIntent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                restartIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        getSystemService(AlarmManager::class.java).set(
            AlarmManager.RTC,
            System.currentTimeMillis() + RESTART_DELAY_MILLIS,
            pendingIntent,
        )
        finishAffinity()
        Process.killProcess(Process.myPid())
    }

    private companion object {
        private const val RESTART_DELAY_MILLIS = 150L
    }
}

internal sealed interface StartupThemeState {
    data object Loading : StartupThemeState

    data class Resolved(val darkTheme: Boolean) : StartupThemeState
}

internal fun resolveStartupThemeState(prefs: UserPreferences?, systemDarkTheme: Boolean): StartupThemeState =
    prefs?.let {
        StartupThemeState.Resolved(
            darkTheme =
                when (ThemeMode.fromPreference(it.themeMode)) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> systemDarkTheme
                },
        )
    } ?: StartupThemeState.Loading

internal fun isHealthConnectPermissionDisclosureAction(action: String?): Boolean =
    action == ACTION_SHOW_HEALTH_CONNECT_PERMISSIONS_RATIONALE

internal const val ACTION_SHOW_HEALTH_CONNECT_PERMISSIONS_RATIONALE =
    "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"

internal const val ACTION_VIEW_HEALTH_PERMISSION_USAGE =
    "android.intent.action.VIEW_PERMISSION_USAGE"
