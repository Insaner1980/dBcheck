package com.dbcheck.app.ui.meter

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeterViewModelSleepTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val harness = MeterViewModelTestHarness()

    @Test
    fun sleepSetupCtaUsesEffectiveProSleepCardPreference() = runTest {
        val viewModel = createViewModel()

        harness.preferencesFlow.value = UserPreferences(isProUser = false, sleepCardEnabled = true)
        runCurrent()

        assertFalse(viewModel.uiState.value.sleepCardEnabled)

        harness.preferencesFlow.value = UserPreferences(isProUser = true, sleepCardEnabled = true)
        runCurrent()

        assertTrue(viewModel.uiState.value.sleepCardEnabled)
    }

    @Test
    fun resetKeepsSleepSetupCtaPreference() = runTest {
        harness.preferencesFlow.value = UserPreferences(isProUser = true, sleepCardEnabled = true)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.resetMeasurement()

        assertTrue(viewModel.uiState.value.sleepCardEnabled)
    }

    private fun createViewModel(): MeterViewModel = harness.createViewModel()
}
