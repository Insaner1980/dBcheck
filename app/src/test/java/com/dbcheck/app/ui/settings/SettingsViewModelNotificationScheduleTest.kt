package com.dbcheck.app.ui.settings

import com.dbcheck.app.MainDispatcherRule
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.domain.noise.NoiseNotificationSchedule
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelNotificationScheduleTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun notificationSchedulePreferenceIsMappedToSettingsState() = runTest {
            val schedule =
                NoiseNotificationSchedule(
                    activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    startMinuteOfDay = 22 * MINUTES_PER_HOUR,
                    endMinuteOfDay = 6 * MINUTES_PER_HOUR,
                )
            val harness =
                SettingsViewModelTestHarness(
                    initialPreferences = UserPreferences(notificationSchedule = schedule),
                )

            val viewModel = harness.createViewModel()
            advanceUntilIdle()

            assertEquals(schedule, viewModel.uiState.value.notificationSchedule)
        }

    @Test
    fun updatingNotificationScheduleDelegatesToPreferencesRepository() = runTest {
            val harness = SettingsViewModelTestHarness()
            val viewModel = harness.createViewModel()
            val schedule =
                NoiseNotificationSchedule(
                    activeDays = setOf(DayOfWeek.FRIDAY),
                    startMinuteOfDay = 9 * MINUTES_PER_HOUR,
                    endMinuteOfDay = 17 * MINUTES_PER_HOUR,
                )

            viewModel.updateNoiseNotification(NoiseNotificationUpdate.NotificationSchedule(schedule))
            advanceUntilIdle()

            coVerify { harness.preferencesRepository.updateNotificationSchedule(schedule) }
        }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
