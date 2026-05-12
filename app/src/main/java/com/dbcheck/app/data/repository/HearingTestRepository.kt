package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.data.local.db.entity.HearingTestResultEntity
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.domain.hearingtest.HearingTestThresholdCodec
import com.dbcheck.app.domain.hearingtest.TestKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HearingTestRepository
    @Inject
    constructor(
        private val hearingTestDao: HearingTestDao,
    ) {
    fun getLatestResult(): Flow<HearingTestResult?> =
        hearingTestDao.getLatestResult().map { entity -> entity?.toDomainModel() }

    fun getResultById(id: Long): Flow<HearingTestResult?> =
        hearingTestDao.getResultById(id).map { entity -> entity?.toDomainModel() }

    suspend fun insertResult(result: HearingTestResult): HearingTestResult {
        val id = hearingTestDao.insertResult(result.toEntity())
        return result.copy(id = id)
    }
    }

private fun HearingTestResultEntity.toDomainModel(): HearingTestResult =
    HearingTestResult(
        id = id,
        timestamp = timestamp,
        overallScore = overallScore,
        rating = rating,
        leftEarThresholds = HearingTestThresholdCodec.parseEarData(leftEarData),
        rightEarThresholds = HearingTestThresholdCodec.parseEarData(rightEarData),
        speechClarity = speechClarity,
        highFreqLimit = highFreqLimit,
        avgThreshold = avgThreshold,
    )

private fun HearingTestResult.toEntity(): HearingTestResultEntity =
    HearingTestResultEntity(
        id = id,
        timestamp = timestamp,
        overallScore = overallScore,
        rating = rating,
        leftEarData =
            HearingTestThresholdCodec.serializeEarData(
                leftEarThresholds.toThresholdMap(Ear.LEFT),
                Ear.LEFT,
            ),
        rightEarData =
            HearingTestThresholdCodec.serializeEarData(
                rightEarThresholds.toThresholdMap(Ear.RIGHT),
                Ear.RIGHT,
            ),
        speechClarity = speechClarity,
        highFreqLimit = highFreqLimit,
        avgThreshold = avgThreshold,
    )

private fun List<Pair<Float, Float>>.toThresholdMap(ear: Ear) =
    associate { (frequency, threshold) ->
        TestKey(ear, frequency) to threshold
    }
