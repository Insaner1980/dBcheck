package com.dbcheck.app.ui.meter

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.components.WaveformBackground
import com.dbcheck.app.ui.meter.components.CircularGauge
import com.dbcheck.app.ui.meter.components.MeterControls
import com.dbcheck.app.ui.meter.components.NoiseLevelPill
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onMicPermissionResult(granted)
    }

    // Check permission on first composition
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        viewModel.onMicPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DbCheckTopAppBar(
            actionIcon = Icons.Outlined.Settings,
            onActionClick = onNavigateToSettings,
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space8))

        // Circular gauge
        CircularGauge(
            currentDb = uiState.currentDb,
            noiseLevel = uiState.noiseLevel,
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space4))

        // Noise level pill
        NoiseLevelPill(noiseLevel = uiState.noiseLevel)

        Spacer(Modifier.height(DbCheckTheme.spacing.space6))

        // Waveform visualization
        WaveformVisualization(
            data = uiState.waveformData,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space4))

        // MIN / AVG / MAX stat cards
        Row(
            modifier = Modifier
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

        // Controls: Reset, Play/Pause, Share
        MeterControls(
            isRecording = uiState.isRecording,
            onToggleRecording = {
                if (!uiState.isMicPermissionGranted) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.toggleRecording()
                }
            },
            onReset = viewModel::resetMeasurement,
            onShare = { /* TODO: Share implementation */ },
            modifier = Modifier.padding(bottom = DbCheckTheme.spacing.space6),
        )
    }
}
