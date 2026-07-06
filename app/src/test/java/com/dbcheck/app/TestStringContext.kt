package com.dbcheck.app

import android.content.Context
import io.mockk.every
import io.mockk.mockk

internal fun testStringContext(): Context = mockk(relaxed = true) {
    stubBillingStrings()
    stubHealthConnectStrings()
    stubReportStrings()
    stubHearingAndHistoryStrings()
    stubAmbientStrings()
    stubMeterStrings()
    stubSettingsStrings()
}

private fun Context.stubBillingStrings() {
    every { getString(R.string.billing_google_play_unavailable) } returns "Google Play Billing is unavailable"
    every { getString(R.string.billing_pro_not_available) } returns "dBcheck Pro is not available"
    every { getString(R.string.billing_purchase_acknowledge_failed) } returns
        "Purchase could not be finalized. Try again from Google Play."
    every { getString(R.string.billing_purchase_failed) } returns "Purchase failed"
    every { getString(R.string.billing_pro_already_unlocked) } returns "dBcheck Pro already unlocked"
    every { getString(R.string.billing_pro_unlocked) } returns "dBcheck Pro unlocked"
    every { getString(R.string.billing_purchase_pending) } returns
        "Purchase pending. Complete payment in Google Play to unlock dBcheck Pro"
    every { getString(R.string.billing_start_purchase_failed) } returns "Unable to start purchase"
    every { getString(R.string.billing_unable_to_open_purchase_flow) } returns
        "Unable to open Google Play purchase flow"
}

private fun Context.stubHealthConnectStrings() {
    every { getString(R.string.health_connect_heart_rate_permission_required) } returns
        "Health Connect heart rate permission is required to show this overlay"
    every { getString(R.string.health_connect_heart_rate_read_failed) } returns
        "Unable to read Health Connect heart rate samples"
    every { getString(R.string.health_connect_hearing_unsupported, any<Long>()) } answers
        {
            val resultId = secondArg<Array<Any>>().single()
            "Health Connect has no supported audiometry record for hearing test $resultId"
        }
    every { getString(R.string.health_connect_noise_exposure_title) } returns "Noise exposure"
    every { getString(R.string.health_connect_noise_sync_permission_missing) } returns
        "Health Connect noise sync permission missing"
    every { getString(R.string.health_connect_session_incomplete) } returns "Session is not complete"
    every { getString(R.string.health_connect_status_check_failed) } returns
        "Unable to check Health Connect status"
    every { getString(R.string.health_connect_sync_failed) } returns "Health Connect write failed"
    every { getString(R.string.health_connect_unavailable) } returns "Unavailable on this device"
    every { getString(R.string.health_connect_unable_to_open) } returns "Unable to open Health Connect"
    every { getString(R.string.health_connect_unavailable_on_device) } returns
        "Health Connect is unavailable on this device"
}

private fun Context.stubReportStrings() {
    every { getString(R.string.report_metric_laeq) } returns "LAeq"
    every { getString(R.string.report_metric_lcpeak) } returns "LCpeak"
    every { getString(R.string.report_metric_max) } returns "Max"
    every { getString(R.string.report_metric_location) } returns "Location"
    every { getString(R.string.report_metric_dosimeter_standard) } returns "Dosimeter standard"
    every { getString(R.string.report_metric_octave_breakdown) } returns "Octave breakdown"
    every { getString(R.string.report_metric_peak) } returns "Peak"
    every { getString(R.string.report_metric_projected_dose) } returns "Projected dose"
    every { getString(R.string.report_metric_sleep_keep_awake) } returns "Sleep keep awake"
    every { getString(R.string.report_metric_sleep_loud_periods) } returns "Sleep loud periods"
    every { getString(R.string.report_metric_sleep_peak_events) } returns "Sleep peak events"
    every { getString(R.string.report_metric_sleep_recorded) } returns "Sleep recorded"
    every { getString(R.string.report_metric_sleep_target) } returns "Sleep target"
    every { getString(R.string.report_metric_sound_type) } returns "Sound type"
    every { getString(R.string.report_metric_weighting) } returns "Weighting"
    every { getString(R.string.report_disclaimer) } returns
        "dBcheck is not a calibrated Class 1/2 sound level meter."
    every { getString(R.string.report_metric_app_version) } returns "App version"
    every { getString(R.string.report_metric_audio_input) } returns "Audio input"
    every { getString(R.string.report_metric_calibration_offset) } returns "Calibration offset"
    every { getString(R.string.report_metric_device) } returns "Device"
    every { getString(R.string.report_metric_response_time) } returns "Response time"
    every { getString(R.string.report_metric_response_time_mixed) } returns "Mixed"
    every { getString(R.string.report_calibration_offset_current, any()) } answers {
        val offset = secondArg<Array<Any>>().single()
        "$offset dB (current setting)"
    }
    every { getString(R.string.report_location_with_accuracy, any(), any(), any()) } answers {
        val args = secondArg<Array<Any>>()
        "${args[0]}, ${args[1]} (accuracy ${args[2]} m)"
    }
    every { getString(R.string.report_location_without_accuracy, any(), any()) } answers {
        val args = secondArg<Array<Any>>()
        "${args[0]}, ${args[1]}"
    }
    every { getString(R.string.report_sound_type_value, any(), any()) } answers {
        val args = secondArg<Array<Any>>()
        "${args[0]} (${args[1]}%)"
    }
    every { getString(R.string.report_upstream_unavailable_note) } returns
        "N/A means the source data was not captured for this session."
    every { getString(R.string.value_unavailable) } returns "N/A"
    every { getString(R.string.value_disabled) } returns "Disabled"
    every { getString(R.string.value_enabled) } returns "Enabled"
    every { getString(R.string.value_available) } returns "Available"
    every { getString(R.string.response_time_fast) } returns "Fast"
    every { getString(R.string.response_time_impulse) } returns "Impulse"
    every { getString(R.string.response_time_slow) } returns "Slow"
    every { getString(R.string.weighting_a) } returns "A-Weight"
    every { getString(R.string.weighting_b) } returns "B-Weight"
    every { getString(R.string.weighting_c) } returns "C-Weight"
    every { getString(R.string.weighting_z) } returns "Z-Weight"
    every { getString(R.string.weighting_itu_r_468) } returns "ITU-R 468"
    every { getString(R.string.meter_dosimeter_standard_niosh_rel) } returns "NIOSH REL"
    every { getString(R.string.meter_dosimeter_standard_osha_pel) } returns "OSHA PEL"
    every { getString(R.string.report_pdf_exported) } returns "PDF report exported"
    every { getString(R.string.report_pdf_failed) } returns "PDF export failed"
    every { getString(R.string.report_pdf_pro_required) } returns "PDF export requires dBcheck Pro"
    every { getString(R.string.report_session_load_failed) } returns "Unable to load session"
    every { getString(R.string.report_session_not_found_error) } returns "Session not found"
    every { getString(R.string.report_share_error_failed) } returns "Unable to share session"
    every { getString(R.string.report_share_error_no_app) } returns "No app available to share session"
    every { getString(R.string.report_session_updated) } returns "Session updated"
    every { getString(R.string.report_wav_delete_failed) } returns "Unable to delete WAV recording"
    every { getString(R.string.report_wav_deleted) } returns "WAV recording deleted"
    every { getString(R.string.report_wav_export_requires_pro) } returns "WAV export requires dBcheck Pro"
    every { getString(R.string.report_wav_not_available) } returns "No WAV recording for this session"
    every { getString(R.string.report_wav_share_failed) } returns "Unable to share WAV recording"
    every { getString(R.string.report_wav_share_no_app) } returns "No app available to share WAV recording"
}

private fun Context.stubHearingAndHistoryStrings() {
    every { getString(R.string.analytics_error_unable_to_load) } returns "Unable to load analytics"
    every { getString(R.string.sound_detection_error_unavailable) } returns "Sound detection unavailable"
    every { getString(R.string.hearing_error_load_failed) } returns "Unable to load hearing test result"
    every { getString(R.string.hearing_error_no_result_to_share) } returns "No hearing test result to share"
    every { getString(R.string.hearing_error_no_share_app) } returns "No app available to share results"
    every { getString(R.string.hearing_error_result_loading) } returns "Hearing test result is still loading"
    every { getString(R.string.hearing_error_save_failed) } returns "Unable to save hearing test result"
    every { getString(R.string.hearing_error_share_failed) } returns "Unable to share hearing test results"
    every { getString(R.string.hearing_error_tone_playback_failed) } returns "Unable to play hearing test tone"
    every { getString(R.string.hearing_recovery_baseline_required) } returns
        "Take a full hearing test before using short recovery checks"
    every { getString(R.string.hearing_test_pro_required) } returns "Hearing test requires dBcheck Pro"
    every { getString(R.string.tinnitus_pitch_description) } returns
        "Create a personal tracking pitch profile with a short tone preview for each ear."
    every { getString(R.string.tinnitus_pitch_disclaimer) } returns
        "For personal tracking only. This is not a medical test."
    every { getString(R.string.tinnitus_pitch_playback_failed) } returns "Unable to play pitch preview"
    every { getString(R.string.tinnitus_pitch_pro_required) } returns
        "Tinnitus pitch profile requires dBcheck Pro"
    every { getString(R.string.tinnitus_pitch_save_failed) } returns "Unable to save pitch profile"
    every { getString(R.string.tinnitus_pitch_saved) } returns "Pitch profile saved"
    every { getString(R.string.tinnitus_pitch_title) } returns "Personal tracking pitch profile"
    every { getString(R.string.history_error_unable_to_load) } returns "Unable to load history"
    every { getString(R.string.history_trend_similar_to_last_week) } returns "Similar to last week"
    every { getString(R.string.history_trend_stable) } returns "Stable"
    every { getString(R.string.session_name_pro_required) } returns "Session naming requires dBcheck Pro"
    every { getString(R.string.session_name_unable_to_update) } returns "Unable to update session"
    every { getString(R.string.session_unlimited_history_requires_pro) } returns
        "Unlimited history requires dBcheck Pro"
}

private fun Context.stubAmbientStrings() {
    every { getString(R.string.ambient_sound_description) } returns
        "Choose a locally generated ambient sound, volume, and optional stop timer."
    every { getString(R.string.ambient_sound_notification_required) } returns
        "Notifications are required for ambient sound playback"
    every { getString(R.string.ambient_sound_pro_required) } returns
        "Ambient sound playback requires dBcheck Pro"
    every { getString(R.string.ambient_sound_title) } returns "Ambient sound"
}

private fun Context.stubMeterStrings() {
    every { getString(R.string.meter_recording_error_microphone_required) } returns
        "Microphone access is required to measure sound levels"
    every { getString(R.string.meter_recording_error_read_failed) } returns "Measurement stopped unexpectedly"
    every { getString(R.string.meter_recording_error_start_failed) } returns "Unable to start measurement"
    every { getString(R.string.meter_recording_error_storage_failed) } returns
        "Measurement stopped because data could not be saved"
    every { getString(R.string.meter_share_error_failed) } returns "Unable to share meter results"
    every { getString(R.string.meter_share_error_no_app) } returns "No app available to share results"
    every { getString(R.string.meter_share_error_not_ready) } returns "Start measuring before sharing results"
    every { getString(R.string.meter_start_background_failed) } returns "Unable to start background measurement"
    every { getString(R.string.meter_stop_background_failed) } returns "Unable to stop measurement"
}

private fun Context.stubSettingsStrings() {
    every { getString(R.string.settings_backup_created) } returns "Backup created"
    every { getString(R.string.settings_backup_failed) } returns "Backup failed"
    every { getString(R.string.settings_backup_invalid_database) } returns
        "Backup file is not a valid dBcheck database"
    every { getString(R.string.settings_backup_not_found) } returns "Backup file not found"
    every { getString(R.string.settings_backup_not_managed) } returns "Backup file is not managed by dBcheck"
    every { getString(R.string.settings_backup_not_db_file) } returns
        "Backup file is not a dBcheck database backup"
    every { getString(R.string.settings_backup_restore_failed) } returns "Restore failed"
    every { getString(R.string.settings_backup_restored) } returns "Backup restored"
    every { getString(R.string.settings_backup_stop_recording) } returns
        "Stop recording before managing backups"
    every { getString(R.string.settings_backup_unable_to_load) } returns "Unable to load backups"
    every { getString(R.string.settings_clear_history_done) } returns "History cleared"
    every { getString(R.string.settings_clear_history_failed) } returns "Unable to clear history"
    every { getString(R.string.settings_clear_history_stop_recording) } returns
        "Stop recording before clearing history"
    every { getString(R.string.settings_calibration_profile_default_name) } returns "Device default"
    every { getString(R.string.settings_calibration_profile_last_default_error) } returns
        "Keep one default calibration profile"
    every { getString(R.string.settings_csv_export_failed) } returns "CSV export failed"
    every { getString(R.string.settings_csv_export_ready) } returns "CSV export ready"
    every { getString(R.string.settings_csv_export_requires_pro) } returns "CSV export requires dBcheck Pro"
    every { getString(R.string.settings_csv_no_export_app) } returns "No app available to export CSV"
    every { getString(R.string.share_csv_clip_label) } returns "dBcheck CSV export"
}
