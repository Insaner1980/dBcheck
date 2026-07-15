package com.dbcheck.app.ui.camera

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraXPreviewBindingContractTest {
    @Test
    fun cameraPreviewUsesPreviewViewAndLifecycleBinding() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()

        assertTrue(source.contains("fun CameraXPreviewContent("))
        assertTrue(source.contains("AndroidView("))
        assertTrue(source.contains("PreviewView("))
        assertTrue(source.contains("ProcessCameraProvider.getInstance("))
        assertTrue(source.contains("Preview.Builder().build()"))
        assertTrue(source.contains("CameraSelector.DEFAULT_BACK_CAMERA"))
        assertTrue(source.contains("bindToLifecycle("))
        assertTrue(source.contains("preview?.let { cameraProvider.unbind(it) }"))
        assertTrue(source.contains("imageCapture?.let { cameraProvider.unbind(it) }"))
        assertTrue(source.contains("videoCapture?.let { cameraProvider.unbind(it) }"))
    }

    @Test
    fun cameraPreviewBindsImageCaptureUseCaseForPhotoCapture() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()

        assertTrue(source.contains("onImageCaptureReady: (ImageCapture?) -> Unit"))
        assertTrue(source.contains("ImageCapture.Builder().build()"))
        assertTrue(source.contains("bindToLifecycle("))
        assertTrue(source.contains("imageCapture"))
        assertTrue(source.contains("currentOnImageCaptureReady(imageCapture)"))
        assertTrue(source.contains("currentOnImageCaptureReady(null)"))
    }

    @Test
    fun cameraPreviewBindsSilentVideoCaptureUseCaseWithoutAudio() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()

        assertTrue(source.contains("onVideoCaptureReady: (VideoCapture<Recorder>?) -> Unit"))
        assertTrue(source.contains("Recorder.Builder()"))
        assertTrue(source.contains("VideoCapture.withOutput(recorder)"))
        assertTrue(source.contains("FileOutputOptions.Builder("))
        assertTrue(source.contains("prepareRecording(context, outputOptions)"))
        assertTrue(source.contains(".start("))
        assertTrue(source.contains("VideoRecordEvent.Finalize"))
        assertTrue(source.contains("videoCapture"))
        assertTrue(source.contains("currentOnVideoCaptureReady(videoCapture)"))
        assertTrue(source.contains("currentOnVideoCaptureReady(null)"))
        assertTrue(!source.contains("withAudioEnabled("))
        assertTrue(!source.contains("Manifest.permission.RECORD_AUDIO"))
    }

    @Test
    fun routeRequestsCameraPermissionBeforeBindingPreview() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()

        assertTrue(source.contains("rememberLauncherForActivityResult"))
        assertTrue(source.contains("ActivityResultContracts.RequestPermission()"))
        assertTrue(source.contains("CameraPermissionPolicy.resolve"))
        assertTrue(source.contains("shouldShowRequestPermissionRationale"))
        assertTrue(source.contains("hasCameraPermission("))
    }

    @Test
    fun cameraUnavailableHasFallbackUiAndScreenshotPreview() {
        val routeSource = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()
        val screenshotSource =
            projectFile("src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt").readText()

        assertTrue(routeSource.contains("CameraPreviewUnavailableContent("))
        assertTrue(routeSource.contains("onPreviewUnavailable"))
        assertTrue(routeSource.contains("R.string.camera_overlay_unavailable_title"))
        assertTrue(screenshotSource.contains("fun CameraOverlayUnavailablePreview()"))
    }
}
