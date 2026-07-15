package com.dbcheck.app.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertNull
import org.junit.Test
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class TfliteSoundClassifierTest {
    @Test
    fun closeReleasesClassifierAndNextClassificationCreatesANewInstance() {
        val firstClassifier = emptyClassifier()
        val secondClassifier = emptyClassifier()
        val classifierFactory = mockk<() -> AudioClassifier>()
        every { classifierFactory.invoke() } returnsMany listOf(firstClassifier, secondClassifier)
        val classifier = TfliteSoundClassifier(classifierFactory)

        assertNull(classifier.classify(floatArrayOf(0.1f)))
        classifier.close()
        assertNull(classifier.classify(floatArrayOf(0.2f)))

        verify(exactly = 1) { firstClassifier.close() }
        verify(exactly = 1) { firstClassifier.createInputTensorAudio() }
        verify(exactly = 1) { secondClassifier.createInputTensorAudio() }
        verify(exactly = 2) { classifierFactory.invoke() }
    }

    @Test
    fun closeBeforeFirstClassificationDoesNotLoadModel() {
        val classifierFactory = mockk<() -> AudioClassifier>()
        val classifier = TfliteSoundClassifier(classifierFactory)

        classifier.close()

        verify(exactly = 0) { classifierFactory.invoke() }
    }

    private fun emptyClassifier(): AudioClassifier {
        val classifier = mockk<AudioClassifier>()
        val audio = mockk<TensorAudio>()
        every { classifier.createInputTensorAudio() } returns audio
        every { audio.load(any<FloatArray>()) } just runs
        every { classifier.classify(audio) } returns emptyList()
        every { classifier.close() } just runs
        return classifier
    }
}
