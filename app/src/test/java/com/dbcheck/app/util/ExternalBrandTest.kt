package com.dbcheck.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalBrandTest {
    @Test
    fun noiseLevelColorsMapToExternalBrandArgbValues() {
        assertEquals(Color(0xFF8EA58E).toArgb(), ExternalBrand.noiseLevelArgb(NoiseLevel.QUIET))
        assertEquals(Color(0xFF5C6060).toArgb(), ExternalBrand.noiseLevelArgb(NoiseLevel.NORMAL))
        assertEquals(Color(0xFFC9A24D).toArgb(), ExternalBrand.noiseLevelArgb(NoiseLevel.ELEVATED))
        assertEquals(Color(0xFFE07A7A).toArgb(), ExternalBrand.noiseLevelArgb(NoiseLevel.DANGEROUS))
    }

    @Test
    fun shareSurfacesUseExternalBrandFontsMarginsAndWordmark() {
        val shareSource = projectFile("src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt").readText()
        val cameraSource =
            projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt").readText()

        assertTrue(shareSource.contains("ExternalBrand.SHARE_CARD_MARGIN_PX"))
        assertTrue(shareSource.contains("ExternalBrand.WORDMARK"))
        assertTrue(shareSource.contains("ExternalBrand.manropePaint"))
        assertTrue(shareSource.contains("ExternalBrand.spaceGroteskPaint"))
        assertFalse(shareSource.contains("Typeface.create(\"sans-serif"))
        assertTrue(cameraSource.contains("ExternalBrand.WORDMARK"))
        assertTrue(cameraSource.contains("ExternalBrand.SHARE_CARD_PANEL_RADIUS_PX"))
        assertTrue(cameraSource.contains("ExternalBrand.manropePaint"))
        assertTrue(cameraSource.contains("ExternalBrand.spaceGroteskPaint"))
    }

    @Test
    fun widgetAndNotificationUseExternalNoiseLevelColors() {
        val widgetSource = projectFile("src/main/java/com/dbcheck/app/widget/DbCheckWidget.kt").readText()
        val notificationSource = projectFile("src/main/java/com/dbcheck/app/service/NotificationHelper.kt").readText()

        assertTrue(widgetSource.contains("widgetNoiseLevelColor(noiseLevel)"))
        assertTrue(widgetSource.contains("ColorProvider(ExternalBrand.noiseLevelColor(level))"))
        assertTrue(widgetSource.contains("ImageProvider(R.drawable.ic_widget_lock)"))
        assertTrue(notificationSource.contains("setTextColor(R.id.notification_noise_label, noiseLevel.textColor)"))
        assertTrue(notificationSource.contains("ExternalBrand.noiseLevelArgb(noiseLevel)"))
    }
}
