package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.audio.AudioInputDevice
import com.dbcheck.app.domain.audio.AudioInputDeviceType
import com.dbcheck.app.service.AudioInputDeviceDiscoveryPort
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelAudioInputDeviceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness = SettingsViewModelTestHarness(UserPreferences(isProUser = true))
    private val discoveryPort = FakeAudioInputDeviceDiscoveryPort()

    @Test
    fun initLoadsAudioInputDevicesIntoSettingsUiState() = runTest {
        discoveryPort.devices =
            listOf(
                AudioInputDevice(
                    id = USB_DEVICE_ID,
                    displayName = "USB-C microphone",
                    type = AudioInputDeviceType.USB,
                    isExternal = true,
                    sampleRatesHz = listOf(44_100, 48_000),
                    channelCounts = listOf(1, 2),
                ),
            )

        val viewModel = harness.createViewModel(audioInputDeviceDiscoveryPort = discoveryPort)
        advanceUntilIdle()

        assertEquals(
            listOf("USB-C microphone"),
            viewModel.uiState.value.audioInputDevices.map { it.displayName },
        )
        assertEquals(AudioInputDeviceType.USB, viewModel.uiState.value.audioInputDevices.single().type)
        assertTrue(viewModel.uiState.value.audioInputDevices.single().isExternal)
        assertEquals(listOf(44_100, 48_000), viewModel.uiState.value.audioInputDevices.single().sampleRatesHz)
        assertEquals(listOf(1, 2), viewModel.uiState.value.audioInputDevices.single().channelCounts)
    }

    @Test
    fun proUserCanSelectDiscoveredAudioInputDevice() = runTest {
        discoveryPort.devices =
            listOf(
                AudioInputDevice(
                    id = USB_DEVICE_ID,
                    displayName = "USB-C microphone",
                    type = AudioInputDeviceType.USB,
                    isExternal = true,
                ),
            )
        val viewModel = harness.createViewModel(audioInputDeviceDiscoveryPort = discoveryPort)
        advanceUntilIdle()

        viewModel.selectAudioInputDevice(USB_DEVICE_ID)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            harness.preferencesRepository.updateSelectedAudioInputDeviceId(USB_DEVICE_ID)
        }
    }

    @Test
    fun freeUserCannotSelectAudioInputDevice() = runTest {
        val freeHarness = SettingsViewModelTestHarness(UserPreferences(isProUser = false))
        discoveryPort.devices =
            listOf(
                AudioInputDevice(
                    id = USB_DEVICE_ID,
                    displayName = "USB-C microphone",
                    type = AudioInputDeviceType.USB,
                    isExternal = true,
                ),
            )
        val viewModel = freeHarness.createViewModel(audioInputDeviceDiscoveryPort = discoveryPort)
        advanceUntilIdle()

        viewModel.selectAudioInputDevice(USB_DEVICE_ID)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            freeHarness.preferencesRepository.updateSelectedAudioInputDeviceId(any())
        }
    }

    @Test
    fun missingSelectedExternalDeviceFallsBackToBuiltInSelectionInUiState() = runTest {
        val fallbackHarness =
            SettingsViewModelTestHarness(
                UserPreferences(
                    isProUser = true,
                    selectedAudioInputDeviceId = USB_DEVICE_ID,
                ),
            )
        discoveryPort.devices =
            listOf(
                AudioInputDevice(
                    id = BUILT_IN_DEVICE_ID,
                    displayName = "Phone microphone",
                    type = AudioInputDeviceType.BUILT_IN_MIC,
                    isExternal = false,
                ),
            )

        val viewModel = fallbackHarness.createViewModel(audioInputDeviceDiscoveryPort = discoveryPort)
        advanceUntilIdle()

        assertEquals(BUILT_IN_DEVICE_ID, viewModel.uiState.value.selectedAudioInputDeviceId)
        coVerify(exactly = 0) {
            fallbackHarness.preferencesRepository.updateSelectedAudioInputDeviceId(BUILT_IN_DEVICE_ID)
        }
    }

    private class FakeAudioInputDeviceDiscoveryPort : AudioInputDeviceDiscoveryPort {
        var devices: List<AudioInputDevice> = emptyList()

        override suspend fun listInputDevices(): List<AudioInputDevice> = devices
    }

    private companion object {
        const val BUILT_IN_DEVICE_ID = 1
        const val USB_DEVICE_ID = 12
    }
}
