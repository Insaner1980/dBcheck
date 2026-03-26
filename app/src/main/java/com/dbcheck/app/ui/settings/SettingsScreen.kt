package com.dbcheck.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.settings.components.AudioCalibrationSection
import com.dbcheck.app.ui.settings.components.DisplayAppearanceSection
import com.dbcheck.app.ui.settings.components.NoiseNotificationsSection
import com.dbcheck.app.ui.settings.components.ProUpsellCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(modifier = Modifier.fillMaxSize()) {
        DbCheckTopAppBar(actionIcon = Icons.Outlined.Person)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Text(
                text = "SYSTEM PREFERENCES",
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = "Settings",
                style = typography.headlineLg,
                color = colors.material.onSurface,
            )

            Spacer(Modifier.height(spacing.space2))

            AudioCalibrationSection(
                sensitivityOffset = uiState.micSensitivityOffset,
                frequencyWeighting = uiState.frequencyWeighting,
                isProUser = uiState.isProUser,
                onSensitivityChange = viewModel::updateMicSensitivity,
                onWeightingChange = viewModel::updateFrequencyWeighting,
            )

            NoiseNotificationsSection(
                exposureAlertsEnabled = uiState.exposureAlertsEnabled,
                peakWarningsEnabled = uiState.peakWarningsEnabled,
                notificationThreshold = uiState.notificationThreshold,
                onExposureAlertsChange = viewModel::updateExposureAlerts,
                onPeakWarningsChange = viewModel::updatePeakWarnings,
                onThresholdChange = viewModel::updateNotificationThreshold,
            )

            DisplayAppearanceSection(
                themeMode = uiState.themeMode,
                onThemeModeChange = viewModel::updateThemeMode,
            )

            if (!uiState.isProUser) {
                ProUpsellCard(onUpgradeClick = { /* TODO: Launch billing */ })
            }

            // Footer
            Text(
                text = "dBcheck v1.0.0 · Privacy · Terms",
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = spacing.space6),
            )
        }
    }
}
