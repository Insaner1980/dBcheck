package com.dbcheck.app.domain.report

import com.dbcheck.app.domain.audio.WeightingType
import org.junit.Assert.assertEquals
import org.junit.Test

class EquivalentLevelLabelTest {
    @Test
    fun labelsFollowFrequencyWeightingType() {
        assertEquals("LAeq", equivalentLevelLabelForWeighting(WeightingType.A.name))
        assertEquals("LBeq", equivalentLevelLabelForWeighting(WeightingType.B.name))
        assertEquals("LCeq", equivalentLevelLabelForWeighting(WeightingType.C.name))
        assertEquals("LZeq", equivalentLevelLabelForWeighting(WeightingType.Z.name))
        assertEquals("Leq (ITU-R 468)", equivalentLevelLabelForWeighting(WeightingType.ITUR468.name))
    }

    @Test
    fun unknownWeightingFallsBackToGenericEquivalentLevel() {
        assertEquals("Leq", equivalentLevelLabelForWeighting("unknown"))
    }
}
