package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.domain.audio.SoundClassification
import com.dbcheck.app.domain.audio.SoundClassificationCandidate
import com.dbcheck.app.domain.audio.SoundClassificationPolicy
import com.dbcheck.app.domain.audio.SoundClassifier
import com.dbcheck.app.domain.audio.SoundClassifierConfig
import com.dbcheck.app.domain.audio.YamnetAudioConfig
import com.dbcheck.app.domain.audio.YamnetModelAssets
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.core.BaseOptions

class MediaPipeSoundClassifier internal constructor(private val classifierFactory: () -> MediaPipeClassifierRuntime) :
    SoundClassifier {
    constructor(context: Context) : this(
        classifierFactory = {
            val baseOptions =
                BaseOptions.builder()
                    .setModelAssetPath(YamnetModelAssets.MODEL_PATH)
                    .build()
            val classifierOptions =
                AudioClassifier.AudioClassifierOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMaxResults(SoundClassifierConfig.MAX_RESULTS)
                    .setScoreThreshold(SoundClassifierConfig.MIN_CONFIDENCE)
                    .build()
            AndroidMediaPipeClassifierRuntime(
                AudioClassifier.createFromOptions(context, classifierOptions),
            )
        },
    )

    private val classifierLock = Any()
    private var classifier: MediaPipeClassifierRuntime? = null

    override fun classify(window: FloatArray): SoundClassification? {
        if (window.isEmpty()) {
            return null
        }

        return synchronized(classifierLock) {
            val activeClassifier = classifier ?: classifierFactory().also { classifier = it }
            SoundClassificationPolicy.selectBest(
                activeClassifier.classify(window),
            )
        }
    }

    override fun close() {
        synchronized(classifierLock) {
            val activeClassifier = classifier
            classifier = null
            activeClassifier?.close()
        }
    }
}

internal interface MediaPipeClassifierRuntime : AutoCloseable {
    fun classify(window: FloatArray): List<SoundClassificationCandidate>
}

private class AndroidMediaPipeClassifierRuntime(private val classifier: AudioClassifier) :
    MediaPipeClassifierRuntime,
    AutoCloseable by classifier {
    override fun classify(window: FloatArray): List<SoundClassificationCandidate> {
        val audioData =
            AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(YAMNET_CHANNEL_COUNT)
                    .setSampleRate(YamnetAudioConfig.SAMPLE_RATE_HZ.toFloat())
                    .build(),
                window.size,
            ).also { data -> data.load(window) }

        return classifier
            .classify(audioData)
            .classificationResults()
            .flatMap { result -> result.classifications() }
            .flatMap { classifications -> classifications.categories() }
            .map(Category::toSoundClassificationCandidate)
    }
}

private fun Category.toSoundClassificationCandidate(): SoundClassificationCandidate = SoundClassificationCandidate(
    label = categoryName(),
    confidence = score(),
)

private const val YAMNET_CHANNEL_COUNT = 1
