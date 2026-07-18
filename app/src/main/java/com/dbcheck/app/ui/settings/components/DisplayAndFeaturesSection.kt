package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.ThemeMode
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.hearing.components.VoiceBaselineCard
import com.dbcheck.app.ui.hearing.components.VoiceBaselineCardActions
import com.dbcheck.app.ui.hearing.components.VoiceBaselineCardState
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.displayNameStringRes

data class DisplayAndFeaturesSectionState(
    val themeMode: String,
    val waveformStyle: WaveformStyle,
    val refreshRate: MeterRefreshRate,
    val lockscreenMeterEnabled: Boolean,
    val showLockscreenMeterPublicly: Boolean,
    val technicalMetadataEnabled: Boolean,
    val dosimeterCardEnabled: Boolean,
    val soundDetectionEnabled: Boolean,
    val sleepCardEnabled: Boolean,
    val voiceBaselineLevelDb: Float?,
    val voiceBaselineSampleCount: Int,
    val canCalibrateVoiceBaseline: Boolean,
    val isProUser: Boolean,
)

data class DisplayAndFeaturesSectionActions(
    val onThemeModeChange: (String) -> Unit,
    val onWaveformStyleChange: (WaveformStyle) -> Unit,
    val onRefreshRateChange: (MeterRefreshRate) -> Unit,
    val onLockscreenMeterChange: (Boolean) -> Unit,
    val onShowLockscreenMeterPubliclyChange: (Boolean) -> Unit,
    val onTechnicalMetadataChange: (Boolean) -> Unit,
    val onDosimeterCardChange: (Boolean) -> Unit,
    val onSoundDetectionChange: (Boolean) -> Unit,
    val onSleepCardChange: (Boolean) -> Unit,
    val onCalibrateVoiceBaseline: () -> Unit,
    val onUpgradeClick: () -> Unit,
)

@Composable
fun DisplayAndFeaturesSection(
    state: DisplayAndFeaturesSectionState,
    actions: DisplayAndFeaturesSectionActions,
    modifier: Modifier = Modifier,
) {
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(title = stringResource(R.string.settings_display_features_title))

        DisplayAppearanceCard(
            state = state,
            actions = actions,
        )
        Spacer(Modifier.height(spacing.space4))
        FeatureTogglesCard(
            state = state,
            actions = actions,
        )
        Spacer(Modifier.height(spacing.space4))
        VoiceBaselineCard(
            state =
                VoiceBaselineCardState(
                    levelDb = state.voiceBaselineLevelDb,
                    sampleCount = state.voiceBaselineSampleCount,
                    canCalibrate = state.canCalibrateVoiceBaseline,
                    isLocked = !state.isProUser,
                ),
            actions =
                VoiceBaselineCardActions(
                    onCalibrate = actions.onCalibrateVoiceBaseline,
                    onUpgradeClick = actions.onUpgradeClick,
                ),
        )
        Spacer(Modifier.height(spacing.space4))
        LockscreenMeterSection(
            state =
                LockscreenMeterSectionState(
                    lockscreenMeterEnabled = state.lockscreenMeterEnabled,
                    showLockscreenMeterPublicly = state.showLockscreenMeterPublicly,
                    isProUser = state.isProUser,
                ),
            actions =
                LockscreenMeterSectionActions(
                    onLockscreenMeterChange = actions.onLockscreenMeterChange,
                    onShowLockscreenMeterPubliclyChange = actions.onShowLockscreenMeterPubliclyChange,
                    onUpgradeClick = actions.onUpgradeClick,
                ),
            showTitle = false,
        )
    }
}

@Composable
private fun FeatureTogglesCard(state: DisplayAndFeaturesSectionState, actions: DisplayAndFeaturesSectionActions) {
    ProLockOverlay(
        isLocked = !state.isProUser,
        onUpgradeClick = actions.onUpgradeClick,
    ) {
        SettingsCardColumn {
            Text(
                text = stringResource(R.string.settings_feature_toggles_title),
                style = DbCheckTheme.typography.bodyLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
            FeatureToggleRow(
                title = stringResource(R.string.settings_feature_technical_metadata_title),
                subtitle = stringResource(R.string.settings_feature_technical_metadata_subtitle),
                checked = state.technicalMetadataEnabled,
                onCheckedChange = actions.onTechnicalMetadataChange,
            )
            FeatureToggleRow(
                title = stringResource(R.string.settings_feature_dosimeter_card_title),
                subtitle = stringResource(R.string.settings_feature_dosimeter_card_subtitle),
                checked = state.dosimeterCardEnabled,
                onCheckedChange = actions.onDosimeterCardChange,
            )
            FeatureToggleRow(
                title = stringResource(R.string.settings_feature_sound_detection_title),
                subtitle = stringResource(R.string.settings_feature_sound_detection_subtitle),
                checked = state.soundDetectionEnabled,
                onCheckedChange = actions.onSoundDetectionChange,
            )
            FeatureToggleRow(
                title = stringResource(R.string.settings_feature_sleep_card_title),
                subtitle = stringResource(R.string.settings_feature_sleep_card_subtitle),
                checked = state.sleepCardEnabled,
                onCheckedChange = actions.onSleepCardChange,
            )
        }
    }
}

@Composable
private fun FeatureToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingsToggleDescriptionRow(
        title = title,
        subtitle = subtitle,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun DisplayAppearanceCard(state: DisplayAndFeaturesSectionState, actions: DisplayAndFeaturesSectionActions) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    SettingsCardColumn {
        Text(
            text = stringResource(R.string.display_appearance_title),
            style = typography.bodyLg,
            color = colors.material.onSurface,
        )
        SettingsChipGroup(label = stringResource(R.string.display_dark_mode)) {
            ThemeMode.entries.forEach { mode ->
                DbCheckChip(
                    text = stringResource(mode.displayNameStringRes()),
                    selected = state.themeMode == mode.preferenceValue,
                    onClick = { actions.onThemeModeChange(mode.preferenceValue) },
                )
            }
        }

        SettingsChipGroup(label = stringResource(R.string.display_waveform_style)) {
            WaveformStyle.entries.forEach { style ->
                DbCheckChip(
                    text = stringResource(style.displayNameStringRes()),
                    selected = state.waveformStyle == style,
                    onClick = { actions.onWaveformStyleChange(style) },
                )
            }
        }

        SettingsChipGroup(
            label = stringResource(R.string.display_refresh_rate),
            helperText = stringResource(R.string.display_refresh_rate_helper),
        ) {
            MeterRefreshRate.entries.forEach { rate ->
                DbCheckChip(
                    text = stringResource(rate.displayNameStringRes()),
                    selected = state.refreshRate == rate,
                    onClick = { actions.onRefreshRateChange(rate) },
                )
            }
        }
    }
}

@Composable
private fun SettingsChipGroup(label: String, helperText: String? = null, chips: @Composable () -> Unit) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = typography.bodyLg, color = colors.material.onSurface)
        helperText?.let {
            Spacer(Modifier.height(spacing.space1))
            Text(it, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        }
        Spacer(Modifier.height(spacing.space2))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            chips()
        }
    }
}
