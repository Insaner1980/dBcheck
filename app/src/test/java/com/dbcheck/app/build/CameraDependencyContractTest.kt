package com.dbcheck.app.build

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraDependencyContractTest {
    @Test
    fun cameraXUsesSingleStableVersionInVersionCatalog() {
        val catalog = projectFile("../gradle/libs.versions.toml").readText()

        assertTrue(catalog.contains("""cameraX = "1.6.1""""))
        expectedCameraArtifacts.forEach { artifact ->
            assertTrue(catalog.contains(artifact.catalogLine()))
        }
    }

    @Test
    fun appDeclaresCameraXDependenciesNeededByFutureOverlayPhases() {
        val buildGradle = projectFile("build.gradle.kts").readText()

        assertTrue(buildGradle.contains("implementation(libs.androidx.camera.core)"))
        assertTrue(buildGradle.contains("implementation(libs.androidx.camera.camera2)"))
        assertTrue(buildGradle.contains("implementation(libs.androidx.camera.lifecycle)"))
        assertTrue(buildGradle.contains("implementation(libs.androidx.camera.view)"))
        assertTrue(buildGradle.contains("implementation(libs.androidx.camera.video)"))
    }

    private data class CameraArtifact(
        val alias: String,
        val artifactId: String,
    ) {
        fun catalogLine(): String =
            "$alias = { group = \"androidx.camera\", name = \"$artifactId\", version.ref = \"cameraX\" }"
    }

    private companion object {
        val expectedCameraArtifacts = listOf(
            CameraArtifact("androidx-camera-core", "camera-core"),
            CameraArtifact("androidx-camera-camera2", "camera-camera2"),
            CameraArtifact("androidx-camera-lifecycle", "camera-lifecycle"),
            CameraArtifact("androidx-camera-view", "camera-view"),
            CameraArtifact("androidx-camera-video", "camera-video"),
        )
    }
}
