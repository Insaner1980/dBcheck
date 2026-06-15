package com.dbcheck.app.domain.audio

class FakeSoundClassifier(initialResults: List<SoundClassification?> = emptyList()) : SoundClassifier {
    private val queuedResults = ArrayDeque(initialResults)

    val classifiedWindows: List<FloatArray>
        get() = mutableClassifiedWindows.map(FloatArray::copyOf)

    private val mutableClassifiedWindows = mutableListOf<FloatArray>()

    fun enqueue(result: SoundClassification?) {
        queuedResults.addLast(result)
    }

    override fun classify(window: FloatArray): SoundClassification? {
        mutableClassifiedWindows += window.copyOf()
        return queuedResults.removeFirstOrNull()
    }
}
