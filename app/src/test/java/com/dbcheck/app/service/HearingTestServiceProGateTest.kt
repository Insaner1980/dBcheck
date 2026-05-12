package com.dbcheck.app.service

import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.Ear
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.sync.HealthConnectManager
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
            coEvery { insertResult(any()) } returns hearingResult()
        }
    private val healthConnectManager = mockk<HealthConnectManager>(relaxed = true)

    @Test
    fun freeUserCannotSaveCompletedHearingTest() =
        runTest {
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

    private fun createService(): HearingTestService =
        HearingTestService(
            hearingTestRepository = hearingTestRepository,
            preferencesRepository = preferencesRepository,
            healthConnectManager = healthConnectManager,
        )

    private companion object {
        fun thresholds(): Map<TestKey, Float> =
            mapOf(
                TestKey(Ear.LEFT, 1_000f) to -30f,
                TestKey(Ear.RIGHT, 1_000f) to -25f,
            )

        fun hearingResult() =
            HearingTestResult(
                id = 42L,
                timestamp = 1_700_000_000_000L,
                overallScore = 86,
                rating = "Good",
                leftEarThresholds = listOf(1_000f to -30f),
                rightEarThresholds = listOf(1_000f to -25f),
                speechClarity = 84f,
                highFreqLimit = 16_000f,
                avgThreshold = -27.5f,
            )
    }
}
