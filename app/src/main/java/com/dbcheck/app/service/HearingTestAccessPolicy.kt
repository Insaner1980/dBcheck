package com.dbcheck.app.service

import android.content.Context
import com.dbcheck.app.R
import com.dbcheck.app.data.local.preferences.model.UserPreferences
import com.dbcheck.app.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

internal suspend fun PreferencesRepository.requireProHearingTestPreferences(context: Context): UserPreferences {
    val preferences = userPreferences.first()
    check(preferences.isProUser) { context.getString(R.string.hearing_test_pro_required) }
    return preferences
}
