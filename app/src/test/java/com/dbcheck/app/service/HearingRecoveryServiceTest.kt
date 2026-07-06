package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingRecoveryRepository
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.HearingRecoveryResult
import com.dbcheck.app.domain.hearingtest.HearingRecoveryStatus
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.testRecoveryBaselineResult
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class HearingRecoveryServiceTest {
    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = true))
    private val hearingTestRepository =
        mockk<HearingTestRepository> {
            every { getLatestResult() } returns flowOf(baselineResult())
        }
    private val hearingRecoveryRepository =
        mockk<HearingRecoveryRepository> {
            coEvery { insertResult(any()) } answers {
                firstArg<HearingRecoveryResult>().copy(id = SAVED_RECOVERY_ID)
            }
        }
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }

    @Test
    fun proUserSavesShortRecoveryCheckAgainstLatestBaseline() = runTest {
        val inserted = slot<HearingRecoveryResult>()
        coEvery { hearingRecoveryRepository.insertResult(capture(inserted)) } answers {
            inserted.captured.copy(id = SAVED_RECOVERY_ID)
        }
        val service = createService()

        val id =
            service.saveCompletedRecoveryCheck(
                thresholds =
                    mapOf(
                        TestKey(Ear.LEFT, 1000f) to -20f,
                        TestKey(Ear.LEFT, 4000f) to -30f,
                        TestKey(Ear.LEFT, 8000f) to -30f,
                        TestKey(Ear.RIGHT, 1000f) to -30f,
                        TestKey(Ear.RIGHT, 4000f) to -30f,
                        TestKey(Ear.RIGHT, 8000f) to -10f,
                    ),
                timestamp = RECOVERY_TIMESTAMP,
            )

        assertEquals(SAVED_RECOVERY_ID, id)
        assertEquals(BASELINE_ID, inserted.captured.baselineTestId)
        assertEquals(RECOVERY_TIMESTAMP, inserted.captured.timestamp)
        assertEquals(6, inserted.captured.testedFrequencyCount)
        assertEquals(5f, inserted.captured.averageShiftDb)
        assertEquals(20f, inserted.captured.maxShiftDb)
        assertEquals(HearingRecoveryStatus.ELEVATED_SHIFT, inserted.captured.status)
    }

    @Test
    fun missingBaselineBlocksRecoveryCheckSave() = runTest {
        every { hearingTestRepository.getLatestResult() } returns flowOf(null)
        val service = createService()

        try {
            service.saveCompletedRecoveryCheck(emptyMap(), timestamp = RECOVERY_TIMESTAMP)
            fail("Expected missing baseline to block recovery check")
        } catch (error: IllegalStateException) {
            assertEquals("Take a full hearing test before using short recovery checks", error.message)
        }
        coVerify(exactly = 0) { hearingRecoveryRepository.insertResult(any()) }
    }

    @Test
    fun freeUserCannotSaveRecoveryCheck() = runTest {
        preferencesFlow.value = UserPreferences(isProUser = false)
        val service = createService()

        try {
            service.saveCompletedRecoveryCheck(emptyMap(), timestamp = RECOVERY_TIMESTAMP)
            fail("Expected Pro gate to block recovery check")
        } catch (error: IllegalStateException) {
            assertEquals("Hearing test requires dBcheck Pro", error.message)
        }
        coVerify(exactly = 0) { hearingRecoveryRepository.insertResult(any()) }
    }

    private fun createService(): HearingRecoveryService = HearingRecoveryService(
        context = testStringContext(),
        hearingTestRepository = hearingTestRepository,
        hearingRecoveryRepository = hearingRecoveryRepository,
        preferencesRepository = preferencesRepository,
    )

    private fun baselineResult() = testRecoveryBaselineResult(id = BASELINE_ID)

    private companion object {
        const val BASELINE_ID = 7L
        const val SAVED_RECOVERY_ID = 42L
        const val RECOVERY_TIMESTAMP = 2_000L
    }
}
