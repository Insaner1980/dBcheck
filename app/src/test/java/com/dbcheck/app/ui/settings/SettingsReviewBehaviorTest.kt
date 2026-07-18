package com.dbcheck.app.ui.settings

import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.ui.navigation.settingsLegacyRedirectPlan
import com.dbcheck.app.ui.settings.state.CalibrationProfileUiState
import com.dbcheck.app.ui.settings.state.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsReviewBehaviorTest {
    @Test
    fun purchaseFeedbackIsAbsentWithoutVisibleMessage() {
        assertNull(purchaseFeedbackFor(SettingsUiState()))
    }

    @Test
    fun completedPendingUnavailableAndFailedFeedbackStayVisibleUntilPresentedClear() {
        listOf(
            SettingsUiState(purchaseMessage = "Completed") to SettingsPurchaseFeedbackTone.SUCCESS,
            SettingsUiState(purchaseMessage = "Pending") to SettingsPurchaseFeedbackTone.SUCCESS,
            SettingsUiState(purchaseErrorMessage = "Unavailable") to SettingsPurchaseFeedbackTone.ERROR,
            SettingsUiState(purchaseErrorMessage = "Failed") to SettingsPurchaseFeedbackTone.ERROR,
        ).forEach { (state, expectedTone) ->
            val feedback = requireNotNull(purchaseFeedbackFor(state))

            assertEquals(expectedTone, feedback.tone)
            assertTrue(feedback.shouldAutoClearAfterVisible)
        }
    }

    @Test
    fun freeOctavePresentationIsLockedAndUsesRichSliderPreview() {
        val presentation = octaveCalibrationPresentation(SettingsUiState(isProUser = false))

        assertTrue(presentation.isLocked)
        assertTrue(presentation.profile.octaveBandOffsets.isNotEmpty())
    }

    @Test
    fun proOctavePresentationUsesSelectedSharedProfileUnlocked() {
        val selectedProfile =
            CalibrationProfileUiState(
                id = 42L,
                name = "Field mic",
                micSensitivityOffset = 1.5f,
                isDefault = false,
                isSelected = true,
                canDelete = true,
            )
        val presentation =
            octaveCalibrationPresentation(
                SettingsUiState(
                    isProUser = true,
                    selectedCalibrationProfileId = selectedProfile.id,
                    calibrationProfiles = listOf(selectedProfile),
                ),
            )

        assertFalse(presentation.isLocked)
        assertSame(selectedProfile, presentation.profile)
    }

    @Test
    fun legacyFalseRedirectsOnlyToHome() {
        val plan = settingsLegacyRedirectPlan(showPro = false)

        assertEquals(listOf(Screen.Settings.HOME_ROUTE), plan.routes)
        assertEquals(Screen.Settings.HOME_ROUTE, plan.backTargetRoute)
    }

    @Test
    fun legacyTrueBuildsHomeThenProAboutSoBackReturnsHome() {
        val plan = settingsLegacyRedirectPlan(showPro = true)

        assertEquals(
            listOf(Screen.Settings.HOME_ROUTE, Screen.Settings.PRO_ABOUT_ROUTE),
            plan.routes,
        )
        assertEquals(Screen.Settings.HOME_ROUTE, plan.backTargetRoute)
    }
}
