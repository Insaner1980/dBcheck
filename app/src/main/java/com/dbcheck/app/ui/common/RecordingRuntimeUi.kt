package com.dbcheck.app.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

internal fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

@SuppressLint("InlinedApi")
internal fun Context.hasPostNotificationsPermission(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@SuppressLint("InlinedApi")
internal fun requestPostNotificationsPermissionIfNeeded(
    context: Context,
    launcher: ActivityResultLauncher<String>,
    notificationPermissionAlreadyRequested: Boolean = false,
    sdkInt: Int = Build.VERSION.SDK_INT,
) {
    if (
        PostNotificationPermissionPolicy.shouldRequestNotificationPermission(
            sdkInt = sdkInt,
            notificationPermissionGranted = context.hasPostNotificationsPermission(sdkInt),
            notificationPermissionAlreadyRequested = notificationPermissionAlreadyRequested,
        )
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

internal object PostNotificationPermissionPolicy {
    fun shouldRequestNotificationPermission(
        sdkInt: Int,
        notificationPermissionGranted: Boolean,
        notificationPermissionAlreadyRequested: Boolean,
    ): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        !notificationPermissionGranted &&
        !notificationPermissionAlreadyRequested
}

internal fun currentRecordingDurationMs(sessionStartTime: Long, fallbackDurationMs: Long): Long =
    if (sessionStartTime > 0L) {
        (System.currentTimeMillis() - sessionStartTime).coerceAtLeast(0L)
    } else {
        fallbackDurationMs
    }
