package com.dbcheck.app.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeSoundClassifierTest {
    @Test
    fun fakeReturnsQueuedResultsInOrderAndCopiesInputWindows() {
        val classifier =
            FakeSoundClassifier(
                initialResults =
                    listOf(
                        SoundClassification(label = "Speech", confidence = 0.80f),
                        SoundClassification(label = "Music", confidence = 0.70f),
                    ),
            )
        val firstWindow = floatArrayOf(0.1f, 0.2f)
        val secondWindow = floatArrayOf(0.3f)

        val firstResult = classifier.classify(firstWindow)
        val secondResult = classifier.classify(secondWindow)
        firstWindow[0] = 1f

        assertEquals(SoundClassification(label = "Speech", confidence = 0.80f), firstResult)
        assertEquals(SoundClassification(label = "Music", confidence = 0.70f), secondResult)
        assertEquals(2, classifier.classifiedWindows.size)
        assertEquals(0.1f, classifier.classifiedWindows[0][0], 0f)
    }

    @Test
    fun fakeReturnsNullWhenNoResultIsQueued() {
        val classifier = FakeSoundClassifier()

        val result = classifier.classify(floatArrayOf(0f))

        assertNull(result)
    }
}
