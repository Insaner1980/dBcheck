package com.dbcheck.app.ui.tinnitus

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.common.UiNumberFormatter

@Composable
internal fun tinnitusPitchFrequencyLabel(frequencyHz: Float): String = if (frequencyHz >= 1_000f) {
        stringResource(R.string.tinnitus_pitch_frequency_khz, UiNumberFormatter.oneDecimal(frequencyHz / 1_000f))
    } else {
        stringResource(R.string.tinnitus_pitch_frequency_hz, UiNumberFormatter.integer(frequencyHz))
    }
