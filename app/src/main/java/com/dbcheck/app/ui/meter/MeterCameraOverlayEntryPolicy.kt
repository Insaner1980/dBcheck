package com.dbcheck.app.ui.meter

internal enum class MeterCameraOverlayEntryDestination {
    CameraOverlay,
    Upgrade,
}

internal object MeterCameraOverlayEntryPolicy {
    fun destination(isProUser: Boolean): MeterCameraOverlayEntryDestination = if (isProUser) {
            MeterCameraOverlayEntryDestination.CameraOverlay
        } else {
            MeterCameraOverlayEntryDestination.Upgrade
        }
}
