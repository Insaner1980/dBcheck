package com.dbcheck.app.domain.hearingtest

import org.junit.Assert.assertEquals
import org.junit.Test

class HearingTestPolicyTest {
    @Test
    fun policyOwnsFrequenciesAndToneTiming() {
        assertEquals(listOf(250f, 500f, 1000f, 2000f, 4000f, 8000f), HearingTestPolicy.TEST_FREQUENCIES)
        assertEquals(250f, HearingTestPolicy.MIN_FREQUENCY_HZ)
        assertEquals(8000f, HearingTestPolicy.MAX_FREQUENCY_HZ)
        assertEquals(1500L, HearingTestPolicy.TONE_DURATION_MS)
    }

    @Test
    fun ratingCodesAreCentralized() {
        assertEquals(HearingRating.EXCELLENT, HearingRating.fromScore(90))
        assertEquals(HearingRating.GOOD, HearingRating.fromScore(75))
        assertEquals(HearingRating.FAIR, HearingRating.fromScore(50))
        assertEquals(HearingRating.POOR, HearingRating.fromScore(49))
        assertEquals(HearingRating.POOR, HearingRating.fromCode("unknown"))
    }
}
