package com.dbcheck.app.service

import androidx.core.app.NotificationCompat

internal object NotificationPrivacyPolicy {
    fun measurementLockscreenVisibility(
        isProUser: Boolean,
        lockscreenMeterEnabled: Boolean,
        showLockscreenMeterPublicly: Boolean,
    ): Int = if (isProUser && lockscreenMeterEnabled && showLockscreenMeterPublicly) {
        NotificationCompat.VISIBILITY_PUBLIC
    } else {
        NotificationCompat.VISIBILITY_PRIVATE
    }
}
