package com.dbcheck.app.ui.components

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCardPeakPolicyTest {
    @Test
    fun sessionPeakWarningUsesLcPeakPolicyInsteadOfExposureBoundary() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/SessionCard.kt").readText()

        assertTrue(source.contains("NoiseAlertPolicy.PEAK_WARNING_DB"))
        assertFalse(source.contains("peakDb >= NoiseLevel.ELEVATED.maxDb"))
    }
}
