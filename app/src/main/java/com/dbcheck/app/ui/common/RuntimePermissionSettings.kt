package com.dbcheck.app.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun Context.openAppPermissionSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        },
    )
}

internal fun Context.openAppNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        },
    )
}
