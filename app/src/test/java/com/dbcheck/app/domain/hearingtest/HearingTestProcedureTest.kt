package com.dbcheck.app.domain.hearingtest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HearingTestProcedureTest {
    @Test
    fun modifiedHughsonWestlakeFindsThresholdAfterTwoAscendingHeardResponses() {
        val procedure = HearingTestProcedure(frequencies = listOf(1_000f), ears = listOf(Ear.LEFT))

        val first = procedure.start()
        assertEquals(-30f, first.amplitudeDb, 0f)

        val louder = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-25f, louder.progress.amplitudeDb, 0f)

        val quieter = procedure.onHeard() as HearingTestStepResult.Continue
        assertEquals(-35f, quieter.progress.amplitudeDb, 0f)

        val secondLouder = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-30f, secondLouder.progress.amplitudeDb, 0f)

        val completed = procedure.onHeard() as HearingTestStepResult.Completed
        assertEquals(mapOf(TestKey(Ear.LEFT, 1_000f) to -30f), completed.thresholds)
    }

    @Test
    fun continuousHeardResponsesRecordExcellentFloorThreshold() {
        val procedure = HearingTestProcedure(frequencies = listOf(1_000f), ears = listOf(Ear.LEFT))

        procedure.start()
        assertTrue(procedure.onHeard() is HearingTestStepResult.Continue)
        assertTrue(procedure.onHeard() is HearingTestStepResult.Continue)
        assertTrue(procedure.onHeard() is HearingTestStepResult.Continue)

        val completed = procedure.onHeard() as HearingTestStepResult.Completed

        assertEquals(mapOf(TestKey(Ear.LEFT, 1_000f) to -60f), completed.thresholds)
    }
}
