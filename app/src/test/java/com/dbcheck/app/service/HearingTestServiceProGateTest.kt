package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.sync.HealthConnectManager
import com.dbcheck.app.testHearingResult
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class HearingTestServiceProGateTest {
    private val preferencesFlow = MutableStateFlow(UserPreferences(isProUser = true))
    private val preferencesRepository =
        mockk<PreferencesRepository> {
            every { userPreferences } returns preferencesFlow
        }
    private val hearingTestRepository =
        mockk<HearingTestRepository> {
            coEvery { insertResult(any()) } returns testHearingResult()
        }
    private val healthConnectManager = mockk<HealthConnectManager>(relaxed = true)

    @Test
    fun proUserSavesCompletedHearingTestAndReturnsSavedId() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true, healthConnectEnabled = false)
            val service = createService()

            val savedId = service.saveCompletedTest(thresholds(), timestamp = 1_700_000_123_000L)

            assertEquals(42L, savedId)
            coVerify {
                hearingTestRepository.insertResult(
                    match { result ->
                        result.timestamp == 1_700_000_123_000L &&
                            result.leftEarThresholds == listOf(1_000f to -30f) &&
                            result.rightEarThresholds == listOf(1_000f to -25f)
                    },
                )
            }
            coVerify(exactly = 0) { healthConnectManager.writeHearingTestResult(any()) }
        }

    @Test
    fun proUserHealthConnectEnabledWritesSavedHearingTestResult() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = true, healthConnectEnabled = true)
            val savedResult = testHearingResult()
            coEvery { hearingTestRepository.insertResult(any()) } returns savedResult
            val service = createService()

            val savedId = service.saveCompletedTest(thresholds(), timestamp = 1_700_000_123_000L)

            assertEquals(savedResult.id, savedId)
            coVerify { healthConnectManager.writeHearingTestResult(savedResult) }
        }

    @Test
    fun freeUserCannotSaveCompletedHearingTest() = runTest {
            preferencesFlow.value = UserPreferences(isProUser = false)
            val service = createService()

            try {
                service.saveCompletedTest(thresholds())
                fail("Expected Pro gate to block hearing test save")
            } catch (error: IllegalStateException) {
                assertEquals("Hearing test requires dBcheck Pro", error.message)
            }
            coVerify(exactly = 0) { hearingTestRepository.insertResult(any()) }
        }

    private fun createService(): HearingTestService = HearingTestService(
            context = testStringContext(),
            hearingTestRepository = hearingTestRepository,
            preferencesRepository = preferencesRepository,
            healthConnectManager = healthConnectManager,
        )

    private companion object {
        fun thresholds(): Map<TestKey, Float> = mapOf(
                TestKey(Ear.LEFT, 1_000f) to -30f,
                TestKey(Ear.RIGHT, 1_000f) to -25f,
            )
    }
}
