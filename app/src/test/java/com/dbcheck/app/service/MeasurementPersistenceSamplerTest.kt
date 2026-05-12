package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.domain.audio.DecibelReading
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementPersistenceSamplerTest {
    @Test
    fun firstReadingIsPersisted() {
        val sampler = MeasurementPersistenceSampler()

        assertTrue(
            sampler.shouldPersist(
                reading = reading(timestamp = 1_000L, db = 60f),
                refreshRate = MeterRefreshRate.LOW,
                currentMaxDbBeforeReading = 0f,
            ),
        )
    }

    @Test
    fun standardRatePersistsWhenIntervalHasElapsed() {
        val sampler = MeasurementPersistenceSampler()

        sampler.markPersisted(reading(timestamp = 1_000L, db = 60f))

        assertFalse(
            sampler.shouldPersist(
                reading = reading(timestamp = 1_900L, db = 61f),
                refreshRate = MeterRefreshRate.STANDARD,
                currentMaxDbBeforeReading = 62f,
            ),
        )
        assertTrue(
            sampler.shouldPersist(
                reading = reading(timestamp = 2_000L, db = 61f),
                refreshRate = MeterRefreshRate.STANDARD,
                currentMaxDbBeforeReading = 62f,
            ),
        )
    }

    @Test
    fun thresholdCrossingForcesPersistenceBeforeInterval() {
        val sampler = MeasurementPersistenceSampler()

        sampler.markPersisted(reading(timestamp = 1_000L, db = 84f))

        assertTrue(
            sampler.shouldPersist(
                reading = reading(timestamp = 1_100L, db = 85f),
                refreshRate = MeterRefreshRate.LOW,
                currentMaxDbBeforeReading = 84f,
            ),
        )
        sampler.markPersisted(reading(timestamp = 1_100L, db = 85f))
        assertTrue(
            sampler.shouldPersist(
                reading = reading(timestamp = 1_200L, db = 84f),
                refreshRate = MeterRefreshRate.LOW,
                currentMaxDbBeforeReading = 85f,
            ),
        )
    }

    @Test
    fun newSessionMaxForcesPersistenceBeforeInterval() {
        val sampler = MeasurementPersistenceSampler()

        sampler.markPersisted(reading(timestamp = 1_000L, db = 70f))

        assertTrue(
            sampler.shouldPersist(
                reading = reading(timestamp = 1_100L, db = 72f),
                refreshRate = MeterRefreshRate.LOW,
                currentMaxDbBeforeReading = 70f,
            ),
        )
    }

    @Test
    fun stopPersistsLatestUnpersistedReadingOnce() {
        val sampler = MeasurementPersistenceSampler()
        val first = reading(timestamp = 1_000L, db = 70f)
        val latest = reading(timestamp = 1_100L, db = 71f)

        sampler.markPersisted(first)
        sampler.rememberLatest(latest)

        assertTrue(sampler.latestUnpersistedOnStop() === latest)
        sampler.markPersisted(latest)
        assertTrue(sampler.latestUnpersistedOnStop() == null)
    }

    private fun reading(
        timestamp: Long,
        db: Float,
    ) = DecibelReading(
        instantDb = db,
        weightedDb = db,
        timestamp = timestamp,
        peakAmplitude = 0.5f,
    )
}
