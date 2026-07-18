package com.dbcheck.app.ui

import com.dbcheck.app.projectFile
import com.dbcheck.app.ui.navigation.BottomNavDestination
import com.dbcheck.app.ui.navigation.Screen
import com.dbcheck.app.ui.navigation.selectedTopLevelRouteFor
import com.dbcheck.app.ui.navigation.settingsLegacyRedirectPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UiDocumentationContractTest {
    @Test
    fun navigationDocsMatchLiveFiveDestinationAndSessionDetailShellContract() {
        val spec = rootDocument("UI-SPEC.md")
        val project = rootDocument("PROJECT.md")
        val navSource = projectFile("src/main/java/com/dbcheck/app/ui/navigation/DbCheckNavHost.kt").readText()

        assertEquals(
            listOf("meter", "analytics", "hearing", "history", "settings"),
            BottomNavDestination.entries.map { it.screen.route },
        )
        assertEquals(Screen.History.route, selectedTopLevelRouteFor("history/detail/42"))
        assertTrue(navSource.contains("val showNavigation = selectedTopLevelRoute != null"))
        assertEquals(emptyList<String>(), navigationDocumentationViolations(spec, project))
    }

    @Test
    fun navigationDocumentationContractRejectsOldListAndHiddenDetailMutations() {
        val spec = rootDocument("UI-SPEC.md")
        val project = rootDocument("PROJECT.md")
        val oldList =
            spec.replace(
                TOP_LEVEL_DESTINATIONS,
                "Meter, Analytics, History ja Settings",
            )
        val hiddenDetail =
            project.replace(
                SESSION_DETAIL_NAVIGATION,
                "`history/detail/{sessionId}` kuuluu History-valintaan mutta piilottaa yhteisen navigaation.",
            )

        assertTrue(navigationDocumentationViolations(oldList, project).any { it.contains("five destinations") })
        assertTrue(navigationDocumentationViolations(spec, hiddenDetail).any { it.contains("Session Detail") })
    }

    @Test
    fun documentsPinSettingsGraphAndSharedViewModelContract() {
        val documents = listOf(rootDocument("UI-SPEC.md"), rootDocument("PROJECT.md"))
        val routes =
            listOf(
                "`settings/home`",
                "`settings/calibration`",
                "`settings/calibration/octave`",
                "`settings/notifications`",
                "`settings/data_privacy`",
                "`settings/display`",
                "`settings/pro_about`",
            )

        documents.forEach { document ->
            routes.forEach { route -> assertTrue("Missing documented route $route", document.contains(route)) }
            assertTrue(document.contains("graph-scoped `SettingsViewModel`"))
            assertTrue(document.contains("reselect-to-home"))
            assertTrue(document.contains("`settings?showPro={showPro}`"))
            assertTrue(document.contains("yhteensopivuusredirect"))
        }
    }

    @Test
    fun architectureDocsPinCurrentUpgradeRouteAndHearingOwnership() {
        assertEquals(Screen.Settings.PRO_ABOUT_ROUTE, Screen.Settings.createRoute(showPro = true))
        assertEquals(
            listOf(Screen.Settings.HOME_ROUTE, Screen.Settings.PRO_ABOUT_ROUTE),
            settingsLegacyRedirectPlan(showPro = true).routes,
        )

        val agents = rootDocument("AGENTS.md")
        val memory = rootDocument("memory/MEMORY.md")
        listOf(agents, memory).forEach { document ->
            assertTrue(document.contains("Screen.Settings.createRoute(true)"))
            assertTrue(document.contains("settingsLegacyRedirectPlan(showPro = true)"))
            assertTrue(document.contains("settings/pro_about"))
            assertFalse(document.contains("settings?showPro=true"))
            assertFalse(document.contains("Analytics Overview"))
            assertFalse(document.contains("Meterin ja Analytics"))
            assertFalse(document.contains("Meter and Analytics"))
        }
        assertTrue(agents.contains("Hearing-hubi näyttää `HearingRecoveryCard`in"))
        assertTrue(agents.contains("Hearing-hubi näyttää `TinnitusPitchCard`in"))
        assertTrue(agents.contains("Hearing-hubi näyttää Pro-gatetun `AmbientSoundCard`in"))
        assertTrue(memory.contains("Hearing hub renders `HearingRecoveryCard`"))
        assertTrue(memory.contains("Hearing hub shows `TinnitusPitchCard`"))
        assertTrue(memory.contains("Hearing hub shows `AmbientSoundCard`"))
    }

    @Test
    fun screenshotDocumentationCountsComeFromSourcesAndRecursiveReferences() {
        val counts = measuredScreenshotCounts()
        val documents = listOf(rootDocument("UI-SPEC.md"), rootDocument("PROJECT.md"))

        assertEquals(56, counts.componentPreviews)
        assertEquals(39, counts.fullScreenPreviews)
        assertEquals(5, counts.largeFontPreviews)
        assertEquals(95, counts.referencePngs)
        documents.forEach { document ->
            assertEquals(emptyList<String>(), screenshotDocumentationViolations(document, counts))
        }
    }

    @Test
    fun screenshotDocumentationContractRejectsStaleCountMutation() {
        val counts = measuredScreenshotCounts()
        val spec = rootDocument("UI-SPEC.md")
        val stale = spec.replace("${counts.componentPreviews} komponenttipreviewta", "54 komponenttipreviewta")

        assertTrue(screenshotDocumentationViolations(stale, counts).any { it.contains("component previews") })
    }

    @Test
    fun disclosureDocsMatchLiveInfoIconButtonContractAndRejectLabelMutation() {
        val source =
            projectFile("src/main/java/com/dbcheck/app/ui/settings/components/CompactDisclosureInfo.kt").readText()
        val spec = rootDocument("UI-SPEC.md")
        val project = rootDocument("PROJECT.md")

        assertTrue(source.contains("IconButton("))
        assertTrue(source.contains("onClick = { dialogVisible = true }"))
        assertEquals(emptyList<String>(), disclosureDocumentationViolations(spec, project))

        val stale = spec.replace("erillinen info-IconButton avaa dialogin", "kompakti label avaa dialogin")
        assertTrue(disclosureDocumentationViolations(stale, project).isNotEmpty())
    }

    private fun navigationDocumentationViolations(spec: String, project: String): List<String> = buildList {
        val specNavigation = spec.substringAfter("## 1. UI-teknologia ja paapolut").substringBefore("## 2.")
        val projectNavigation = project.substringAfter("## Navigaatio").substringBefore("---")
        listOf(specNavigation, projectNavigation).forEach { section ->
            if (!section.normalized().contains(TOP_LEVEL_DESTINATIONS)) add("Missing exact five destinations")
            if (!section.normalized().contains(SESSION_DETAIL_NAVIGATION)) add("Wrong Session Detail navigation")
        }
        if (listOf(spec, project).any { it.contains("Meter, Analytics, History ja Settings") }) {
            add("Stale four-destination list")
        }
    }

    private fun measuredScreenshotCounts(): ScreenshotCounts {
        val componentSource = projectFile("src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt")
        val fullScreenSource = projectFile("src/screenshotTest/kotlin/com/dbcheck/app/FullScreenScreenshotTests.kt")
        val referenceRoot = projectDirectory("src/screenshotTestDebug/reference")
        val componentPreviews = previewCount(componentSource)
        val fullScreenPreviews = previewCount(fullScreenSource)
        return ScreenshotCounts(
            componentPreviews = componentPreviews,
            fullScreenPreviews = fullScreenPreviews,
            largeFontPreviews = Regex("fontScale = 1\\.5f").findAll(fullScreenSource.readText()).count(),
            referencePngs = referenceRoot.walkTopDown().count { it.isFile && it.extension.equals("png", true) },
        )
    }

    private fun screenshotDocumentationViolations(document: String, counts: ScreenshotCounts): List<String> =
        buildList {
            val lightDarkPreviews = counts.fullScreenPreviews - counts.largeFontPreviews
            val totalPreviews = counts.componentPreviews + counts.fullScreenPreviews
            if (!document.contains("${counts.componentPreviews} komponenttipreviewta")) {
                add("Wrong component previews")
            }
            if (!document.contains("$lightDarkPreviews light/dark full-screen -tilaa")) add("Wrong matrix previews")
            if (!document.contains("${counts.largeFontPreviews} fontScale = 1.5f -previewta")) {
                add("Wrong large-font previews")
            }
            if (!document.contains("$totalPreviews `@PreviewTest`-funktiota")) add("Wrong total previews")
            if (!document.contains("${counts.referencePngs} baseline-PNG:ta")) add("Wrong reference PNGs")
            if (!document.lowercase().contains("rekursiivisesti tiedostojarjestelmasta")) {
                add("Missing recursive source count")
            }
        }

    private fun disclosureDocumentationViolations(spec: String, project: String): List<String> = buildList {
        if (Regex("erillinen info-IconButton avaa dialogin").findAll(spec).count() != DISCLOSURE_SURFACE_COUNT) {
            add("UI spec must document each info IconButton")
        }
        if (!project.contains("erillinen info-IconButton avaa dialogin")) {
            add("Project must document the shared info IconButton")
        }
        if (listOf(spec, project).any { it.contains("label avaa dialogin") }) add("Label must not own dialog click")
    }

    private fun rootDocument(name: String): String = listOf(File(name), File("..", name))
        .firstOrNull(File::isFile)
        ?.readText()
        ?: error("Project document does not exist: $name")

    private fun projectDirectory(path: String): File = listOf(File(path), File("app", path))
        .firstOrNull(File::isDirectory)
        ?: error("Project directory does not exist: $path")

    private fun previewCount(file: File): Int = Regex("(?m)^\\s*@PreviewTest\\b").findAll(file.readText()).count()

    private fun String.normalized(): String = replace(Regex("\\s+"), " ").trim()

    private data class ScreenshotCounts(
        val componentPreviews: Int,
        val fullScreenPreviews: Int,
        val largeFontPreviews: Int,
        val referencePngs: Int,
    )

    private companion object {
        const val TOP_LEVEL_DESTINATIONS = "Meter, Trends (`analytics`), Hearing, History ja Settings"
        const val SESSION_DETAIL_NAVIGATION =
            "`history/detail/{sessionId}` kuuluu History-valintaan ja nayttaa yhteisen bottom barin tai navigation railin."
        const val DISCLOSURE_SURFACE_COUNT = 3
    }
}
