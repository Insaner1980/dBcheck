package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.HearingTestDao
import com.dbcheck.app.data.local.db.entity.HearingTestResultEntity
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HearingTestRepositoryTest {
    private val hearingTestDao = mockk<HearingTestDao>()
    private val repository = HearingTestRepository(hearingTestDao)

    @Test
    fun latestResultMapsEntityThresholdDataToDomainModel() = runTest {
        every { hearingTestDao.getLatestResult() } returns
            flowOf(
                entity(
                    leftEarData = "1000.0:25.0,250.0:10.0,bad-entry",
                    rightEarData = "500.0:15.0,2000.0:35.0",
                ),
            )

        val result = repository.getLatestResult().first()

        checkNotNull(result)
        assertEquals(RESULT_ID, result.id)
        assertEquals(TIMESTAMP, result.timestamp)
        assertEquals(88, result.overallScore)
        assertEquals("Good", result.rating)
        assertEquals(listOf(250f to 10f, 1000f to 25f), result.leftEarThresholds)
        assertEquals(listOf(500f to 15f, 2000f to 35f), result.rightEarThresholds)
        assertEquals(91.5f, result.speechClarity)
        assertEquals(8000f, result.highFreqLimit)
        assertEquals(21.25f, result.avgThreshold)
    }

    @Test
    fun resultByIdMapsMissingEntityToNull() = runTest {
        every { hearingTestDao.getResultById(RESULT_ID) } returns flowOf(null)

        assertNull(repository.getResultById(RESULT_ID).first())
    }

    @Test
    fun insertResultSerializesThresholdsAndReturnsAssignedId() = runTest {
        val insertedEntity = slot<HearingTestResultEntity>()
        coEvery { hearingTestDao.insertResult(capture(insertedEntity)) } returns INSERTED_ID

        val inserted = repository.insertResult(domainResult())

        assertEquals(INSERTED_ID, inserted.id)
        with(insertedEntity.captured) {
            assertEquals(0L, id)
            assertEquals(TIMESTAMP, timestamp)
            assertEquals(93, overallScore)
            assertEquals("Excellent", rating)
            assertEquals("250.0:5.0,1000.0:20.0", leftEarData)
            assertEquals("500.0:10.0,2000.0:30.0", rightEarData)
            assertEquals(95f, speechClarity)
            assertEquals(8000f, highFreqLimit)
            assertEquals(16.25f, avgThreshold)
        }
        coVerify(exactly = 1) { hearingTestDao.insertResult(any()) }
    }

    private fun entity(leftEarData: String, rightEarData: String): HearingTestResultEntity = HearingTestResultEntity(
        id = RESULT_ID,
        timestamp = TIMESTAMP,
        overallScore = 88,
        rating = "Good",
        leftEarData = leftEarData,
        rightEarData = rightEarData,
        speechClarity = 91.5f,
        highFreqLimit = 8000f,
        avgThreshold = 21.25f,
    )

    private fun domainResult(): HearingTestResult = HearingTestResult(
        timestamp = TIMESTAMP,
        overallScore = 93,
        rating = "Excellent",
        leftEarThresholds = listOf(1000f to 20f, 250f to 5f),
        rightEarThresholds = listOf(2000f to 30f, 500f to 10f),
        speechClarity = 95f,
        highFreqLimit = 8000f,
        avgThreshold = 16.25f,
    )

    private companion object {
        const val RESULT_ID = 7L
        const val INSERTED_ID = 42L
        const val TIMESTAMP = 1_700_000_000_000L
    }
}
