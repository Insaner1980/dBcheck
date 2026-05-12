package com.dbcheck.app.service

import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.HearingTestResultCalculator
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.sync.HealthConnectManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HearingTestService
    @Inject
    constructor(
        private val hearingTestRepository: HearingTestRepository,
        private val preferencesRepository: PreferencesRepository,
        private val healthConnectManager: HealthConnectManager,
    ) {
        suspend fun saveCompletedTest(
            thresholds: Map<TestKey, Float>,
            timestamp: Long = System.currentTimeMillis(),
        ): Long {
            val preferences = preferencesRepository.userPreferences.first()
            check(preferences.isProUser) { PRO_REQUIRED_MESSAGE }

            val result =
                hearingTestRepository.insertResult(
                    HearingTestResultCalculator.build(
                        thresholds = thresholds,
                        timestamp = timestamp,
                    ),
                )
            if (preferences.healthConnectEnabled) {
                healthConnectManager.writeHearingTestResult(result)
            }
            return result.id
        }

        private companion object {
            const val PRO_REQUIRED_MESSAGE = "Hearing test requires dBcheck Pro"
        }
    }
