package com.dbcheck.app.ui.settings.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SettingsDisclosureContractTest {
    @Test
    fun sharedDisclosureUsesTokenizedInfoTargetAndOneSharedDialog() {
        val source = componentSource("CompactDisclosureInfo.kt")

        assertTrue(source.contains("fun CompactDisclosureInfo("))
        assertTrue(source.contains("maxLines = 1"))
        assertTrue(source.contains("IconButton("))
        assertTrue(source.contains("minWidth = spacing.space12"))
        assertTrue(source.contains("minHeight = spacing.space12"))
        assertTrue(source.contains("DbCheckAlertDialog("))
        assertTrue(source.contains("body = fullText"))
        assertTrue(source.contains("text = fullText"))
    }

    @Test
    fun wavDisclosureIsCompactWhenOffAndInlineWhenRawAudioSavingIsOn() {
        val source = componentSource("DataExportSection.kt")

        assertTrue(source.contains("CompactDisclosureInfo("))
        assertTrue(source.contains("fullText = stringResource(R.string.settings_wav_recording_privacy_warning)"))
        assertTrue(source.contains("compactLabel = stringResource(R.string.settings_wav_recording_privacy_compact)"))
        assertTrue(source.contains("showFullInline = enabled"))
    }

    @Test
    fun publicLockscreenDisclosureIsCompactWhenOffAndInlineWhenReadingsArePublic() {
        val source = componentSource("LockscreenMeterSection.kt")

        assertTrue(source.contains("CompactDisclosureInfo("))
        assertTrue(source.contains("fullText = stringResource(R.string.lockscreen_meter_public_warning)"))
        assertTrue(source.contains("compactLabel = stringResource(R.string.lockscreen_meter_public_privacy_compact)"))
        assertTrue(source.contains("showFullInline = showLockscreenMeterPublicly"))
    }

    @Test
    fun passiveDisclosureKeepsCompactInactiveAndFullActiveBranches() {
        val source = componentSource("NoiseNotificationsSection.kt")

        assertTrue(source.contains("CompactDisclosureInfo("))
        assertTrue(
            source.contains(
                "fullText = stringResource(R.string.noise_notifications_passive_monitoring_disclosure)",
            ),
        )
        assertTrue(
            source.contains(
                "compactLabel = stringResource(" +
                    "R.string.noise_notifications_passive_monitoring_disclosure_compact)",
            ),
        )
        assertTrue(source.contains("showFullInline = active"))
    }

    @Test
    fun passiveStartWaitsForConfirmationAndCancelDoesNotForward() {
        val source = componentSource("NoiseNotificationsSection.kt")
        val controls = source.substringAfter("private fun PassiveMonitoringControls(")

        assertTrue(controls.contains("var startConfirmationVisible by rememberSaveable"))
        assertTrue(controls.contains("{ startConfirmationVisible = true }"))
        assertTrue(controls.contains("onClick = passiveAction"))
        assertTrue(controls.contains("PassiveMonitoringStartDialog("))
        assertTrue(controls.contains("onConfirm = {"))
        assertTrue(
            controls.contains(
                "startConfirmationVisible = false\n                    onStartPassiveMonitoring()",
            ),
        )
        assertTrue(controls.contains("onDismiss = { startConfirmationVisible = false }"))
        assertFalse(controls.contains("onClick = if (active) onStopPassiveMonitoring else onStartPassiveMonitoring"))
    }

    @Test
    fun passiveConfirmationUsesFullDisclosureAndCurrentStartCallback() {
        val source = componentSource("NoiseNotificationsSection.kt")
        val dialog = source.substringAfter("private fun PassiveMonitoringStartDialog(")

        assertTrue(dialog.contains("DbCheckAlertDialog("))
        assertTrue(dialog.contains("body = stringResource(R.string.noise_notifications_passive_monitoring_disclosure)"))
        assertTrue(dialog.contains("onConfirm = onConfirm"))
        assertTrue(dialog.contains("onDismiss = onDismiss"))
    }

    @Test
    fun tertiaryButtonPreservesSuppliedLocalizedCasing() {
        val source = sharedComponentSource("DbCheckButton.kt")

        assertFalse(source.contains("text.uppercase()"))
        assertFalse(source.contains("dbCheckButtonText(style, text)"))
        assertTrue(source.contains("text = text"))
    }

    @Test
    fun scheduleKeepsMixedDayChipSemanticsAndItsOwnSlider() {
        val scheduleSource = componentSource("NoiseNotificationsSection.kt")
        val soundReferenceSource =
            projectFile("src/main/java/com/dbcheck/app/ui/meter/components/SoundReferenceCard.kt").readText()

        assertTrue(scheduleSource.contains("selected = day in schedule.activeDays"))
        assertTrue(scheduleSource.contains("contentDescription = dayContentDescription"))
        assertTrue(scheduleSource.contains("stateDescription = dayStateDescription"))
        assertTrue(scheduleSource.contains("private fun NotificationScheduleHourSlider("))
        assertTrue(scheduleSource.contains("DbCheckSlider("))
        assertTrue(soundReferenceSource.contains("private fun SoundReferenceRail("))
        assertTrue(soundReferenceSource.contains("Canvas("))
        assertFalse(soundReferenceSource.contains("DbCheckSlider("))
    }

    @Test
    fun defaultAndFinnishResourcesKeepFullDisclosuresAndNewCompactLabels() {
        val defaultNames = stringResourceNames("values")
        val finnishNames = stringResourceNames("values-fi")
        val requiredNames =
            setOf(
                "action_close",
                "settings_wav_recording_privacy_warning",
                "settings_wav_recording_privacy_compact",
                "lockscreen_meter_public_warning",
                "lockscreen_meter_public_privacy_compact",
                "noise_notifications_passive_monitoring_disclosure",
                "noise_notifications_passive_monitoring_disclosure_compact",
            )

        assertTrue(defaultNames.containsAll(requiredNames))
        assertTrue(finnishNames.containsAll(requiredNames))
    }

    private fun stringResourceNames(directory: String): Set<String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(projectFile("src/main/res/$directory/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it).attributes.getNamedItem("name").nodeValue }
            .toSet()
    }
}

private fun componentSource(fileName: String) = listOf(
    File("src/main/java/com/dbcheck/app/ui/settings/components/$fileName"),
    File("app/src/main/java/com/dbcheck/app/ui/settings/components/$fileName"),
).firstOrNull(File::isFile)
    ?.readText()
    .orEmpty()

private fun sharedComponentSource(fileName: String) =
    projectFile("src/main/java/com/dbcheck/app/ui/components/$fileName").readText()
