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
import androidx.compose.ui.res.stringResource
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
            text = stringResource(R.string.health_connect_permission_dialog_title),
            style = typography.headlineMd,
            color = colors.material.onSurface,
        )
        Text(
            text = stringResource(R.string.health_connect_disclosure_intro),
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
        )
        HealthConnectDisclosureItem(
            title = stringResource(R.string.health_connect_disclosure_noise_title),
            body = stringResource(R.string.health_connect_disclosure_noise_body),
        )
        HealthConnectDisclosureItem(
            title = stringResource(R.string.health_connect_disclosure_heart_title),
            body = stringResource(R.string.health_connect_disclosure_heart_body),
        )
        HealthConnectDisclosureItem(
            title = stringResource(R.string.health_connect_disclosure_hearing_title),
            body = stringResource(R.string.health_connect_disclosure_hearing_body),
        )
        Spacer(Modifier.height(spacing.space2))
        Text(
            text = stringResource(R.string.health_connect_disclosure_footer),
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
