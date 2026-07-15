package com.dbcheck.app.ui.meter

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeterScreenLayoutContractTest {
    @Test
    fun recordingControlsStayOutsideScrollableReadout() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/MeterScreen.kt").readText()
        val meterContent =
            source
                .substringAfter("private fun MeterContent")
                .substringBefore("private fun MeterControlsSection")

        assertTrue(meterContent.contains(".weight(1f)"))
        assertTrue(meterContent.contains(".verticalScroll(scrollState)"))
        assertEquals(1, Regex("MeterControlsSection\\(").findAll(meterContent).count())
        assertTrue(meterContent.indexOf("MeterReadoutContent(") < meterContent.indexOf("MeterControlsSection("))
    }
}
