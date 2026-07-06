package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.PassiveMonitoringDailySummaryRow
import com.dbcheck.app.data.local.db.dao.PassiveMonitoringDao
import com.dbcheck.app.data.local.db.entity.PassiveMonitoringSampleEntity
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.domain.noise.DecibelMath
import com.dbcheck.app.domain.passive.PassiveMonitoringDailySummary
import com.dbcheck.app.domain.passive.PassiveMonitoringSample
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassiveMonitoringRepository
    @Inject
    constructor(
        private val passiveMonitoringDao: PassiveMonitoringDao,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        suspend fun recordSample(sample: PassiveMonitoringSample): Long =
            passiveMonitoringDao.insertSample(sample.toEntity())

        fun observeDailySummary(startTimeMs: Long, endTimeMs: Long): Flow<PassiveMonitoringDailySummary> =
            passiveMonitoringDao
                .observeDailySummary(startTimeMs = startTimeMs, endTimeMs = endTimeMs)
                .map { row -> row.toDomainModel() }
                .flowOn(defaultDispatcher)

        suspend fun clearAllSamples() {
            passiveMonitoringDao.deleteAllSamples()
        }
    }

private fun PassiveMonitoringSample.toEntity(): PassiveMonitoringSampleEntity = PassiveMonitoringSampleEntity(
        id = id,
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        readingCount = readingCount,
        minDb = minDb,
        averageDb = averageDb,
        maxDb = maxDb,
        peakDb = peakDb,
        totalEnergy = totalEnergy,
    )

private fun PassiveMonitoringDailySummaryRow.toDomainModel(): PassiveMonitoringDailySummary {
    if (sampleCount <= 0 || readingCount <= 0) {
        return PassiveMonitoringDailySummary()
    }
    return PassiveMonitoringDailySummary(
        sampleCount = sampleCount,
        readingCount = readingCount,
        minDb = minDb,
        averageDb = DecibelMath.energyAverageDb(totalEnergy ?: 0.0, readingCount),
        maxDb = maxDb,
        peakDb = peakDb,
    )
}
