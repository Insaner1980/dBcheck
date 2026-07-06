package com.dbcheck.app.ui.camera

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPermissionPolicyTest {
    @Test
    fun requestUsesAndroidCameraPermission() {
        val request = CameraPermissionPolicy.resolve(
            cameraGranted = false,
            hasRequestedCameraPermission = false,
            shouldShowCameraRationale = false,
        )

        assertEquals(Manifest.permission.CAMERA, request.permission)
    }

    @Test
    fun missingCameraPermissionBeforeFirstRequestLaunchesRuntimeRequest() {
        val request = CameraPermissionPolicy.resolve(
            cameraGranted = false,
            hasRequestedCameraPermission = false,
            shouldShowCameraRationale = false,
        )

        assertEquals(CameraPermissionStatus.ShouldRequest, request.status)
        assertTrue(request.shouldLaunchPermissionRequest)
        assertFalse(request.shouldOpenAppSettings)
    }

    @Test
    fun grantedCameraPermissionNeedsNoRuntimeRequest() {
        val request = CameraPermissionPolicy.resolve(
            cameraGranted = true,
            hasRequestedCameraPermission = true,
            shouldShowCameraRationale = false,
        )

        assertEquals(CameraPermissionStatus.Granted, request.status)
        assertFalse(request.shouldLaunchPermissionRequest)
        assertFalse(request.shouldOpenAppSettings)
    }

    @Test
    fun deniedCameraPermissionCanBeRetriedWhenAndroidShowsRationale() {
        val request = CameraPermissionPolicy.resolve(
            cameraGranted = false,
            hasRequestedCameraPermission = true,
            shouldShowCameraRationale = true,
        )

        assertEquals(CameraPermissionStatus.Denied, request.status)
        assertFalse(request.shouldLaunchPermissionRequest)
        assertFalse(request.shouldOpenAppSettings)
    }

    @Test
    fun deniedCameraPermissionOpensSettingsWhenAndroidHidesRationaleAfterRequest() {
        val request = CameraPermissionPolicy.resolve(
            cameraGranted = false,
            hasRequestedCameraPermission = true,
            shouldShowCameraRationale = false,
        )

        assertEquals(CameraPermissionStatus.PermanentlyDenied, request.status)
        assertFalse(request.shouldLaunchPermissionRequest)
        assertTrue(request.shouldOpenAppSettings)
    }
}
