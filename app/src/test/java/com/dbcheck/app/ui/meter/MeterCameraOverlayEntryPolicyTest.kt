package com.dbcheck.app.ui.meter

import org.junit.Assert.assertEquals
import org.junit.Test

class MeterCameraOverlayEntryPolicyTest {
    @Test
    fun proUserOpensCameraOverlay() {
        assertEquals(
            MeterCameraOverlayEntryDestination.CameraOverlay,
            MeterCameraOverlayEntryPolicy.destination(isProUser = true),
        )
    }

    @Test
    fun freeUserOpensUpgrade() {
        assertEquals(
            MeterCameraOverlayEntryDestination.Upgrade,
            MeterCameraOverlayEntryPolicy.destination(isProUser = false),
        )
    }
}
