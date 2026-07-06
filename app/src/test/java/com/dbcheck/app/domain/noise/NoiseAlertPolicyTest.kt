package com.dbcheck.app.domain.noise

import org.junit.Assert.assertEquals
import org.junit.Test

class NoiseAlertPolicyTest {
    @Test
    fun notificationPolicyOwnsDurationAndPeakThreshold() {
        assertEquals(30, NoiseAlertPolicy.EXPOSURE_DURATION_MINUTES)
        assertEquals(30L * 60_000L, NoiseAlertPolicy.EXPOSURE_DURATION_MS)
        assertEquals(120f, NoiseAlertPolicy.PEAK_WARNING_DB, 0.001f)
        assertEquals(100f, NoiseAlertPolicy.EXPOSURE_DOSE_ALERT_PERCENT, 0.001f)
        assertEquals(100f, NoiseAlertPolicy.PROJECTED_DOSE_ALERT_PERCENT, 0.001f)
        assertEquals(30, NoiseAlertPolicy.ALERT_RETRY_COOLDOWN_MINUTES)
        assertEquals(30L * 60_000L, NoiseAlertPolicy.ALERT_RETRY_COOLDOWN_MS)
    }
}
