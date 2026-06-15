package com.dbcheck.app.data.local.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dbcheck.app.data.local.preferences.model.MeterRefreshRate
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.data.local.preferences.model.WaveformStyle
import com.dbcheck.app.domain.audio.ResponseTime
import com.dbcheck.app.domain.noise.DosimeterStandard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class UserPreferencesDataStoreMappingTest {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val notificationThresholdKey = intPreferencesKey("notification_threshold")
    private val micSensitivityOffsetKey = floatPreferencesKey("mic_sensitivity_offset")
    private val frequencyWeightingKey = stringPreferencesKey("frequency_weighting")
    private val responseTimeKey = stringPreferencesKey("response_time")
    private val dosimeterStandardKey = stringPreferencesKey("dosimeter_standard")
    private val waveformStyleKey = stringPreferencesKey("waveform_style")
    private val refreshRateKey = stringPreferencesKey("refresh_rate")
    private val soundDetectionKey = booleanPreferencesKey("sound_detection")
    private val soundDetectionPersistenceKey = booleanPreferencesKey("sound_detection_persistence")
    private val wavRecordingDefaultKey = booleanPreferencesKey("wav_recording_default")

    @Test
    fun readIOExceptionFallsBackToDefaultPreferences() = runTest {
        val preferences =
            flow<Preferences> {
                throw IOException("read failed")
            }.toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(UserPreferenceDefaults.THEME_MODE, preferences.themeMode)
        assertEquals(UserPreferenceDefaults.NOTIFICATION_THRESHOLD, preferences.notificationThreshold)
        assertEquals(UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET, preferences.micSensitivityOffset, 0f)
        assertEquals(UserPreferenceDefaults.FREQUENCY_WEIGHTING, preferences.frequencyWeighting)
        assertEquals(UserPreferenceDefaults.responseTime, preferences.responseTime)
        assertEquals(UserPreferenceDefaults.dosimeterStandard, preferences.dosimeterStandard)
        assertEquals(UserPreferenceDefaults.waveformStyle, preferences.waveformStyle)
        assertEquals(UserPreferenceDefaults.refreshRate, preferences.refreshRate)
        assertEquals(UserPreferenceDefaults.SOUND_DETECTION_ENABLED, preferences.soundDetectionEnabled)
        assertEquals(
            UserPreferenceDefaults.SOUND_DETECTION_PERSISTENCE_ENABLED,
            preferences.soundDetectionPersistenceEnabled,
        )
        assertEquals(UserPreferenceDefaults.WAV_RECORDING_DEFAULT_ENABLED, preferences.wavRecordingDefaultEnabled)
        assertFalse(preferences.isProUser)
    }

    @Test
    fun invalidStoredValuesAreNormalizedBeforeReachingCallers() = runTest {
        val preferences =
            flowOf(
                preferencesOf(
                    themeModeKey to "midnight",
                    notificationThresholdKey to 130,
                    micSensitivityOffsetKey to 25f,
                    frequencyWeightingKey to "Q",
                    responseTimeKey to "instant",
                    dosimeterStandardKey to "european",
                    waveformStyleKey to "sparkline",
                    refreshRateKey to "turbo",
                ),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(UserPreferenceDefaults.THEME_MODE, preferences.themeMode)
        assertEquals(UserPreferenceDefaults.NOTIFICATION_THRESHOLD_MAX, preferences.notificationThreshold)
        assertEquals(UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET_MAX, preferences.micSensitivityOffset, 0f)
        assertEquals(UserPreferenceDefaults.FREQUENCY_WEIGHTING, preferences.frequencyWeighting)
        assertEquals(ResponseTime.FAST, preferences.responseTime)
        assertEquals(DosimeterStandard.NIOSH_REL, preferences.dosimeterStandard)
        assertEquals(WaveformStyle.LINE, preferences.waveformStyle)
        assertEquals(MeterRefreshRate.STANDARD, preferences.refreshRate)
    }

    @Test
    fun storedResponseTimeIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(responseTimeKey to ResponseTime.SLOW.preferenceValue),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(ResponseTime.SLOW, preferences.responseTime)
    }

    @Test
    fun storedDosimeterStandardIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(dosimeterStandardKey to DosimeterStandard.OSHA_PEL.preferenceValue),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(DosimeterStandard.OSHA_PEL, preferences.dosimeterStandard)
    }

    @Test
    fun storedSoundDetectionToggleIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(soundDetectionKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.soundDetectionEnabled)
    }

    @Test
    fun storedSoundDetectionPersistenceOptInIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(soundDetectionPersistenceKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.soundDetectionPersistenceEnabled)
    }

    @Test
    fun storedWavRecordingDefaultOptInIsMappedIntoPreferences() = runTest {
        val preferences =
            flowOf(
                preferencesOf(wavRecordingDefaultKey to true),
            ).toUserPreferencesFlow(isDebugBuild = false)
                .first()

        assertEquals(true, preferences.wavRecordingDefaultEnabled)
    }
}
