package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.domain.audio.SoundClassification
import com.dbcheck.app.domain.audio.SoundClassificationCandidate
import com.dbcheck.app.domain.audio.SoundClassificationPolicy
import com.dbcheck.app.domain.audio.SoundClassifier
import com.dbcheck.app.domain.audio.SoundClassifierConfig
import com.dbcheck.app.domain.audio.YamnetModelAssets
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.AudioClassifier.AudioClassifierOptions

class TfliteSoundClassifier internal constructor(private val classifierFactory: () -> AudioClassifier) :
    SoundClassifier {
    constructor(context: Context) : this(
        classifierFactory = {
            AudioClassifier.createFromFileAndOptions(
                context,
                YamnetModelAssets.MODEL_PATH,
                AudioClassifierOptions.builder()
                    .setMaxResults(SoundClassifierConfig.MAX_RESULTS)
                    .setScoreThreshold(SoundClassifierConfig.MIN_CONFIDENCE)
                    .build(),
            )
        },
    )

    private val classifierLock = Any()
    private var classifier: AudioClassifier? = null

    override fun classify(window: FloatArray): SoundClassification? {
        if (window.isEmpty()) {
            return null
        }

        return synchronized(classifierLock) {
            val activeClassifier = classifier ?: classifierFactory().also { classifier = it }
            val audio = activeClassifier.createInputTensorAudio()
            audio.load(window)
            SoundClassificationPolicy.selectBest(
                activeClassifier.classify(audio).flatMap { classifications ->
                    classifications.categories.map(Category::toSoundClassificationCandidate)
                },
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

private fun Category.toSoundClassificationCandidate(): SoundClassificationCandidate = SoundClassificationCandidate(
        label = label,
        confidence = score,
    )
