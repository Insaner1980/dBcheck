package com.dbcheck.app.domain.audio

import android.content.Context
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.AudioClassifier.AudioClassifierOptions

class TfliteSoundClassifier(
    private val context: Context,
) : SoundClassifier {
    private val classifier: AudioClassifier by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AudioClassifier.createFromFileAndOptions(
            context,
            YamnetModelAssets.MODEL_PATH,
            AudioClassifierOptions.builder()
                .setMaxResults(SoundClassifierConfig.MAX_RESULTS)
                .setScoreThreshold(SoundClassifierConfig.MIN_CONFIDENCE)
                .build(),
        )
    }

    override fun classify(window: FloatArray): SoundClassification? {
        if (window.isEmpty()) {
            return null
        }

        val audio = classifier.createInputTensorAudio()
        audio.load(window)
        return SoundClassificationPolicy.selectBest(
            classifier.classify(audio).flatMap { classifications ->
                classifications.categories.map(Category::toSoundClassificationCandidate)
            },
        )
    }
}

private fun Category.toSoundClassificationCandidate(): SoundClassificationCandidate =
    SoundClassificationCandidate(
        label = label,
        confidence = score,
    )
