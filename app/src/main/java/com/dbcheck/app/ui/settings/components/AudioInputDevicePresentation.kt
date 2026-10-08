package com.dbcheck.app.ui.settings.components

import androidx.annotation.StringRes
import com.dbcheck.app.R
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import com.dbcheck.app.ui.settings.state.AudioInputDeviceUiState

internal data class AudioInputDevicePresentation(
    val representativeId: Int,
    val memberIds: List<Int>,
    val displayName: String,
    val type: AudioInputDeviceType,
    val selectedMemberId: Int?,
    val isVisuallySelected: Boolean,
)

internal fun audioInputDevicePresentations(
    devices: List<AudioInputDeviceUiState>,
    selectedDeviceId: Int?,
): List<AudioInputDevicePresentation> {
    val groups =
        devices
            .groupBy { device ->
                AudioInputDevicePresentationKey(
                    normalizedProductName = device.displayName.normalizedProductName(),
                    type = device.type,
                )
            }.values
            .map { members ->
                val sortedMembers = members.sortedBy(AudioInputDeviceUiState::id)
                val representative = sortedMembers.first()
                val memberIds = sortedMembers.map(AudioInputDeviceUiState::id)
                AudioInputDevicePresentation(
                    representativeId = representative.id,
                    memberIds = memberIds,
                    displayName = representative.displayName.normalizedDisplayName(),
                    type = representative.type,
                    selectedMemberId = selectedDeviceId?.takeIf { it in memberIds },
                    isVisuallySelected = false,
                )
            }

    val selectedGroup = groups.firstOrNull { it.selectedMemberId != null }
    val selectedGroupRepresentativeId =
        when {
            selectedGroup != null -> selectedGroup.representativeId

            selectedDeviceId != null -> null

            else ->
                groups
                    .filter { it.type == AudioInputDeviceType.BUILT_IN_MIC }
                    .minByOrNull(AudioInputDevicePresentation::representativeId)
                    ?.representativeId
        }

    return groups.map { group ->
        group.copy(isVisuallySelected = group.representativeId == selectedGroupRepresentativeId)
    }
}

@StringRes
internal fun AudioInputDeviceType.presentationSubtitleStringRes(): Int = when (this) {
    AudioInputDeviceType.BUILT_IN_MIC -> R.string.settings_audio_input_type_built_in
    AudioInputDeviceType.WIRED_HEADSET -> R.string.settings_audio_input_type_wired
    AudioInputDeviceType.USB -> R.string.settings_audio_input_type_usb
    AudioInputDeviceType.BLUETOOTH -> R.string.settings_audio_input_type_bluetooth
    AudioInputDeviceType.OTHER -> R.string.settings_audio_input_type_other
}

private data class AudioInputDevicePresentationKey(val normalizedProductName: String, val type: AudioInputDeviceType)

private fun String.normalizedProductName(): String = normalizedDisplayName().lowercase()

private fun String.normalizedDisplayName(): String = trim().replace(WHITESPACE_REGEX, " ")

private val WHITESPACE_REGEX = Regex("\\s+")
