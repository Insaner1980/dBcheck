package com.dbcheck.app.ui.accessibility

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dbcheck.app.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PluralAccessibilityResourceTest {
    private val resources = ApplicationProvider.getApplicationContext<Application>().resources

    @Test
    fun heartRateChartDescriptionFormatsSingularAndPluralSamples() {
        assertEquals(
            "Heart rate chart. 1 sample. Latest 72 BPM, range 60 to 120 BPM.",
            resources.getQuantityString(
                R.plurals.heart_rate_chart_description,
                1,
                1,
                72,
                "60",
                "120",
            ),
        )
        assertEquals(
            "Heart rate chart. 2 samples. Latest 72 BPM, range 60 to 120 BPM.",
            resources.getQuantityString(
                R.plurals.heart_rate_chart_description,
                2,
                2,
                72,
                "60",
                "120",
            ),
        )
    }

    @Test
    fun monthlyTrendChartDescriptionFormatsSingularAndPluralDays() {
        assertEquals(
            "30-day weighted dB trend chart. Last 30 days. Rolling average 70.0 dB. " +
                "1 day with data, range 60.0 to 80.0 dB.",
            resources.getQuantityString(
                R.plurals.a11y_monthly_trend_chart_with_data,
                1,
                "Last 30 days",
                "70.0",
                1,
                "60.0",
                "80.0",
            ),
        )
        assertEquals(
            "30-day weighted dB trend chart. Last 30 days. Rolling average 70.0 dB. " +
                "2 days with data, range 60.0 to 80.0 dB.",
            resources.getQuantityString(
                R.plurals.a11y_monthly_trend_chart_with_data,
                2,
                "Last 30 days",
                "70.0",
                2,
                "60.0",
                "80.0",
            ),
        )
    }

    @Test
    fun spectralBarsDescriptionFormatsSingularAndPluralBands() {
        assertEquals(
            "Spectral analysis bars. Dominant frequency 1.0 kHz, bandwidth Wide, 1 frequency band.",
            resources.getQuantityString(
                R.plurals.a11y_spectral_analysis_bars_live,
                1,
                "1.0 kHz",
                "Wide",
                1,
            ),
        )
        assertEquals(
            "Spectral analysis bars. Dominant frequency 1.0 kHz, bandwidth Wide, 24 frequency bands.",
            resources.getQuantityString(
                R.plurals.a11y_spectral_analysis_bars_live,
                24,
                "1.0 kHz",
                "Wide",
                24,
            ),
        )
    }

    @Test
    fun timeSeriesChartDescriptionFormatsSingularAndPluralSamples() {
        assertEquals(
            "Time series chart. 1 sample over 1 min. LCeq 70.0 dB, minimum 60.0 dB, maximum 80.0 dB.",
            resources.getQuantityString(
                R.plurals.report_time_series_chart_description,
                1,
                1,
                "1 min",
                "70.0",
                "60.0",
                "80.0",
                "LCeq",
            ),
        )
        assertEquals(
            "Time series chart. 2 samples over 1 min. LCeq 70.0 dB, minimum 60.0 dB, maximum 80.0 dB.",
            resources.getQuantityString(
                R.plurals.report_time_series_chart_description,
                2,
                2,
                "1 min",
                "70.0",
                "60.0",
                "80.0",
                "LCeq",
            ),
        )
    }
}
