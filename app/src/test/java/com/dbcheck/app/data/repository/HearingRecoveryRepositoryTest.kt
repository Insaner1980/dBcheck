package com.dbcheck.app.data.repository

import com.dbcheck.app.data.local.db.dao.HearingRecoveryDao
import com.dbcheck.app.data.local.db.entity.HearingRecoveryResultEntity
import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
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

class HearingRecoveryRepositoryTest {
    private val hearingRecoveryDao = mockk<HearingRecoveryDao>()
    private val repository = HearingRecoveryRepository(hearingRecoveryDao)

    @Test
    fun latestResultMapsSerializedEarShiftsToDomainModel() = runTest {
        every { hearingRecoveryDao.getLatestResult() } returns
            flowOf(
                entity(
                    leftEarShiftData = "1000.0:5.0,4000.0:10.0,bad-entry",
                    rightEarShiftData = "1000.0:-5.0,8000.0:20.0",
                ),
            )

        val result = repository.getLatestResult().first()

        checkNotNull(result)
        assertEquals(RESULT_ID, result.id)
        assertEquals(BASELINE_ID, result.baselineTestId)
        assertEquals(TIMESTAMP, result.timestamp)
        assertEquals(4, result.testedFrequencyCount)
        assertEquals(7.5f, result.averageShiftDb)
        assertEquals(20f, result.maxShiftDb)
        assertEquals(HearingRecoveryStatus.ELEVATED_SHIFT, result.status)
        assertEquals(listOf(1000f to 5f, 4000f to 10f), result.leftEarShifts)
        assertEquals(listOf(1000f to -5f, 8000f to 20f), result.rightEarShifts)
    }

    @Test
    fun latestResultMapsMissingEntityToNull() = runTest {
        every { hearingRecoveryDao.getLatestResult() } returns flowOf(null)

        assertNull(repository.getLatestResult().first())
    }

    @Test
    fun insertResultSerializesEarShiftsAndReturnsAssignedId() = runTest {
        val insertedEntity = slot<HearingRecoveryResultEntity>()
        coEvery { hearingRecoveryDao.insertResult(capture(insertedEntity)) } returns INSERTED_ID

        val inserted = repository.insertResult(domainResult())

        assertEquals(INSERTED_ID, inserted.id)
        with(insertedEntity.captured) {
            assertEquals(0L, id)
            assertEquals(BASELINE_ID, baselineTestId)
            assertEquals(TIMESTAMP, timestamp)
            assertEquals(3, testedFrequencyCount)
            assertEquals(5f, averageShiftDb)
            assertEquals(10f, maxShiftDb)
            assertEquals("STABLE", status)
            assertEquals("1000.0:5.0,4000.0:0.0", leftEarShiftData)
            assertEquals("8000.0:10.0", rightEarShiftData)
        }
        coVerify(exactly = 1) { hearingRecoveryDao.insertResult(any()) }
    }

    private fun entity(leftEarShiftData: String, rightEarShiftData: String): HearingRecoveryResultEntity =
        HearingRecoveryResultEntity(
            id = RESULT_ID,
            baselineTestId = BASELINE_ID,
            timestamp = TIMESTAMP,
            testedFrequencyCount = 4,
            averageShiftDb = 7.5f,
            maxShiftDb = 20f,
            status = "ELEVATED_SHIFT",
            leftEarShiftData = leftEarShiftData,
            rightEarShiftData = rightEarShiftData,
        )

    private fun domainResult(): HearingRecoveryResult = HearingRecoveryResult(
        baselineTestId = BASELINE_ID,
        timestamp = TIMESTAMP,
        testedFrequencyCount = 3,
        averageShiftDb = 5f,
        maxShiftDb = 10f,
        status = HearingRecoveryStatus.STABLE,
        leftEarShifts = listOf(1000f to 5f, 4000f to 0f),
        rightEarShifts = listOf(8000f to 10f),
    )

    private companion object {
        const val RESULT_ID = 7L
        const val INSERTED_ID = 42L
        const val BASELINE_ID = 99L
        const val TIMESTAMP = 1_700_000_000_000L
    }
}
