package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundClassificationPolicyTest {
    @Test
    fun selectBestReturnsNullForEmptyOutput() {
        val result = SoundClassificationPolicy.selectBest(emptyList())

        assertNull(result)
    }

    @Test
    fun selectBestFiltersCandidatesBelowConfidenceThreshold() {
        val result =
            SoundClassificationPolicy.selectBest(
                listOf(
                    SoundClassificationCandidate(
                        label = "Speech",
                        confidence = SoundClassifierConfig.MIN_CONFIDENCE - 0.01f,
                    ),
                ),
            )

        assertNull(result)
    }

    @Test
    fun selectBestAcceptsCandidateAtConfidenceThreshold() {
        val result =
            SoundClassificationPolicy.selectBest(
                listOf(
                    SoundClassificationCandidate(
                        label = "Speech",
                        confidence = SoundClassifierConfig.MIN_CONFIDENCE,
                    ),
                ),
            )

        assertEquals(SoundClassification(label = "Speech", confidence = SoundClassifierConfig.MIN_CONFIDENCE), result)
    }

    @Test
    fun selectBestReturnsHighestConfidenceCandidateAboveThreshold() {
        val result =
            SoundClassificationPolicy.selectBest(
                listOf(
                    SoundClassificationCandidate(label = "Speech", confidence = 0.40f),
                    SoundClassificationCandidate(label = "Music", confidence = 0.85f),
                    SoundClassificationCandidate(label = "Vehicle", confidence = 0.70f),
                ),
            )

        assertEquals(SoundClassification(label = "Music", confidence = 0.85f), result)
    }
}
