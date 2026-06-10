package com.dbcheck.app.domain.noise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundReferenceCatalogTest {
    @Test
    fun referencesAreSortedByRepresentativeDbAndHaveUniqueIds() {
        val references = SoundReferenceCatalog.references

        assertTrue(references.zipWithNext().all { (left, right) -> left.db <= right.db })
        assertEquals(references.size, references.map { it.id }.toSet().size)
    }

    @Test
    fun nearestReferenceUsesClosestRepresentativeDb() {
        assertSame(SoundReferenceId.WHISPERING, SoundReferenceCatalog.nearestReference(32f).id)
        assertSame(SoundReferenceId.CONVERSATION, SoundReferenceCatalog.nearestReference(67f).id)
        assertSame(SoundReferenceId.SIREN, SoundReferenceCatalog.nearestReference(123f).id)
    }

    @Test
    fun nearestReferenceTieBreaksToLowerDbReference() {
        assertSame(SoundReferenceId.WHISPERING, SoundReferenceCatalog.nearestReference(35f).id)
    }

    @Test
    fun markerPositionClampsToZeroToOneThirtyDbAxis() {
        assertEquals(0f, SoundReferenceCatalog.markerPosition(-10f), 0.001f)
        assertEquals(0.5f, SoundReferenceCatalog.markerPosition(65f), 0.001f)
        assertEquals(1f, SoundReferenceCatalog.markerPosition(140f), 0.001f)
    }

    @Test
    fun nearestReferenceMarkerUsesReferenceDbForPosition() {
        val marker = SoundReferenceCatalog.nearestReferenceMarker(123f)

        assertSame(SoundReferenceId.SIREN, marker.reference.id)
        assertEquals(120f / 130f, marker.position, 0.001f)
    }
}
