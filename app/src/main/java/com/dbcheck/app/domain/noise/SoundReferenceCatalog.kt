package com.dbcheck.app.domain.noise

import kotlin.math.abs

data class SoundReference(
    val id: SoundReferenceId,
    val label: String,
    val db: Float,
    val sourceMinDb: Float = db,
    val sourceMaxDb: Float = db,
)

data class SoundReferenceMarker(val reference: SoundReference, val position: Float)

enum class SoundReferenceId {
    WHISPERING,
    REFRIGERATOR,
    DISHWASHER,
    CONVERSATION,
    LAWN_MOWER,
    MOTORCYCLE,
    SPORTS_EVENT,
    ROCK_CONCERT,
    SIREN,
    FIREWORKS,
}

object SoundReferenceCatalog {
    const val SOURCE_URL = "https://www.cdc.gov/nceh/hearing_loss/infographic/"

    val references: List<SoundReference> =
        listOf(
            SoundReference(
                id = SoundReferenceId.WHISPERING,
                label = "Whispering",
                db = 30f,
            ),
            SoundReference(
                id = SoundReferenceId.REFRIGERATOR,
                label = "Refrigerator",
                db = 40f,
            ),
            SoundReference(
                id = SoundReferenceId.DISHWASHER,
                label = "Dishwasher",
                db = 55f,
                sourceMinDb = 45f,
                sourceMaxDb = 65f,
            ),
            SoundReference(
                id = SoundReferenceId.CONVERSATION,
                label = "Conversation",
                db = 70f,
                sourceMinDb = 65f,
                sourceMaxDb = 80f,
            ),
            SoundReference(
                id = SoundReferenceId.LAWN_MOWER,
                label = "Lawn mower",
                db = 90f,
                sourceMinDb = 80f,
                sourceMaxDb = 100f,
            ),
            SoundReference(
                id = SoundReferenceId.MOTORCYCLE,
                label = "Motorcycle",
                db = 95f,
                sourceMinDb = 80f,
                sourceMaxDb = 110f,
            ),
            SoundReference(
                id = SoundReferenceId.SPORTS_EVENT,
                label = "Sports event",
                db = 100f,
                sourceMinDb = 94f,
                sourceMaxDb = 110f,
            ),
            SoundReference(
                id = SoundReferenceId.ROCK_CONCERT,
                label = "Rock concert",
                db = 105f,
                sourceMinDb = 95f,
                sourceMaxDb = 115f,
            ),
            SoundReference(
                id = SoundReferenceId.SIREN,
                label = "Siren",
                db = 120f,
                sourceMinDb = 110f,
                sourceMaxDb = 129f,
            ),
            SoundReference(
                id = SoundReferenceId.FIREWORKS,
                label = "Fireworks",
                db = 140f,
                sourceMinDb = 140f,
                sourceMaxDb = 160f,
            ),
        )

    val referenceMarkers: List<SoundReferenceMarker> = references.map(::markerForReference)

    fun nearestReference(db: Float): SoundReference = references.minWith(
        compareBy<SoundReference> { abs(it.db - db) }
            .thenBy { it.db },
    )

    fun markerPosition(db: Float): Float = SoundLevelDisplayScale.positionForDb(db)

    fun nearestReferenceMarker(db: Float): SoundReferenceMarker = markerForReference(nearestReference(db))

    private fun markerForReference(reference: SoundReference): SoundReferenceMarker = SoundReferenceMarker(
        reference = reference,
        position = markerPosition(reference.db),
    )
}
