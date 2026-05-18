package com.dbcheck.app

import android.os.Bundle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.theme.DbCheckTheme

class HealthConnectPermissionDisclosureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DbCheckTheme(darkTheme = isSystemInDarkTheme()) {
                HealthConnectPermissionDisclosureScreen()
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
internal fun HealthConnectPermissionDisclosureScreen() {
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
