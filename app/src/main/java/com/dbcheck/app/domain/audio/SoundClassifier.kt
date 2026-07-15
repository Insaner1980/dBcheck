package com.dbcheck.app.domain.audio

fun interface SoundClassifier : AutoCloseable {
    fun classify(window: FloatArray): SoundClassification?

    override fun close() = Unit
}

data class SoundClassification(val label: String, val confidence: Float)

data class SoundClassificationCandidate(val label: String, val confidence: Float)

object SoundClassifierConfig {
    const val MIN_CONFIDENCE = 0.30f
    const val MAX_RESULTS = 1
}

object SoundClassificationPolicy {
    fun selectBest(
        candidates: List<SoundClassificationCandidate>,
        minConfidence: Float = SoundClassifierConfig.MIN_CONFIDENCE,
    ): SoundClassification? = candidates
            .asSequence()
            .filter { candidate -> candidate.label.isNotBlank() }
            .filter { candidate -> candidate.confidence.isFinite() }
            .filter { candidate -> candidate.confidence >= minConfidence }
            .maxByOrNull { candidate -> candidate.confidence }
            ?.let { candidate ->
                SoundClassification(
                    label = candidate.label,
                    confidence = candidate.confidence,
                )
            }
}
