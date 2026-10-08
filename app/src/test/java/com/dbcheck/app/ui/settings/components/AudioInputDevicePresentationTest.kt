package com.dbcheck.app.ui.settings.components

import com.dbcheck.app.R
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import com.dbcheck.app.ui.settings.state.AudioInputDeviceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioInputDevicePresentationTest {
    @Test
    fun duplicateIdsWithNormalizedProductNameAndSameTypeBecomeOneRow() {
        val presentations =
            audioInputDevicePresentations(
                devices =
                    listOf(
                        device(id = 9, displayName = "  Studio   Mic ", type = AudioInputDeviceType.USB),
                        device(id = 3, displayName = "studio mic", type = AudioInputDeviceType.USB),
                    ),
                selectedDeviceId = null,
            )

        assertEquals(1, presentations.size)
        assertEquals(listOf(3, 9), presentations.single().memberIds)
        assertEquals(3, presentations.single().representativeId)
        assertEquals("studio mic", presentations.single().displayName)
    }

    @Test
    fun sameProductNameWithDifferentTypesRemainsSeparate() {
        val presentations =
            audioInputDevicePresentations(
                devices =
                    listOf(
                        device(id = 1, displayName = "Headset", type = AudioInputDeviceType.WIRED_HEADSET),
                        device(id = 2, displayName = " headset ", type = AudioInputDeviceType.BLUETOOTH),
                    ),
                selectedDeviceId = null,
            )

        assertEquals(2, presentations.size)
        assertEquals(
            listOf(AudioInputDeviceType.WIRED_HEADSET, AudioInputDeviceType.BLUETOOTH),
            presentations.map(AudioInputDevicePresentation::type),
        )
    }

    @Test
    fun selectedMemberIdRemainsSelectedWithoutBeingNormalizedToRepresentative() {
        val presentation =
            audioInputDevicePresentations(
                devices =
                    listOf(
                        device(id = 3, displayName = "USB-C mic", type = AudioInputDeviceType.USB),
                        device(id = 9, displayName = "USB-C mic", type = AudioInputDeviceType.USB),
                    ),
                selectedDeviceId = 9,
            ).single()

        assertTrue(presentation.isVisuallySelected)
        assertEquals(9, presentation.selectedMemberId)
        assertEquals(3, presentation.representativeId)
    }

    @Test
    fun nullSelectionUsesBuiltInAsVisualFallbackWithoutSelectedMember() {
        val presentations =
            audioInputDevicePresentations(
                devices =
                    listOf(
                        device(id = 4, displayName = "USB mic", type = AudioInputDeviceType.USB),
                        device(id = 8, displayName = "Built-in microphone", type = AudioInputDeviceType.BUILT_IN_MIC),
                    ),
                selectedDeviceId = null,
            )

        val builtIn = presentations.single { it.type == AudioInputDeviceType.BUILT_IN_MIC }
        assertTrue(builtIn.isVisuallySelected)
        assertNull(builtIn.selectedMemberId)
        assertFalse(presentations.single { it.type == AudioInputDeviceType.USB }.isVisuallySelected)
    }

    @Test
    fun nullSelectionWithoutBuiltInDoesNotSelectAnotherDevice() {
        val presentations =
            audioInputDevicePresentations(
                devices =
                    listOf(
                        device(id = 4, displayName = "USB mic", type = AudioInputDeviceType.USB),
                        device(id = 5, displayName = "Headset", type = AudioInputDeviceType.BLUETOOTH),
                    ),
                selectedDeviceId = null,
            )

        assertTrue(presentations.none(AudioInputDevicePresentation::isVisuallySelected))
        assertTrue(presentations.all { it.selectedMemberId == null })
    }

    @Test
    fun explicitRowSelectionUsesDeterministicRepresentativeId() {
        val presentation =
            audioInputDevicePresentations(
                devices =
                    listOf(
                        device(id = 42, displayName = "USB mic", type = AudioInputDeviceType.USB),
                        device(id = 7, displayName = "USB mic", type = AudioInputDeviceType.USB),
                        device(id = 19, displayName = "USB mic", type = AudioInputDeviceType.USB),
                    ),
                selectedDeviceId = null,
            ).single()

        assertEquals(7, presentation.representativeId)
    }

    @Test
    fun eachDeviceTypeUsesItsOwnPresentationSubtitle() {
        assertEquals(
            R.string.settings_audio_input_type_built_in,
            AudioInputDeviceType.BUILT_IN_MIC.presentationSubtitleStringRes(),
        )
        assertEquals(
            R.string.settings_audio_input_type_wired,
            AudioInputDeviceType.WIRED_HEADSET.presentationSubtitleStringRes(),
        )
        assertEquals(R.string.settings_audio_input_type_usb, AudioInputDeviceType.USB.presentationSubtitleStringRes())
        assertEquals(
            R.string.settings_audio_input_type_bluetooth,
            AudioInputDeviceType.BLUETOOTH.presentationSubtitleStringRes(),
        )
        assertEquals(
            R.string.settings_audio_input_type_other,
            AudioInputDeviceType.OTHER.presentationSubtitleStringRes(),
        )
    }

    private fun device(id: Int, displayName: String, type: AudioInputDeviceType): AudioInputDeviceUiState =
        AudioInputDeviceUiState(
            id = id,
            displayName = displayName,
            type = type,
            isExternal = type != AudioInputDeviceType.BUILT_IN_MIC,
        )
}
