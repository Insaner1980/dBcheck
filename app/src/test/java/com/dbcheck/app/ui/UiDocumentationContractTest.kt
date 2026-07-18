package com.dbcheck.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UiDocumentationContractTest {
    @Test
    fun uiSpecDocumentsFiveTopLevelDestinationsAndFullscreenSelection() {
        val spec = rootDocument("UI-SPEC.md")

        assertContainsInOrder(
            source = spec,
            markers = listOf("Meter", "Trends (`analytics`)", "Hearing", "History", "Settings"),
        )
        assertTrue(spec.contains("viisi top-level-kohdetta"))
        assertTrue(spec.contains("bottom bar"))
        assertTrue(spec.contains("navigation rail"))
        assertTrue(spec.contains("`history/detail/{sessionId}` kuuluu History-valintaan"))
        assertTrue(spec.contains("ei rootina nayta yhteista navigaatiota"))
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
    fun documentsUseMeasuredScreenshotMatrixAndReferenceCounts() {
        val documents = listOf(rootDocument("UI-SPEC.md"), rootDocument("PROJECT.md"))

        documents.forEach { document ->
            assertTrue(document.contains("56 komponenttipreviewta"))
            assertTrue(document.contains("34 light/dark full-screen -tilaa"))
            assertTrue(document.contains("5 fontScale = 1.5f -previewta"))
            assertTrue(document.contains("95 `@PreviewTest`-funktiota"))
            assertTrue(document.contains("95 baseline-PNG:ta"))
        }
    }

    private fun rootDocument(name: String): String = listOf(File(name), File("..", name))
            .firstOrNull(File::isFile)
            ?.readText()
            ?: error("Project document does not exist: $name")

    private fun assertContainsInOrder(source: String, markers: List<String>) {
        var cursor = -1
        markers.forEach { marker ->
            val next = source.indexOf(marker, startIndex = cursor + 1)
            assertTrue("Missing or out-of-order documentation marker $marker", next > cursor)
            cursor = next
        }
    }
}
