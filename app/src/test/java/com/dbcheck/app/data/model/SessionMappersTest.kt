package com.dbcheck.app.data.model

import com.dbcheck.app.data.local.db.entity.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionMappersTest {
    @Test
    fun toDomainModelMapsCompleteLocationMetadata() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = 1_700_000_000_000L,
                endTime = 1_700_000_060_000L,
                avgDb = 72f,
                frequencyWeighting = "A",
                locationLatitude = 60.1699,
                locationLongitude = 24.9384,
                locationAccuracyMeters = 18.5f,
                locationCapturedAt = 1_700_000_001_000L,
            )

        val location = checkNotNull(session.toDomainModel().location)

        assertEquals(60.1699, location.latitude, 0.0001)
        assertEquals(24.9384, location.longitude, 0.0001)
        assertEquals(18.5f, location.accuracyMeters)
        assertEquals(1_700_000_001_000L, location.capturedAt)
    }

    @Test
    fun toDomainModelOmitsIncompleteLocationMetadata() {
        val session =
            SessionEntity(
                id = 7L,
                startTime = 1_700_000_000_000L,
                avgDb = 72f,
                frequencyWeighting = "A",
                locationLatitude = 60.1699,
                locationLongitude = 24.9384,
            )

        assertNull(session.toDomainModel().location)
    }
}
