package com.dbcheck.app.release

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PermissionDeviceQaMatrixTest {
    @Test
    fun permissionDeviceQaMatrixCoversManifestPermissionsAndRuntimeSurfaces() {
        val matrix = permissionDeviceQaMatrixFile()

        assertTrue("Osa 95 requires docs/qa/permission-device-qa-matrix.md", matrix.isFile)

        val content = matrix.readText()
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()

        expectedPermissions.forEach { permission ->
            assertTrue("Manifest must declare $permission", manifest.contains(permission))
            assertTrue("QA matrix must cover $permission", content.contains(permission))
        }

        forbiddenLocationPermissions.forEach { permission ->
            assertFalse("Manifest must not declare $permission", manifest.contains(permission))
            assertTrue("QA matrix must document the excluded $permission scope", content.contains(permission))
        }

        expectedRuntimeSurfaces.forEach { surface ->
            assertTrue("QA matrix must cover $surface", content.contains(surface))
        }
    }

    @Test
    fun permissionDeviceQaMatrixRecordsDeviceCoverageAndKnownManualGaps() {
        val content = permissionDeviceQaMatrixFile().readText()

        expectedApiRows.forEach { apiRow ->
            assertTrue("QA matrix must include $apiRow", content.contains(apiRow))
        }

        listOf(
            "adb devices",
            "emulator -list-avds",
            "Pixel_10",
            "Pixel_9_Pro",
            "Android 16 / API 36",
            "Device smoke: PARTIAL PASS",
            "Real microphone recording: PASS",
            "Foreground-service promotion: PASS",
            "Notification permission denial: PASS",
            "CameraX preview/photo/video: PASS",
            "Android Sharesheet grant: PASS",
            "Health Connect permission flow: PASS",
            "TalkBack navigation: PARTIAL",
            "16 KB compatibility: PASS",
            "Release risk",
            "Osa 96 - Billing production QA",
        ).forEach { expected ->
            assertTrue("QA matrix must record $expected", content.contains(expected))
        }
    }

    @Test
    fun coarseLocationPermissionIsRequestedOnlyFromTheUserLocationAction() {
        val settingsScreen =
            projectFile("src/main/java/com/dbcheck/app/ui/settings/SettingsPages.kt").readText()
        val dataExportSection =
            projectFile("src/main/java/com/dbcheck/app/ui/settings/components/DataExportSection.kt").readText()
        val coarsePermissionLaunch =
            "locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)"
        val productionSourceRoot =
            listOf(File("src/main/java"), File("app/src/main/java"))
                .first(File::isDirectory)
        val productionSources =
            productionSourceRoot
                .walkTopDown()
                .filter { file -> file.isFile && file.extension == "kt" }
                .joinToString(separator = "\n") { file -> file.readText() }

        assertTrue(settingsScreen.contains(coarsePermissionLaunch))
        assertEquals(1, productionSources.split(coarsePermissionLaunch).size - 1)
        assertTrue(settingsScreen.contains("onRequestLocationPermission"))
        assertTrue(dataExportSection.contains("onRequestLocationPermission"))
        assertFalse(settingsScreen.contains("Manifest.permission.ACCESS_FINE_LOCATION"))
        assertFalse(settingsScreen.contains("Manifest.permission.ACCESS_BACKGROUND_LOCATION"))
    }

    private fun permissionDeviceQaMatrixFile(): File = listOf(
        File("docs/qa/permission-device-qa-matrix.md"),
        File("..", "docs/qa/permission-device-qa-matrix.md"),
    ).firstOrNull(File::isFile) ?: File("docs/qa/permission-device-qa-matrix.md")

    private companion object {
        val expectedPermissions = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MICROPHONE",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
            "android.permission.VIBRATE",
            "com.android.vending.BILLING",
            "android.permission.health.WRITE_EXERCISE",
            "android.permission.health.READ_HEART_RATE",
        )

        val forbiddenLocationPermissions = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
        )

        val expectedRuntimeSurfaces = listOf(
            "Meter measurement",
            "Sleep recording",
            "Passive monitoring",
            "Ambient sound playback",
            "Camera overlay",
            "Session location metadata",
            "Health Connect noise sync",
            "Health Connect heart-rate overlay",
            "Lockscreen meter",
            "microphone",
            "mediaPlayback",
        )

        val expectedApiRows = listOf(
            "Android 11 / API 30",
            "Android 12 / API 31-32",
            "Android 13 / API 33",
            "Android 14 / API 34",
            "Android 15+ / API 35+",
        )
    }
}
