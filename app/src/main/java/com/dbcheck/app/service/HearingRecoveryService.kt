package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.R
import com.dbcheck.app.data.repository.HearingRecoveryRepository
import com.dbcheck.app.data.repository.HearingTestRepository
import com.dbcheck.app.data.repository.PreferencesRepository
import com.dbcheck.app.domain.hearingtest.HearingRecoveryCalculator
import com.dbcheck.app.domain.hearingtest.TestKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HearingRecoveryService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val hearingTestRepository: HearingTestRepository,
        private val hearingRecoveryRepository: HearingRecoveryRepository,
        private val preferencesRepository: PreferencesRepository,
    ) {
        suspend fun saveCompletedRecoveryCheck(
            thresholds: Map<TestKey, Float>,
            timestamp: Long = System.currentTimeMillis(),
        ): Long {
            preferencesRepository.requireProHearingTestPreferences(context)

            val baseline =
                checkNotNull(hearingTestRepository.getLatestResult().first()) {
                    context.getString(R.string.hearing_recovery_baseline_required)
                }
            val result =
                hearingRecoveryRepository.insertResult(
                    HearingRecoveryCalculator.build(
                        baseline = baseline,
                        thresholds = thresholds,
                        timestamp = timestamp,
                    ),
                )
            return result.id
        }
    }
