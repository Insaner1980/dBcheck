package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.domain.calibration.CalibrationOffsetPolicy
import com.dbcheck.app.ui.components.DbCheckAlertDialog
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.settings.state.AudioInputDeviceUiState
import com.dbcheck.app.ui.settings.state.CalibrationProfileUiState
import com.dbcheck.app.ui.settings.state.OctaveCalibrationBandUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import com.dbcheck.app.util.displayNameStringRes

data class AudioCalibrationSectionState(
    val sensitivityOffset: Float,
    val frequencyWeighting: String,
    val responseTime: ResponseTime,
    val isProUser: Boolean,
    val profiles: List<CalibrationProfileUiState>,
    val selectedProfileId: Long?,
    val profileErrorMessage: String?,
    val audioInputDevices: List<AudioInputDeviceUiState>,
    val selectedAudioInputDeviceId: Int?,
)

data class AudioCalibrationSectionActions(
    val onSensitivityChange: (Float) -> Unit,
    val onWeightingChange: (String) -> Unit,
    val onResponseTimeChange: (ResponseTime) -> Unit,
    val onSelectAudioInputDevice: (Int) -> Unit,
    val onCreateProfile: (String) -> Unit,
    val onSelectProfile: (Long) -> Unit,
    val onRenameProfile: (Long, String) -> Unit,
    val onDeleteProfile: (Long) -> Unit,
    val onOpenOctaveCalibration: () -> Unit,
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

        ResponseTimeControls(
            responseTime = state.responseTime,
            onResponseTimeChange = actions.onResponseTimeChange,
        )

        Spacer(Modifier.height(DbCheckTheme.spacing.space5))

        AudioInputDeviceControls(
            devices = state.audioInputDevices,
            selectedDeviceId = state.selectedAudioInputDeviceId,
            onSelectDevice = actions.onSelectAudioInputDevice,
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

        Spacer(Modifier.height(DbCheckTheme.spacing.space5))
        OctaveCalibrationNavigationRow(onClick = actions.onOpenOctaveCalibration)
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
private fun OctaveCalibrationNavigationRow(onClick: () -> Unit) {
    SettingsDescriptionRow(
        title = stringResource(R.string.settings_calibration_octave_title),
        subtitle = stringResource(R.string.settings_calibration_octave_subtitle),
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
            )
        },
    )
}

@Composable
fun OctaveCalibrationSection(
    profile: CalibrationProfileUiState?,
    onOffsetChange: (Long, Float, Float) -> Unit,
    onReset: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (profile == null) {
        Text(
            text = stringResource(R.string.settings_calibration_profiles_empty),
            style = DbCheckTheme.typography.bodyMd,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }

    SettingsCardColumn(modifier = modifier) {
        OctaveCalibrationControls(
            profile = profile,
            enabled = enabled,
            onOffsetChange = { centerFrequencyHz, offsetDb ->
                onOffsetChange(profile.id, centerFrequencyHz, offsetDb)
            },
            onReset = { onReset(profile.id) },
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
private fun ResponseTimeControls(responseTime: ResponseTime, onResponseTimeChange: (ResponseTime) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.settings_audio_response_time),
            style = DbCheckTheme.typography.bodyLg,
            color = DbCheckTheme.colorScheme.material.onSurface,
        )
        Spacer(Modifier.height(DbCheckTheme.spacing.space2))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
        ) {
            ResponseTime.entries.forEach { response ->
                DbCheckChip(
                    text = stringResource(response.displayNameStringRes()),
                    selected = responseTime == response,
                    onClick = { onResponseTimeChange(response) },
                )
            }
        }
    }
}

@Composable
private fun AudioInputDeviceControls(
    devices: List<AudioInputDeviceUiState>,
    selectedDeviceId: Int?,
    onSelectDevice: (Int) -> Unit,
) {
    val colors = DbCheckTheme.colorScheme

    CalibrationControlGroup(
        title = stringResource(R.string.settings_audio_input_title),
        subtitle = stringResource(R.string.settings_audio_input_subtitle),
        trailingContent = {},
    ) {
        if (devices.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_audio_input_empty),
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                devices.forEachIndexed { index, device ->
                    if (index > 0) {
                        HorizontalDivider(color = colors.material.outlineVariant)
                    }
                    AudioInputDeviceRow(
                        device = device,
                        selected = device.id == selectedDeviceId,
                        onSelect = { onSelectDevice(device.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioInputDeviceRow(device: AudioInputDeviceUiState, selected: Boolean, onSelect: () -> Unit) {
    SelectableCalibrationRow(
        selected = selected,
        onSelect = onSelect,
        title = device.displayName,
        subtitle = stringResource(device.type.labelStringRes()),
    )
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
    val colors = DbCheckTheme.colorScheme

    CalibrationControlGroup(
        title = stringResource(R.string.settings_calibration_profiles_title),
        subtitle = stringResource(R.string.settings_calibration_profiles_subtitle),
        trailingContent = {
            DbCheckButton(
                text = stringResource(R.string.action_add),
                onClick = onCreateClick,
                style = DbCheckButtonStyle.Tertiary,
                height = DbCheckTheme.spacing.space12,
            )
        },
    ) {
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
    enabled: Boolean = true,
    onReset: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme

    CalibrationControlGroup(
        title = stringResource(R.string.settings_calibration_octave_title),
        subtitle = stringResource(R.string.settings_calibration_octave_subtitle),
        trailingContent = {
            IconButton(
                onClick = onReset,
                enabled = enabled && profile.octaveBandOffsets.hasCustomOffsets(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.settings_calibration_octave_reset),
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            profile.octaveBandOffsets.forEachIndexed { index, band ->
                if (index > 0) {
                    HorizontalDivider(color = colors.material.outlineVariant)
                }
                OctaveCalibrationBandSlider(
                    band = band,
                    enabled = enabled,
                    onOffsetChange = { offsetDb -> onOffsetChange(band.centerFrequencyHz, offsetDb) },
                )
            }
        }
    }
}

@Composable
private fun CalibrationControlGroup(
    title: String,
    subtitle: String,
    trailingContent: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
    ) {
        SettingsDescriptionRow(
            title = title,
            subtitle = subtitle,
            trailingContent = trailingContent,
        )
        content()
    }
}

@Composable
private fun OctaveCalibrationBandSlider(
    band: OctaveCalibrationBandUiState,
    enabled: Boolean,
    onOffsetChange: (Float) -> Unit,
) {
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
            enabled = enabled,
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
    SelectableCalibrationRow(
        selected = selected,
        onSelect = onSelect,
        title = profile.name,
        subtitle = profileSubtitle(profile),
    ) {
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
private fun SelectableCalibrationRow(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    subtitle: String,
    trailingContent: @Composable RowScope.() -> Unit = {},
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
                text = title,
                style = DbCheckTheme.typography.bodyMd.copy(fontWeight = FontWeight.SemiBold),
                color = colors.material.onSurface,
            )
            Text(
                text = subtitle,
                style = DbCheckTheme.typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
        }
        trailingContent()
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

    DbCheckAlertDialog(
        title = title,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(trimmedName) },
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.action_cancel),
        confirmEnabled = trimmedName.isNotBlank(),
    ) {
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
    }
}

@Composable
private fun DeleteCalibrationProfileDialog(
    profile: CalibrationProfileUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DbCheckAlertDialog(
        title = stringResource(R.string.settings_calibration_profile_delete_title),
        body = stringResource(R.string.settings_calibration_profile_delete_message, profile.name),
        confirmText = stringResource(R.string.action_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.action_cancel),
        icon = Icons.Outlined.Delete,
    )
}

@Composable
private fun profileSubtitle(profile: CalibrationProfileUiState): String = if (profile.isDefault) {
        stringResource(R.string.settings_calibration_profile_offset_default, formatOffset(profile.micSensitivityOffset))
    } else {
        stringResource(R.string.settings_calibration_profile_offset, formatOffset(profile.micSensitivityOffset))
    }

@Composable
private fun formatOffset(offset: Float): String = stringResource(R.string.settings_calibration_offset_db, offset)

@Composable
private fun formatCenterFrequency(centerFrequencyHz: Float): String = if (centerFrequencyHz >= 1_000f) {
        stringResource(R.string.settings_calibration_frequency_khz, centerFrequencyHz / 1_000f)
    } else {
        stringResource(R.string.settings_calibration_frequency_hz, centerFrequencyHz)
    }

private fun AudioInputDeviceType.labelStringRes(): Int = when (this) {
        AudioInputDeviceType.BUILT_IN_MIC -> R.string.settings_audio_input_type_built_in
        AudioInputDeviceType.WIRED_HEADSET -> R.string.settings_audio_input_type_wired
        AudioInputDeviceType.USB -> R.string.settings_audio_input_type_usb
        AudioInputDeviceType.BLUETOOTH -> R.string.settings_audio_input_type_bluetooth
        AudioInputDeviceType.OTHER -> R.string.settings_audio_input_type_other
    }

private fun List<OctaveCalibrationBandUiState>.hasCustomOffsets(): Boolean =
    any { it.offsetDb != CalibrationOffsetPolicy.DEFAULT_OFFSET_DB }
