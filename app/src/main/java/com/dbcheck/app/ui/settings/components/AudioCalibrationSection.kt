package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.calibration.CalibrationOffsetPolicy
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.settings.state.CalibrationProfileUiState
import com.dbcheck.app.ui.settings.state.OctaveCalibrationBandUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.displayNameStringRes
import java.util.Locale

data class AudioCalibrationSectionState(
    val sensitivityOffset: Float,
    val frequencyWeighting: String,
    val isProUser: Boolean,
    val profiles: List<CalibrationProfileUiState>,
    val selectedProfileId: Long?,
    val profileErrorMessage: String?,
)

data class AudioCalibrationSectionActions(
    val onSensitivityChange: (Float) -> Unit,
    val onWeightingChange: (String) -> Unit,
    val onCreateProfile: (String) -> Unit,
    val onSelectProfile: (Long) -> Unit,
    val onRenameProfile: (Long, String) -> Unit,
    val onDeleteProfile: (Long) -> Unit,
    val onOctaveBandOffsetChange: (Long, Float, Float) -> Unit,
    val onResetOctaveBandOffsets: (Long) -> Unit,
    val onUpgradeClick: () -> Unit,
)

@Composable
fun AudioCalibrationSection(
    state: AudioCalibrationSectionState,
    actions: AudioCalibrationSectionActions,
    modifier: Modifier = Modifier,
) {
    SettingsLockedCardSection(
        title = stringResource(R.string.settings_audio_calibration_title),
        isLocked = !state.isProUser,
        onUpgradeClick = actions.onUpgradeClick,
        modifier = modifier,
    ) {
        AudioCalibrationContent(
            state = state,
            actions = actions,
        )
    }
}

@Composable
private fun AudioCalibrationContent(state: AudioCalibrationSectionState, actions: AudioCalibrationSectionActions) {
    var profileEditor by remember { mutableStateOf<CalibrationProfileUiState?>(null) }
    var isCreateDialogVisible by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<CalibrationProfileUiState?>(null) }
    val selectedProfile = state.profiles.firstOrNull { it.isSelected || state.selectedProfileId == it.id }

    Column(modifier = Modifier.fillMaxWidth()) {
        MicSensitivityControls(
            sensitivityOffset = state.sensitivityOffset,
            onSensitivityChange = actions.onSensitivityChange,
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space5))

        FrequencyWeightingControls(
            frequencyWeighting = state.frequencyWeighting,
            onWeightingChange = actions.onWeightingChange,
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space5))

        CalibrationProfileControls(
            profiles = state.profiles,
            selectedProfileId = state.selectedProfileId,
            errorMessage = state.profileErrorMessage,
            onCreateClick = { isCreateDialogVisible = true },
            onSelectProfile = actions.onSelectProfile,
            onRenameClick = { profileEditor = it },
            onDeleteClick = { deleteCandidate = it },
        )

        selectedProfile?.takeIf { it.octaveBandOffsets.isNotEmpty() }?.let { profile ->
            Spacer(Modifier.height(DbCheckTheme.spacing.space5))

            OctaveCalibrationControls(
                profile = profile,
                onOffsetChange = { centerFrequencyHz, offsetDb ->
                    actions.onOctaveBandOffsetChange(profile.id, centerFrequencyHz, offsetDb)
                },
                onReset = { actions.onResetOctaveBandOffsets(profile.id) },
            )
        }
    }

    if (isCreateDialogVisible) {
        CalibrationProfileEditorDialog(
            title = stringResource(R.string.settings_calibration_profile_create_title),
            initialName = "",
            onConfirm = { name ->
                actions.onCreateProfile(name)
                isCreateDialogVisible = false
            },
            onDismiss = { isCreateDialogVisible = false },
        )
    }

    profileEditor?.let { profile ->
        CalibrationProfileEditorDialog(
            title = stringResource(R.string.settings_calibration_profile_rename_title),
            initialName = profile.name,
            onConfirm = { name ->
                actions.onRenameProfile(profile.id, name)
                profileEditor = null
            },
            onDismiss = { profileEditor = null },
        )
    }

    deleteCandidate?.let { profile ->
        DeleteCalibrationProfileDialog(
            profile = profile,
            onConfirm = {
                actions.onDeleteProfile(profile.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null },
        )
    }
}

@Composable
private fun MicSensitivityControls(sensitivityOffset: Float, onSensitivityChange: (Float) -> Unit) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val sensitivityRange =
        UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET_MIN..UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET_MAX

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_audio_mic_sensitivity),
                style = typography.bodyLg,
                color = colors.material.onSurface,
            )
            Text(
                text = formatOffset(sensitivityOffset),
                style = typography.dataMd,
                color = colors.material.onSurface,
            )
        }
        DbCheckSlider(
            value = sensitivityOffset,
            onValueChange = onSensitivityChange,
            valueRange = sensitivityRange,
        )
        Text(
            stringResource(R.string.settings_audio_sensitivity_helper),
            style = typography.bodyMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun FrequencyWeightingControls(frequencyWeighting: String, onWeightingChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.settings_audio_frequency_weighting),
            style = DbCheckTheme.typography.bodyLg,
            color = DbCheckTheme.colorScheme.material.onSurface,
        )
        Spacer(Modifier.height(DbCheckTheme.spacing.space2))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
        ) {
            WeightingType.entries.forEach { weight ->
                DbCheckChip(
                    text = stringResource(weight.displayNameStringRes()),
                    selected = frequencyWeighting == weight.name,
                    onClick = { onWeightingChange(weight.name) },
                )
            }
        }
    }
}

@Composable
private fun CalibrationProfileControls(
    profiles: List<CalibrationProfileUiState>,
    selectedProfileId: Long?,
    errorMessage: String?,
    onCreateClick: () -> Unit,
    onSelectProfile: (Long) -> Unit,
    onRenameClick: (CalibrationProfileUiState) -> Unit,
    onDeleteClick: (CalibrationProfileUiState) -> Unit,
) {
    val spacing = DbCheckTheme.spacing
    val colors = DbCheckTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        SettingsDescriptionRow(
            title = stringResource(R.string.settings_calibration_profiles_title),
            subtitle = stringResource(R.string.settings_calibration_profiles_subtitle),
        ) {
            DbCheckButton(
                text = stringResource(R.string.action_add),
                onClick = onCreateClick,
                style = DbCheckButtonStyle.Tertiary,
                height = spacing.space12,
            )
        }

        if (profiles.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_calibration_profiles_empty),
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                profiles.forEachIndexed { index, profile ->
                    if (index > 0) {
                        HorizontalDivider(color = colors.material.outlineVariant)
                    }
                    CalibrationProfileRow(
                        profile = profile,
                        selected = profile.isSelected || selectedProfileId == profile.id,
                        onSelect = { onSelectProfile(profile.id) },
                        onRename = { onRenameClick(profile) },
                        onDelete = { onDeleteClick(profile) },
                    )
                }
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.error,
            )
        }
    }
}

@Composable
private fun OctaveCalibrationControls(
    profile: CalibrationProfileUiState,
    onOffsetChange: (Float, Float) -> Unit,
    onReset: () -> Unit,
) {
    val spacing = DbCheckTheme.spacing
    val colors = DbCheckTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        SettingsDescriptionRow(
            title = stringResource(R.string.settings_calibration_octave_title),
            subtitle = stringResource(R.string.settings_calibration_octave_subtitle),
        ) {
            IconButton(
                onClick = onReset,
                enabled = profile.octaveBandOffsets.hasCustomOffsets(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.settings_calibration_octave_reset),
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            profile.octaveBandOffsets.forEachIndexed { index, band ->
                if (index > 0) {
                    HorizontalDivider(color = colors.material.outlineVariant)
                }
                OctaveCalibrationBandSlider(
                    band = band,
                    onOffsetChange = { offsetDb -> onOffsetChange(band.centerFrequencyHz, offsetDb) },
                )
            }
        }
    }
}

@Composable
private fun OctaveCalibrationBandSlider(band: OctaveCalibrationBandUiState, onOffsetChange: (Float) -> Unit) {
    val spacing = DbCheckTheme.spacing
    val colors = DbCheckTheme.colorScheme
    val bandLabel = formatCenterFrequency(band.centerFrequencyHz)
    val offsetLabel = formatOffset(band.offsetDb)
    val sliderDescription =
        stringResource(
            R.string.settings_calibration_octave_band_content_description,
            bandLabel,
            offsetLabel,
        )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bandLabel,
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.onSurface,
            )
            Text(
                text = offsetLabel,
                style = DbCheckTheme.typography.dataMd,
                color = colors.material.onSurface,
            )
        }
        DbCheckSlider(
            value = band.offsetDb,
            onValueChange = onOffsetChange,
            modifier =
                Modifier.semantics {
                    contentDescription = sliderDescription
                },
            valueRange = CalibrationOffsetPolicy.MIN_OFFSET_DB..CalibrationOffsetPolicy.MAX_OFFSET_DB,
        )
    }
}

@Composable
private fun CalibrationProfileRow(
    profile: CalibrationProfileUiState,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = DbCheckTheme.spacing
    val colors = DbCheckTheme.colorScheme

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).padding(vertical = spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(
                text = profile.name,
                style = DbCheckTheme.typography.bodyMd.copy(fontWeight = FontWeight.SemiBold),
                color = colors.material.onSurface,
            )
            Text(
                text = profileSubtitle(profile),
                style = DbCheckTheme.typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription =
                    stringResource(R.string.settings_calibration_profile_edit_content_description, profile.name),
            )
        }
        IconButton(
            onClick = onDelete,
            enabled = profile.canDelete,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription =
                    stringResource(R.string.settings_calibration_profile_delete_content_description, profile.name),
            )
        }
    }
}

@Composable
private fun CalibrationProfileEditorDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.settings_calibration_profile_name_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                    )
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DbCheckTheme.colorScheme.material.primary.copy(alpha = 0.3f),
                        unfocusedBorderColor = DbCheckTheme.colorScheme.ghostBorder,
                    ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DeleteCalibrationProfileDialog(
    profile: CalibrationProfileUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_calibration_profile_delete_title)) },
        text = { Text(stringResource(R.string.settings_calibration_profile_delete_message, profile.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = DbCheckTheme.colorScheme.material.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun profileSubtitle(profile: CalibrationProfileUiState): String = if (profile.isDefault) {
        stringResource(R.string.settings_calibration_profile_offset_default, formatOffset(profile.micSensitivityOffset))
    } else {
        stringResource(R.string.settings_calibration_profile_offset, formatOffset(profile.micSensitivityOffset))
    }

private fun formatOffset(offset: Float): String =
    "${if (offset >= 0) "+" else ""}${String.format(Locale.getDefault(), "%.1f", offset)} dB"

private fun formatCenterFrequency(centerFrequencyHz: Float): String = if (centerFrequencyHz >= 1_000f) {
        "${String.format(Locale.getDefault(), "%.1f", centerFrequencyHz / 1_000f)} kHz"
    } else {
        "${String.format(Locale.getDefault(), "%.0f", centerFrequencyHz)} Hz"
    }

private fun List<OctaveCalibrationBandUiState>.hasCustomOffsets(): Boolean =
    any { it.offsetDb != CalibrationOffsetPolicy.DEFAULT_OFFSET_DB }
