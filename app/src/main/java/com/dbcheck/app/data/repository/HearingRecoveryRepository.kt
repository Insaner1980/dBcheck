package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.HearingRecoveryDao
import com.dbcheck.app.data.local.db.entity.HearingRecoveryResultEntity
import com.dbcheck.app.domain.hearingtest.HearingFrequencyValueCodec
import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HearingRecoveryRepository
    @Inject
    constructor(private val hearingRecoveryDao: HearingRecoveryDao) {
        fun getLatestResult(): Flow<HearingRecoveryResult?> =
            hearingRecoveryDao.getLatestResult().map { entity -> entity?.toDomainModel() }

        suspend fun insertResult(result: HearingRecoveryResult): HearingRecoveryResult {
            val id = hearingRecoveryDao.insertResult(result.toEntity())
            return result.copy(id = id)
        }
    }

private fun HearingRecoveryResultEntity.toDomainModel(): HearingRecoveryResult = HearingRecoveryResult(
    id = id,
    baselineTestId = baselineTestId,
    timestamp = timestamp,
    testedFrequencyCount = testedFrequencyCount,
    averageShiftDb = averageShiftDb,
    maxShiftDb = maxShiftDb,
    status = HearingRecoveryStatus.entries.firstOrNull { it.name == status } ?: HearingRecoveryStatus.STABLE,
    leftEarShifts = HearingFrequencyValueCodec.parse(leftEarShiftData),
    rightEarShifts = HearingFrequencyValueCodec.parse(rightEarShiftData),
)

private fun HearingRecoveryResult.toEntity(): HearingRecoveryResultEntity = HearingRecoveryResultEntity(
    id = id,
    baselineTestId = baselineTestId,
    timestamp = timestamp,
    testedFrequencyCount = testedFrequencyCount,
    averageShiftDb = averageShiftDb,
    maxShiftDb = maxShiftDb,
    status = status.name,
    leftEarShiftData = HearingFrequencyValueCodec.serialize(leftEarShifts),
    rightEarShiftData = HearingFrequencyValueCodec.serialize(rightEarShifts),
)
