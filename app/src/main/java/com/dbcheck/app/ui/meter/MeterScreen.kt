package com.dbcheck.app.ui.meter

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.meter.components.CircularGauge
import com.dbcheck.app.ui.meter.components.MeterControls
import com.dbcheck.app.ui.meter.components.StatCard
import com.dbcheck.app.ui.meter.components.WaveformVisualization
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun MeterScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MeterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            viewModel.onMicPermissionResult(granted)
            if (!granted) {
                viewModel.onMicPermissionDenied()
            }
        }

    // Request POST_NOTIFICATIONS on Android 13+ (best-effort, app works without it)
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { /* App works fine without notification permission */ }

    LaunchedEffect(Unit) {
        requestMicPermissionIfNeeded(context, viewModel, permissionLauncher)
        requestNotificationPermissionIfNeeded(context, notificationPermissionLauncher)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DbCheckTopAppBar(
            actionIcon = Icons.Outlined.Settings,
            onActionClick = onNavigateToSettings,
        )

        if (uiState.showMicDeniedPrompt) {
            // Full-screen mic denied prompt (per spec section 11)
            MicPermissionDeniedPrompt(
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                },
                onRetry = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            )
        } else {
            MeterContent(
                uiState = uiState,
                onToggleRecording = {
                    if (!uiState.isMicPermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleRecording()
                    }
                },
                onReset = viewModel::resetMeasurement,
                onShare = { },
            )
        }
    }
}

private fun requestMicPermissionIfNeeded(
    context: android.content.Context,
    viewModel: MeterViewModel,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    val granted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    viewModel.onMicPermissionResult(granted)
    if (!granted) {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

private fun requestNotificationPermissionIfNeeded(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun MicPermissionDeniedPrompt(
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = colors.material.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(spacing.space6))
        Text(
            text = "Microphone Access Required",
            style = typography.headlineMd,
            color = colors.material.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = "dBcheck needs microphone access to measure sound levels. Please enable it in your device settings.",
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space8))
        DbCheckButton(
            text = "Open Settings",
            onClick = onOpenSettings,
            height = 48.dp,
        )
        Spacer(Modifier.height(spacing.space3))
        DbCheckButton(
            text = "Try Again",
            onClick = onRetry,
            style = DbCheckButtonStyle.Secondary,
            height = 48.dp,
        )
    }
}

@Composable
private fun ColumnScope.MeterContent(
    uiState: com.dbcheck.app.ui.meter.state.MeterUiState,
    onToggleRecording: () -> Unit,
    onReset: () -> Unit,
    onShare: () -> Unit,
) {
    Spacer(Modifier.height(DbCheckTheme.spacing.space8))

    CircularGauge(
        currentDb = uiState.currentDb,
        noiseLevel = uiState.noiseLevel,
    )

    Spacer(Modifier.height(DbCheckTheme.spacing.space6))

    WaveformVisualization(
        data = uiState.waveformData,
        modifier = Modifier.padding(horizontal = 20.dp),
    )

    Spacer(Modifier.height(DbCheckTheme.spacing.space4))

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            label = "Min",
            value = uiState.minDb,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Avg",
            value = uiState.avgDb,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Max",
            value = uiState.maxDb,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.weight(1f))

    MeterControls(
        isRecording = uiState.isRecording,
        onToggleRecording = onToggleRecording,
        onReset = onReset,
        onShare = onShare,
        modifier = Modifier.padding(bottom = DbCheckTheme.spacing.space6),
    )
}
