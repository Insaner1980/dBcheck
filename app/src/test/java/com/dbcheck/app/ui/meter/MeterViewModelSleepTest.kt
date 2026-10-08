package com.dbcheck.app.ui.meter

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeterViewModelSleepTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness = MeterViewModelTestHarness()

    @Test
    fun sleepCardRequiresBothPreferenceAndProEntitlement() = runTest {
        harness.preferencesFlow.value = UserPreferences(isProUser = true, sleepCardEnabled = true)
        val viewModel = harness.createViewModel()
        runCurrent()

        assertEquals(true, viewModel.uiState.value.sleepCardEnabled)

        harness.preferencesFlow.value = UserPreferences(isProUser = true, sleepCardEnabled = false)
        runCurrent()
        assertEquals(false, viewModel.uiState.value.sleepCardEnabled)

        harness.preferencesFlow.value = UserPreferences(isProUser = false, sleepCardEnabled = true)
        runCurrent()
        assertEquals(false, viewModel.uiState.value.sleepCardEnabled)
    }

    @Test
    fun resetKeepsEffectiveSleepCardAvailability() = runTest {
        harness.preferencesFlow.value = UserPreferences(isProUser = true, sleepCardEnabled = true)
        val viewModel = harness.createViewModel()
        runCurrent()

        viewModel.resetMeasurement()

        assertEquals(true, viewModel.uiState.value.sleepCardEnabled)
    }
}
