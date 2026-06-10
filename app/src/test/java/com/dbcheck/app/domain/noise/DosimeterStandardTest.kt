package com.dbcheck.app.domain.noise

import org.junit.Assert.assertEquals
import org.junit.Test

class DosimeterStandardTest {
    @Test
    fun dosimeterStandardsExposeStablePreferenceValues() {
        assertEquals("niosh_rel", DosimeterStandard.NIOSH_REL.preferenceValue)
        assertEquals("osha_pel", DosimeterStandard.OSHA_PEL.preferenceValue)
    }

    @Test
    fun dosimeterStandardFallsBackToNioshForUnknownPreference() {
        assertEquals(DosimeterStandard.NIOSH_REL, DosimeterStandard.fromPreference(null))
        assertEquals(DosimeterStandard.NIOSH_REL, DosimeterStandard.fromPreference("unknown"))
        assertEquals(DosimeterStandard.OSHA_PEL, DosimeterStandard.fromPreference("osha_pel"))
    }
}
