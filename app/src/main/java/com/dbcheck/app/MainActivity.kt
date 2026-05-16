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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@Composable
@Suppress("FunctionNaming")
private fun HealthConnectPermissionDisclosureScreen() {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = spacing.space8),
        verticalArrangement = Arrangement.spacedBy(spacing.space5),
    ) {
        Text(
            text = "Health Connect permissions",
            style = typography.headlineMd,
            color = colors.material.onSurface,
        )
        Text(
            text =
                "dBcheck requests Health Connect access only when you enable health features in Settings. " +
                    "You can grant or revoke these permissions in Health Connect at any time.",
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
        )
        HealthConnectDisclosureItem(
            title = "Noise exposure sessions",
            body =
                "When Health Connect sync is enabled, dBcheck writes completed measurement sessions " +
                    "as exercise entries. Health Connect does not provide a native noise exposure type, " +
                    "so the entry notes include the session LAeq, maximum, peak, and frequency weighting.",
        )
        HealthConnectDisclosureItem(
            title = "Heart rate overlay",
            body =
                "When the Pro heart rate overlay is enabled, dBcheck reads heart rate samples only for " +
                    "the selected measurement window so analytics can compare pulse response with noise exposure.",
        )
        HealthConnectDisclosureItem(
            title = "Hearing tests",
            body =
                "dBcheck does not write hearing test results to Health Connect because Health Connect " +
                    "does not currently provide a supported audiometry record type.",
        )
        Spacer(Modifier.height(spacing.space2))
        Text(
            text = "Health Connect stores and controls the shared health data. dBcheck does not upload this data.",
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun HealthConnectDisclosureItem(title: String, body: String) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = typography.bodyLg,
            color = colors.material.onSurface,
        )
        Text(
            text = body,
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

internal sealed interface StartupThemeState {
    data object Loading : StartupThemeState

    data class Resolved(
        val darkTheme: Boolean,
    ) : StartupThemeState
}

internal fun resolveStartupThemeState(
    prefs: UserPreferences?,
    systemDarkTheme: Boolean,
): StartupThemeState =
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
    action == ACTION_SHOW_HEALTH_CONNECT_PERMISSIONS_RATIONALE ||
        action == ACTION_VIEW_HEALTH_PERMISSION_USAGE

internal const val ACTION_SHOW_HEALTH_CONNECT_PERMISSIONS_RATIONALE =
    "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"

internal const val ACTION_VIEW_HEALTH_PERMISSION_USAGE =
    "android.intent.action.VIEW_PERMISSION_USAGE"
