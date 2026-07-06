package com.dbcheck.app.ui.camera

import android.Manifest

internal data class CameraPermissionRequest(
    val permission: String,
    val status: CameraPermissionStatus,
    val shouldLaunchPermissionRequest: Boolean,
    val shouldOpenAppSettings: Boolean,
)

internal enum class CameraPermissionStatus {
    Granted,
    ShouldRequest,
    Denied,
    PermanentlyDenied,
}

internal object CameraPermissionPolicy {
    fun resolve(
        cameraGranted: Boolean,
        hasRequestedCameraPermission: Boolean,
        shouldShowCameraRationale: Boolean,
    ): CameraPermissionRequest {
        val status =
            when {
                cameraGranted -> CameraPermissionStatus.Granted
                !hasRequestedCameraPermission -> CameraPermissionStatus.ShouldRequest
                shouldShowCameraRationale -> CameraPermissionStatus.Denied
                else -> CameraPermissionStatus.PermanentlyDenied
            }

        return CameraPermissionRequest(
            permission = Manifest.permission.CAMERA,
            status = status,
            shouldLaunchPermissionRequest = status == CameraPermissionStatus.ShouldRequest,
            shouldOpenAppSettings = status == CameraPermissionStatus.PermanentlyDenied,
        )
    }
}
