package com.dbcheck.app.domain.hearingtest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HearingTestProcedureTest {
    @Test
    fun fullTestVisitsEveryEarFrequencyStageExactlyOnce() {
        val procedure = HearingTestProcedure()
        val expectedKeys = listOf(Ear.LEFT, Ear.RIGHT).flatMap { ear ->
            listOf(250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f).map { TestKey(ear, it) }
        }
        var step: HearingTestStepResult = HearingTestStepResult.Continue(procedure.start())

        expectedKeys.forEachIndexed { index, key ->
            val progress = (step as HearingTestStepResult.Continue).progress
            assertEquals(index + 1, progress.currentPhase)
            assertEquals(12, progress.totalPhases)
            assertEquals(key.ear, progress.currentEar)
            assertEquals(key.frequencyHz, progress.currentFrequency, 0f)
            assertEquals(-30f, progress.amplitudeDb, 0f)
            repeat(4) { step = procedure.onHeard() }
        }

        val completed = step as HearingTestStepResult.Completed
        assertEquals(expectedKeys, completed.thresholds.keys.toList())
        assertTrue(completed.thresholds.values.all { it == -60f })
    }

    @Test
    fun modifiedHughsonWestlakeRequiresTwoAscendingHeardResponsesAtSameAmplitude() {
        val procedure = HearingTestProcedure(frequencies = listOf(1_000f), ears = listOf(Ear.LEFT))

        val first = procedure.start()
        assertEquals(-30f, first.amplitudeDb, 0f)

        val louder = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-25f, louder.progress.amplitudeDb, 0f)

        val quieter = procedure.onHeard() as HearingTestStepResult.Continue
        assertEquals(-35f, quieter.progress.amplitudeDb, 0f)

        val secondLouder = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-30f, secondLouder.progress.amplitudeDb, 0f)

        val heardOnceAtLowerAmplitude = procedure.onHeard() as HearingTestStepResult.Continue
        assertEquals(-40f, heardOnceAtLowerAmplitude.progress.amplitudeDb, 0f)

        val ascendingAgain = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-35f, ascendingAgain.progress.amplitudeDb, 0f)

        val ascendingToPreviousAmplitude = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-30f, ascendingToPreviousAmplitude.progress.amplitudeDb, 0f)

        val completed = procedure.onHeard() as HearingTestStepResult.Completed
        assertEquals(mapOf(TestKey(Ear.LEFT, 1_000f) to -30f), completed.thresholds)
    }

    @Test
    fun initialHeardResponseDoesNotCountAsAscendingHeardResponse() {
        val procedure = HearingTestProcedure(frequencies = listOf(1_000f), ears = listOf(Ear.LEFT))

        procedure.start()
        val initialHeard = procedure.onHeard() as HearingTestStepResult.Continue
        assertEquals(-40f, initialHeard.progress.amplitudeDb, 0f)

        val firstAscendingStep = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-35f, firstAscendingStep.progress.amplitudeDb, 0f)

        val firstAscendingHeard = procedure.onHeard() as HearingTestStepResult.Continue
        assertEquals(-45f, firstAscendingHeard.progress.amplitudeDb, 0f)

        val secondAscendingStep = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-40f, secondAscendingStep.progress.amplitudeDb, 0f)

        val firstAscendingHeardAtLowerAmplitude = procedure.onHeard() as HearingTestStepResult.Continue
        assertEquals(-50f, firstAscendingHeardAtLowerAmplitude.progress.amplitudeDb, 0f)

        val ascendingAgain = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-45f, ascendingAgain.progress.amplitudeDb, 0f)

        val ascendingToPreviousAmplitude = procedure.onNotHeard() as HearingTestStepResult.Continue
        assertEquals(-40f, ascendingToPreviousAmplitude.progress.amplitudeDb, 0f)

        val completed = procedure.onHeard() as HearingTestStepResult.Completed
        assertEquals(mapOf(TestKey(Ear.LEFT, 1_000f) to -40f), completed.thresholds)
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
