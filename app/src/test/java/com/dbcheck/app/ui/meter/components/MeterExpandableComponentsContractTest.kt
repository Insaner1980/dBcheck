package com.dbcheck.app.ui.meter.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class MeterExpandableComponentsContractTest {
    @Test
    fun liveDetailsRowIsTokenizedAccessibleAndGatesChartAndWaveform() {
        val source =
            projectFile("src/main/java/com/dbcheck/app/ui/meter/components/LiveActivityCard.kt").readText()
        val expandedBody = source.substringAfter("if (expanded)")

        assertTrue(source.contains("R.string.meter_live_details"))
        assertTrue(source.contains(".heightIn(min = spacing.space12)"))
        assertTrue(source.contains("this.stateDescription = stateLabel"))
        assertTrue(source.contains("onExpandedChange(!expanded)"))
        assertTrue(expandedBody.contains("LiveSoundLevelChart("))
        assertTrue(expandedBody.contains("WaveformVisualization("))
    }

    @Test
    fun soundReferenceCollapsedRowContainsRequiredFieldsAndExpandedBodyRetainsRailAndList() {
        val source =
            projectFile("src/main/java/com/dbcheck/app/ui/meter/components/SoundReferenceCard.kt").readText()
        val collapsedRow =
            source
                .substringAfter("private fun SoundReferenceCollapsedRow")
                .substringBefore("private fun SoundReferenceExpandedContent")
        val expandedBody = source.substringAfter("private fun SoundReferenceExpandedContent")

        assertTrue(collapsedRow.contains("nearestReference.label"))
        assertTrue(collapsedRow.contains("R.string.sound_reference_db_short"))
        assertTrue(collapsedRow.contains("R.string.sound_reference_current_db"))
        assertTrue(collapsedRow.contains("Icons.Outlined.Expand"))
        assertTrue(collapsedRow.contains(".heightIn(min = spacing.space12)"))
        assertTrue(collapsedRow.contains("this.stateDescription = stateLabel"))
        assertTrue(expandedBody.contains("SoundReferenceRail("))
        assertTrue(expandedBody.contains("markers.forEach"))
    }
}
