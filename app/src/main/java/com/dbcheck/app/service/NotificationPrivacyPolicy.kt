package com.dbcheck.app.service

import androidx.core.app.NotificationCompat

internal object NotificationPrivacyPolicy {
    fun measurementLockscreenVisibility(): Int = NotificationCompat.VISIBILITY_PRIVATE
}
