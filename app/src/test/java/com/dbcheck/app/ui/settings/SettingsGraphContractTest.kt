package com.dbcheck.app.ui.settings

import com.dbcheck.app.projectFile
import com.dbcheck.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGraphContractTest {
    @Test
    fun settingsGraphUsesExactRoutesAndCompatibilityTargets() {
        assertEquals("settings", Screen.Settings.route)
        assertEquals("settings/home", Screen.Settings.HOME_ROUTE)
        assertEquals("settings/calibration", Screen.Settings.CALIBRATION_ROUTE)
        assertEquals("settings/calibration/octave", Screen.Settings.OCTAVE_CALIBRATION_ROUTE)
        assertEquals("settings/notifications", Screen.Settings.NOTIFICATIONS_ROUTE)
        assertEquals("settings/data_privacy", Screen.Settings.DATA_PRIVACY_ROUTE)
        assertEquals("settings/display", Screen.Settings.DISPLAY_ROUTE)
        assertEquals("settings/pro_about", Screen.Settings.PRO_ABOUT_ROUTE)
        assertEquals(Screen.Settings.HOME_ROUTE, Screen.Settings.createRoute(showPro = false))
        assertEquals(Screen.Settings.PRO_ABOUT_ROUTE, Screen.Settings.createRoute(showPro = true))
        assertEquals("settings?showPro={showPro}", Screen.Settings.ROUTE_WITH_ARGS)
    }

    @Test
    fun navHostRegistersNestedGraphAndOneParentScopedViewModelHelper() {
        val source = navigationSource()

        assertTrue(source.contains("navigation("))
        assertTrue(source.contains("startDestination = Screen.Settings.HOME_ROUTE"))
        assertTrue(source.contains("route = Screen.Settings.route"))
        assertTrue(source.contains("remember(backStackEntry)"))
        assertTrue(source.contains("navController.getBackStackEntry(Screen.Settings.route)"))
        assertTrue(source.contains("hiltViewModel(parentEntry)"))
        assertEquals(7, Regex("settingsGraphViewModel\\(navController, backStackEntry\\)").findAll(source).count())
        assertFalse(source.contains("SettingsScreen("))
    }

    @Test
    fun hubHasExactlyFiveTokenizedMaterialRowsInRequiredOrder() {
        val source = pagesSource()
        val hub = source.substringAfter("fun SettingsHomePage(").substringBefore("fun SettingsCalibrationPage(")
        val orderedRoutes =
            listOf(
                "Screen.Settings.CALIBRATION_ROUTE",
                "Screen.Settings.NOTIFICATIONS_ROUTE",
                "Screen.Settings.DATA_PRIVACY_ROUTE",
                "Screen.Settings.DISPLAY_ROUTE",
                "Screen.Settings.PRO_ABOUT_ROUTE",
            )
        var cursor = -1
        orderedRoutes.forEach { route ->
            val next = hub.indexOf(route)
            assertTrue("Missing or out-of-order hub route $route", next > cursor)
            cursor = next
        }

        assertEquals(5, Regex("SettingsHubRow\\(").findAll(hub).count())
        assertTrue(hub.contains("Icons.Outlined.Tune"))
        assertTrue(hub.contains("Icons.Outlined.Notifications"))
        assertTrue(hub.contains("Icons.Outlined.Security"))
        assertTrue(hub.contains("Icons.Outlined.Palette"))
        assertTrue(hub.contains("Icons.Outlined.Info"))
        assertTrue(source.contains("heightIn(min = spacing.space12)"))
        assertFalse(hub.contains("emoji"))
    }

    @Test
    fun pagesHaveRequiredExclusiveSectionOwnershipAndBackContract() {
        val source = pagesSource()
        val pageToContent =
            settingsPageDelegationContracts.map { contract -> contract.pageName to contract.contentName }
        val delegationViolations = settingsPageDelegationViolations(source)
        assertEquals(
            delegationViolations.joinToString(separator = "\n"),
            emptyList<String>(),
            delegationViolations,
        )
        val contentBlocks =
            pageToContent.mapIndexed { index, (_, contentName) ->
                val nextContent = pageToContent.getOrNull(index + 1)?.second ?: "SettingsHubRow"
                contentName to source.pageBlock(contentName, nextContent)
            }.toMap()

        val exclusiveOwners =
            mapOf(
                "AudioCalibrationSection(" to "SettingsCalibrationContent",
                "OctaveCalibrationSection(" to "SettingsOctaveCalibrationContent",
                "NoiseNotificationsSection(" to "SettingsNotificationsContent",
                "HealthSyncSection(" to "SettingsDataPrivacyContent",
                "DataExportSection(" to "SettingsDataPrivacyContent",
                "LockscreenMeterSection(" to "SettingsDataPrivacyContent",
                "DisplayAndFeaturesSection(" to "SettingsDisplayContent",
                "ProUpsellCard(" to "SettingsProAboutContent",
            )
        exclusiveOwners.forEach { (section, owner) ->
            assertTrue("$owner must own $section", contentBlocks.getValue(owner).contains(section))
            assertEquals("$section must have one owner", 1, Regex(Regex.escape(section)).findAll(source).count())
        }

        assertTrue(contentBlocks.getValue("SettingsCalibrationContent").contains("responseTime = uiState.responseTime"))
        assertTrue(contentBlocks.getValue("SettingsProAboutContent").contains("SettingsFooter("))
        assertFalse(contentBlocks.getValue("SettingsDisplayContent").contains("VoiceBaseline"))
        contentBlocks.forEach { (contentName, content) ->
            assertTrue("$contentName must preserve back navigation", content.contains("onBack = onBack"))
        }
        assertTrue(source.contains("DbCheckTopAppBar(title = title, onBackClick = onBack)"))
    }

    @Test
    fun pageDelegationContractRejectsMissingBackAndCallbackWiring() {
        val source = pagesSource()
        val missingBack = source.replaceFirst("onBack = onBack,", "onBack = {},")
        val missingCallback = source.replaceFirst("onOpenOctaveCalibration = onOpenOctaveCalibration,", "")

        assertTrue(settingsPageDelegationViolations(missingBack).any { it.contains("onBack = onBack") })
        assertTrue(
            settingsPageDelegationViolations(missingCallback)
                .any { it.contains("onOpenOctaveCalibration = onOpenOctaveCalibration") },
        )
    }

    @Test
    fun settingsViewModelNoLongerOwnsVoiceBaselineButKeepsAudioSessionGuards() {
        val viewModel = settingsViewModelSource()
        val state = settingsStateSource()
        val display = componentSource("DisplayAndFeaturesSection.kt")

        listOf(viewModel, state, display).forEach { source ->
            assertFalse(source.contains("VoiceBaseline"))
            assertFalse(source.contains("voiceBaseline"))
            assertFalse(source.contains("canCalibrateVoiceBaseline"))
        }
        assertFalse(viewModel.contains("audioSessionManager.isRecording.collect"))
        assertTrue(viewModel.contains("private val audioSessionManager: AudioSessionManager"))
        assertTrue(viewModel.contains("ensureBackupAllowed(audioSessionManager"))
        assertTrue(viewModel.contains("ensureHistoryClearAllowed(audioSessionManager"))
    }

    @Test
    fun everyPurchaseLaunchingPagePresentsSharedFeedbackBeforeClearingIt() {
        val source = pagesSource()
        val purchasePages =
            listOf(
                source.pageBlock("SettingsCalibrationPage", "SettingsOctaveCalibrationPage"),
                source.pageBlock("SettingsOctaveCalibrationPage", "SettingsNotificationsPage"),
                source.pageBlock("SettingsNotificationsPage", "SettingsDataPrivacyPage"),
                source.pageBlock("SettingsDataPrivacyPage", "SettingsDisplayPage"),
                source.pageBlock("SettingsDisplayPage", "SettingsProAboutPage"),
                source.substringAfter("fun SettingsProAboutPage(").substringBefore("private fun SettingsHubRow"),
            )

        purchasePages.forEach { page ->
            assertTrue(page.contains("SettingsPurchaseFeedback("))
        }
        val feedback = source.substringAfter("private fun SettingsPurchaseFeedback(").substringBefore("private fun")
        assertTrue(feedback.contains("InlineStatusRow("))
        assertTrue(feedback.contains("TimedMessageEffect("))
        assertTrue(feedback.contains("viewModel::clearPurchaseMessages"))
        assertFalse(source.contains("private fun SettingsMessageEffects("))
    }

    @Test
    fun homeDoesNotCollectSettingsStateAndSuppressionIsNarrowlyScoped() {
        val source = pagesSource()
        val home = source.substringAfter("fun SettingsHomePage(").substringBefore("fun SettingsCalibrationPage(")

        assertFalse(home.contains("collectAsStateWithLifecycle"))
        assertFalse(source.startsWith("@file:Suppress(\"ViewModelForwarding\")"))
    }
}

private fun navigationSource() = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

private fun pagesSource() = projectFile("src/main/java/com/dbcheck/app/ui/settings/SettingsPages.kt").readText()

private fun settingsViewModelSource() =
    projectFile("src/main/java/com/dbcheck/app/ui/settings/SettingsViewModel.kt").readText()

private fun settingsStateSource() =
    projectFile("src/main/java/com/dbcheck/app/ui/settings/state/SettingsUiState.kt").readText()

private fun componentSource(fileName: String) =
    projectFile("src/main/java/com/dbcheck/app/ui/settings/components/$fileName").readText()

private fun String.pageBlock(name: String, nextName: String): String = substringAfter("fun $name(")
    .substringBefore("fun $nextName(")

private data class SettingsPageDelegationContract(
    val pageName: String,
    val contentName: String,
    val requiredWiring: List<String>,
)

private val settingsPageDelegationContracts =
    listOf(
        SettingsPageDelegationContract(
            pageName = "SettingsCalibrationPage",
            contentName = "SettingsCalibrationContent",
            requiredWiring =
                listOf(
                    "viewModel::updateMicSensitivity",
                    "viewModel::updateFrequencyWeighting",
                    "viewModel::updateResponseTime",
                    "viewModel::selectAudioInputDevice",
                    "viewModel::createCalibrationProfile",
                    "viewModel::selectCalibrationProfile",
                    "viewModel::renameCalibrationProfile",
                    "viewModel::deleteCalibrationProfile",
                    "onOpenOctaveCalibration = onOpenOctaveCalibration",
                    "onUpgradeClick = onStartProPurchase",
                ),
        ),
        SettingsPageDelegationContract(
            pageName = "SettingsOctaveCalibrationPage",
            contentName = "SettingsOctaveCalibrationContent",
            requiredWiring =
                listOf(
                    "viewModel::updateOctaveBandOffset",
                    "viewModel::resetOctaveBandOffsets",
                    "onUpgradeClick = onStartProPurchase",
                ),
        ),
        SettingsPageDelegationContract(
            pageName = "SettingsNotificationsPage",
            contentName = "SettingsNotificationsContent",
            requiredWiring =
                listOf(
                    "noiseNotificationActions(",
                    "viewModel.startPassiveMonitoring()",
                    "viewModel.onPassiveMonitoringPermissionDenied()",
                    "handleStartPassiveMonitoring(",
                    "micPermissionLauncher = micPermissionLauncher",
                    "notificationPermissionLauncher = notificationPermissionLauncher",
                    "onOpenMicrophoneSettings = context::openAppPermissionSettings",
                    "onStartProPurchase = onStartProPurchase",
                ),
        ),
        SettingsPageDelegationContract(
            pageName = "SettingsDataPrivacyPage",
            contentName = "SettingsDataPrivacyContent",
            requiredWiring =
                listOf(
                    "healthSyncActions(viewModel, context, onStartProPurchase)",
                    "dataExportActions(",
                    "onRestartAfterRestore = onRestartAfterRestore",
                    "locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)",
                    "onOpenLocationSettings = context::openAppPermissionSettings",
                    "onLockscreenMeterChange = viewModel::updateLockscreenMeter",
                    "onShowLockscreenMeterPubliclyChange = viewModel::updateShowLockscreenMeterPublicly",
                    "onUpgradeClick = onStartProPurchase",
                ),
        ),
        SettingsPageDelegationContract(
            pageName = "SettingsDisplayPage",
            contentName = "SettingsDisplayContent",
            requiredWiring =
                listOf(
                    "viewModel::updateThemeMode",
                    "DisplayPreferenceUpdate.WaveformStyleChange",
                    "DisplayPreferenceUpdate.RefreshRateChange",
                    "FeatureToggleUpdate.TechnicalMetadata",
                    "FeatureToggleUpdate.DosimeterCard",
                    "FeatureToggleUpdate.SoundDetection",
                    "FeatureToggleUpdate.SleepCard",
                    "onUpgradeClick = onStartProPurchase",
                ),
        ),
        SettingsPageDelegationContract(
            pageName = "SettingsProAboutPage",
            contentName = "SettingsProAboutContent",
            requiredWiring =
                listOf(
                    "rememberProPurchaseAction(viewModel)",
                    "onUpgradeClick = onStartProPurchase",
                    "onDebugForceFreeChange = viewModel::updateDebugForceFree",
                ),
        ),
    )

private fun settingsPageDelegationViolations(source: String): List<String> =
    settingsPageDelegationContracts.flatMapIndexed { index, contract ->
        if (!source.contains("fun ${contract.pageName}(")) {
            return@flatMapIndexed listOf("missing ${contract.pageName}")
        }
        val nextPage = settingsPageDelegationContracts.getOrNull(index + 1)?.pageName ?: "SettingsCalibrationContent"
        val page = source.pageBlock(contract.pageName, nextPage)
        (listOf("${contract.contentName}(", "onBack = onBack") + contract.requiredWiring)
            .filterNot(page::contains)
            .map { requirement -> "${contract.pageName} missing $requirement" }
    }
