package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.service.PassiveMonitoringManager
import com.dbcheck.app.service.PassiveMonitoringServiceController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelPassiveMonitoringTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun passiveMonitoringActiveStateIsMappedToSettingsState() = runTest {
        val passiveMonitoringActive = MutableStateFlow(false)
        val harness =
            SettingsViewModelTestHarness(
                passiveMonitoringManager =
                    mockk<PassiveMonitoringManager> {
                        every { isMonitoring } returns passiveMonitoringActive
                    },
            )

        val viewModel = harness.createViewModel()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.passiveMonitoringActive)

        passiveMonitoringActive.value = true
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.passiveMonitoringActive)
    }

    @Test
    fun startPassiveMonitoringDelegatesToForegroundServiceController() = runTest {
        val controller =
            mockk<PassiveMonitoringServiceController> {
                coEvery { startPassiveMonitoring() } returns true
            }
        val harness = SettingsViewModelTestHarness(passiveMonitoringServiceController = controller)
        val viewModel = harness.createViewModel()

        viewModel.startPassiveMonitoring()
        advanceUntilIdle()

        coVerify(exactly = 1) { controller.startPassiveMonitoring() }
    }

    @Test
    fun stopPassiveMonitoringDelegatesToForegroundServiceController() = runTest {
        val controller =
            mockk<PassiveMonitoringServiceController> {
                coEvery { stopPassiveMonitoring() } returns true
            }
        val harness = SettingsViewModelTestHarness(passiveMonitoringServiceController = controller)
        val viewModel = harness.createViewModel()

        viewModel.stopPassiveMonitoring()
        advanceUntilIdle()

        coVerify(exactly = 1) { controller.stopPassiveMonitoring() }
    }
}
