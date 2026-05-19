package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.HearingTestResultCalculator
import com.dbcheck.app.domain.hearingtest.TestKey
import com.dbcheck.app.sync.HealthConnectManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HearingTestService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val hearingTestRepository: HearingTestRepository,
        private val preferencesRepository: PreferencesRepository,
        private val healthConnectManager: HealthConnectManager,
    ) {
        suspend fun saveCompletedTest(
            thresholds: Map<TestKey, Float>,
            timestamp: Long = System.currentTimeMillis(),
        ): Long {
            val preferences = preferencesRepository.userPreferences.first()
            check(preferences.isProUser) { context.getString(R.string.hearing_test_pro_required) }

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
    }
