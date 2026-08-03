package com.dbcheck.app.ui.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DbCheckInteractionContractTest {
    @Test
    fun appBarHasExactlyTopLevelAndPushedModels() {
        val source =
            source("ui/components/DbCheckTopAppBar.kt") +
                source("ui/components/DbCheckTopAppBarModel.kt")

        assertEquals(1, Regex("data class TopLevel\\(").findAll(source).count())
        assertEquals(1, Regex("data class Pushed\\(").findAll(source).count())
        assertTrue(source.contains("sealed interface DbCheckTopAppBarModel"))
        assertTrue(source.contains("painterResource(R.drawable.ic_dbcheck_mark)"))
        assertTrue(source.contains("Icons.AutoMirrored.Outlined.ArrowBack"))
        assertTrue(source.contains("maxLines = 1"))
        assertTrue(source.contains("overflow = TextOverflow.Ellipsis"))
    }

    @Test
    fun setupScaffoldOwnsOneInlineRouteTitle() {
        val scaffold = source("ui/components/DbCheckSetupScaffold.kt")

        assertTrue(scaffold.contains("title: String"))
        assertTrue(scaffold.contains("DbCheckTopAppBarModel.Pushed(title = title, onBackClick = onBack)"))
        assertFalse(scaffold.contains("title: String, description: String"))
        assertFalse(scaffold.contains("typography.headlineLg"))
    }

    @Test
    fun sessionDetailUsesSharedPushedHeaderAndPreservesMetadataAction() {
        val sessionDetail = source("ui/history/detail/SessionDetailScreen.kt")

        assertTrue(
            sessionDetail.contains(
                "DbCheckTopAppBarModel.Pushed(title = title, onBackClick = actions.onBack)",
            ),
        )
        assertTrue(sessionDetail.contains("onActionClick = actions.onEditMetadata"))
        assertTrue(sessionDetail.contains("Icons.Outlined.Lock else Icons.Outlined.Edit"))
        assertFalse(sessionDetail.contains("private fun SessionDetailTopBar("))
        assertFalse(sessionDetail.contains("Icons.AutoMirrored.Outlined.ArrowBack"))
        assertFalse(sessionDetail.contains(".padding(horizontal = 8.dp, vertical = 12.dp)"))
    }

    @Test
    fun sliderRequiresPresentationLabelsAndSuppressesTicksAndEndStop() {
        val slider = source("ui/components/DbCheckSlider.kt")

        assertTrue(slider.contains("valueLabel: String"))
        assertTrue(slider.contains("minLabel: String"))
        assertTrue(slider.contains("maxLabel: String"))
        assertFalse(slider.contains("valueLabel: String?"))
        assertTrue(slider.contains("drawStopIndicator = null"))
        assertTrue(slider.contains("drawTick = { _, _ -> }"))
        assertTrue(slider.contains("thumbSize = DpSize(spacing.sliderThumbSize, spacing.sliderThumbSize)"))
        assertTrue(slider.contains("stateDescription = \"\$valueLabel, \$minLabel – \$maxLabel\""))
    }

    @Test
    fun allSixSliderCallGroupsProvideTheSharedLabels() {
        val callers =
            listOf(
                source("ui/tinnitus/TinnitusPitchMatcherScreen.kt"),
                source("ui/ambient/AmbientSoundPlaybackScreen.kt"),
                source("ui/settings/components/AudioCalibrationSection.kt"),
                source("ui/settings/components/NoiseNotificationsSection.kt"),
            ).joinToString("\n")
        val calls = composeCalls(callers, "DbCheckSlider")

        assertEquals(6, calls.size)
        calls.forEach { call ->
            assertTrue(call.contains("valueLabel ="))
            assertTrue(call.contains("minLabel ="))
            assertTrue(call.contains("maxLabel ="))
        }
    }

    @Test
    fun chipsKeepFullCopyAndAmbientSelectorsWrapNaturally() {
        val chip = source("ui/components/DbCheckChip.kt")
        val ambient = source("ui/ambient/AmbientSoundPlaybackScreen.kt")
        val selectors = ambient.substringAfter("private fun PresetSelector")

        assertFalse(chip.contains("TextOverflow.Ellipsis"))
        assertTrue(Regex("FlowRow\\(").findAll(selectors).count() >= 2)
        assertTrue(selectors.contains("spacing.chipHorizontalGap"))
        assertTrue(selectors.contains("spacing.chipVerticalGap"))
        assertFalse(selectors.contains("modifier = Modifier.weight(1f)"))
    }

    @Test
    fun compactNavigationAlwaysShowsLabelsAndKeepsTabSemantics() {
        val source = source("ui/components/BottomNavBar.kt")

        assertTrue(source.contains("role = Role.Tab"))
        assertTrue(source.contains("colors.accentContainer"))
        assertTrue(source.contains("if (isSelected) colors.accent else colors.material.onSurfaceVariant"))
        assertFalse(source.contains("if (isSelected) {\n                Text("))
    }

    private fun source(relativePath: String): String =
        projectFile("src/main/java/com/dbcheck/app/$relativePath").readText()

    private fun composeCalls(source: String, functionName: String): List<String> {
        val marker = "$functionName("
        val calls = mutableListOf<String>()
        var searchStart = 0
        while (true) {
            val start = source.indexOf(marker, searchStart)
            if (start < 0) break
            var depth = 1
            var end = start + marker.length
            do {
                when (source[end]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                end++
            } while (end < source.length && depth > 0)
            calls += source.substring(start, end)
            searchStart = end
        }
        return calls
    }
}
