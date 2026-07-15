package com.dbcheck.app.ui.ambient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.domain.ambient.AmbientSoundPolicy
import com.dbcheck.app.domain.ambient.AmbientSoundPreset
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckChipDensity
import com.dbcheck.app.ui.components.DbCheckSetupHeader
import com.dbcheck.app.ui.components.DbCheckSetupScaffold
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun AmbientSoundPlaybackRoute(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AmbientSoundPlaybackViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            viewModel.play(notificationPermissionGranted = granted)
        }
    val onPlay = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.play(notificationPermissionGranted = true)
        }
    }

    AmbientSoundPlaybackScreen(
        state = state,
        onBack = onBack,
        onNavigateToUpgrade = onNavigateToUpgrade,
        onPresetChange = viewModel::updatePreset,
        onVolumeChange = viewModel::updateVolume,
        onTimerChange = viewModel::updateTimerMinutes,
        onPlay = onPlay,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
internal fun AmbientSoundPlaybackScreen(
    state: AmbientSoundPlaybackUiState,
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onPresetChange: (AmbientSoundPreset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTimerChange: (Int) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DbCheckSetupScaffold(
        onBack = onBack,
        modifier = modifier,
        header = {
            DbCheckSetupHeader(
                phase = stringResource(R.string.ambient_sound_phase),
                title = state.title,
                description = state.description,
            )
        },
        cta = {
            ProLockOverlay(
                isLocked = state.isLocked,
                onUpgradeClick = onNavigateToUpgrade,
            ) {
                AmbientSoundPlaybackActions(
                    onPlay = onPlay,
                    onStop = onStop,
                )
            }
        },
    ) {
        ProLockOverlay(
            isLocked = state.isLocked,
            onUpgradeClick = onNavigateToUpgrade,
        ) {
            AmbientSoundPlaybackContent(
                state = state,
                onPresetChange = onPresetChange,
                onVolumeChange = onVolumeChange,
                onTimerChange = onTimerChange,
            )
        }
    }
}

@Composable
internal fun AmbientSoundPlaybackContent(
    state: AmbientSoundPlaybackUiState,
    onPresetChange: (AmbientSoundPreset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTimerChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier) {
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space4), modifier = Modifier.fillMaxWidth()) {
                PresetSelector(selectedPreset = state.preset, onPresetChange = onPresetChange)
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    Text(
                        text = stringResource(R.string.ambient_sound_volume),
                        style = typography.labelMd,
                        color = colors.material.onSurfaceVariant,
                    )
                    DbCheckSlider(
                        value = state.volume,
                        onValueChange = onVolumeChange,
                        valueRange = AmbientSoundPolicy.MIN_VOLUME..AmbientSoundPolicy.MAX_VOLUME,
                        steps = VOLUME_STEPS,
                        valueLabel = stringResource(R.string.ambient_sound_volume_value, (state.volume * 100).toInt()),
                    )
                }
                TimerSelector(selectedTimer = state.timerMinutes, onTimerChange = onTimerChange)
                state.errorMessage?.let {
                    Text(text = it, style = typography.labelSm, color = colors.material.error)
                }
            }
        }
    }
}

@Composable
private fun AmbientSoundPlaybackActions(onPlay: () -> Unit, onStop: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DbCheckButton(
            text = stringResource(R.string.ambient_sound_play),
            onClick = onPlay,
            modifier = Modifier.weight(1f),
            height = DbCheckTheme.spacing.space12,
        )
        DbCheckButton(
            text = stringResource(R.string.ambient_sound_stop),
            onClick = onStop,
            modifier = Modifier.weight(1f),
            style = DbCheckButtonStyle.Secondary,
            height = DbCheckTheme.spacing.space12,
        )
    }
}

@Composable
private fun PresetSelector(selectedPreset: AmbientSoundPreset, onPresetChange: (AmbientSoundPreset) -> Unit) {
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.ambient_sound_preset),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2), modifier = Modifier.fillMaxWidth()) {
            AmbientSoundPreset.entries.forEach { preset ->
                DbCheckChip(
                    onClick = { onPresetChange(preset) },
                    text = preset.label(),
                    selected = selectedPreset == preset,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.GraphicEq, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f),
                    density = DbCheckChipDensity.Compact,
                )
            }
        }
    }
}

@Composable
private fun TimerSelector(selectedTimer: Int, onTimerChange: (Int) -> Unit) {
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = stringResource(R.string.ambient_sound_timer),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2), modifier = Modifier.fillMaxWidth()) {
            AmbientSoundPolicy.TIMER_OPTIONS_MINUTES.forEach { minutes ->
                DbCheckChip(
                    onClick = { onTimerChange(minutes) },
                    text = timerLabel(minutes),
                    selected = selectedTimer == minutes,
                    modifier = Modifier.weight(1f),
                    density = DbCheckChipDensity.Compact,
                )
            }
        }
    }
}

@Composable
private fun AmbientSoundPreset.label(): String = when (this) {
        AmbientSoundPreset.WHITE_NOISE -> stringResource(R.string.ambient_sound_preset_white_noise)
        AmbientSoundPreset.PINK_NOISE -> stringResource(R.string.ambient_sound_preset_pink_noise)
        AmbientSoundPreset.BROWN_NOISE -> stringResource(R.string.ambient_sound_preset_brown_noise)
        AmbientSoundPreset.FAN -> stringResource(R.string.ambient_sound_preset_fan)
    }

@Composable
private fun timerLabel(minutes: Int): String = if (minutes == 0) {
        stringResource(R.string.ambient_sound_timer_none)
    } else {
        stringResource(R.string.ambient_sound_timer_minutes, minutes)
    }

private const val VOLUME_STEPS = 18
