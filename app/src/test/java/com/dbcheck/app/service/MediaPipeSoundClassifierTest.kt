package com.dbcheck.app.service

import com.dbcheck.app.domain.audio.SoundClassificationCandidate
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaPipeSoundClassifierTest {
    @Test
    fun closeReleasesClassifierAndNextClassificationCreatesANewInstance() {
        val firstClassifier = emptyClassifier()
        val secondClassifier = emptyClassifier()
        val classifierFactory = mockk<() -> MediaPipeClassifierRuntime>()
        every { classifierFactory.invoke() } returnsMany listOf(firstClassifier, secondClassifier)
        val classifier = MediaPipeSoundClassifier(classifierFactory)

        assertNull(classifier.classify(floatArrayOf(0.1f)))
        classifier.close()
        assertNull(classifier.classify(floatArrayOf(0.2f)))

        verify(exactly = 1) { firstClassifier.close() }
        verify(exactly = 1) { firstClassifier.classify(any()) }
        verify(exactly = 1) { secondClassifier.classify(any()) }
        verify(exactly = 2) { classifierFactory.invoke() }
    }

    @Test
    fun mediaPipeCategoriesMapToDomainClassification() {
        val mediaPipeClassifier = mockk<MediaPipeClassifierRuntime>()
        every { mediaPipeClassifier.classify(any()) } returns
            listOf(SoundClassificationCandidate(label = "Speech", confidence = 0.8f))
        every { mediaPipeClassifier.close() } just runs
        val classifier =
            MediaPipeSoundClassifier(
                classifierFactory = { mediaPipeClassifier },
            )

        val classification = classifier.classify(floatArrayOf(0.1f))

        assertEquals("Speech", classification?.label)
        assertEquals(0.8f, classification?.confidence)
    }

    @Test
    fun closeBeforeFirstClassificationDoesNotLoadModel() {
        val classifierFactory = mockk<() -> MediaPipeClassifierRuntime>()
        val classifier = MediaPipeSoundClassifier(classifierFactory)

        classifier.close()

        verify(exactly = 0) { classifierFactory.invoke() }
    }

    private fun emptyClassifier(): MediaPipeClassifierRuntime {
        val classifier = mockk<MediaPipeClassifierRuntime>()
        every { classifier.classify(any()) } returns emptyList()
        every { classifier.close() } just runs
        return classifier
    }
}
