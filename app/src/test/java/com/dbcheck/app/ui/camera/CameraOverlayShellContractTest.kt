package com.dbcheck.app.ui.camera

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraOverlayShellContractTest {
    @Test
    fun cameraOverlayScreenExposesStaticPreviewAndOverlaySlots() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()

        assertTrue(source.contains("fun CameraOverlayScreen("))
        assertTrue(source.contains("previewContent: @Composable BoxScope.() -> Unit"))
        assertTrue(source.contains("overlayContent: @Composable BoxScope.() -> Unit"))
        assertTrue(source.contains("CameraStaticPreview("))
    }

    @Test
    fun cameraOverlayScreenHandlesDeniedAndPermanentlyDeniedPermissionStates() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()

        assertTrue(source.contains("CameraPermissionStatus.Denied"))
        assertTrue(source.contains("CameraPermissionStatus.PermanentlyDenied"))
        assertTrue(source.contains("onRequestPermission"))
        assertTrue(source.contains("onOpenSettings"))
    }

    @Test
    fun cameraOverlayRouteCollectsLiveReadoutFromViewModel() {
        val routeSource = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()
        val viewModelSource =
            projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayViewModel.kt").readText()

        assertTrue(routeSource.contains("viewModel: CameraOverlayViewModel = hiltViewModel()"))
        assertTrue(routeSource.contains("viewModel.uiState.collectAsStateWithLifecycle()"))
        assertTrue(routeSource.contains("CameraOverlayReadout("))
        assertTrue(routeSource.contains("uiState = overlayUiState"))
        assertTrue(routeSource.contains("state = uiState"))
        assertTrue(viewModelSource.contains("audioEngine.decibelFlow.collect"))
        assertTrue(viewModelSource.contains("audioSessionManager.isRecording"))
        assertTrue(viewModelSource.contains("activeSessionStartTimeMs"))
    }

    @Test
    fun cameraOverlayRouteCapturesPhotoAndLaunchesShareIntent() {
        val routeSource = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()
        val viewModelSource =
            projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayViewModel.kt").readText()

        assertTrue(routeSource.contains("CameraCaptureButton("))
        assertTrue(routeSource.contains("ImageCapture.OutputFileOptions.Builder("))
        assertTrue(routeSource.contains("takePicture("))
        assertTrue(routeSource.contains("viewModel.onPhotoCaptured("))
        assertTrue(routeSource.contains("photoShareIntents = viewModel.photoShareIntents"))
        assertTrue(routeSource.contains("photoShareIntents.collect"))
        assertTrue(routeSource.contains("Intent.createChooser(intent, shareChooserTitle)"))
        assertFalse(viewModelSource.contains("suspend fun createPhotoCaptureFile"))
        assertTrue(viewModelSource.contains("fun onPhotoCaptureStarted()"))
        assertTrue(viewModelSource.contains("fun onPhotoCaptured("))
    }

    @Test
    fun cameraOverlayRouteRecordsSilentVideoAndShowsPrivacyCopy() {
        val routeSource = projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayRoute.kt").readText()
        val viewModelSource =
            projectFile("src/main/java/com/dbcheck/app/ui/camera/CameraOverlayViewModel.kt").readText()
        val stringsSource = projectFile("src/main/res/values/strings.xml").readText()

        assertTrue(routeSource.contains("CameraVideoCaptureButton("))
        assertTrue(routeSource.contains("startCameraOverlaySilentVideoCapture("))
        assertTrue(routeSource.contains("activeRecording?.stop()"))
        assertTrue(routeSource.contains("R.string.camera_overlay_video_privacy"))
        assertTrue(stringsSource.contains("Microphone audio is not saved"))
        assertFalse(viewModelSource.contains("suspend fun createSilentVideoFile"))
        assertTrue(viewModelSource.contains("fun onVideoRecordingStarted()"))
        assertTrue(viewModelSource.contains("fun onVideoRecordingFinished()"))
        assertTrue(viewModelSource.contains("fun onVideoRecordingFailed()"))
    }

    @Test
    fun screenshotPreviewsCoverGrantedDeniedAndPermanentlyDeniedShells() {
        val source = projectFile("src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt").readText()

        assertTrue(source.contains("fun CameraOverlayGrantedPreview()"))
        assertTrue(source.contains("fun CameraOverlayDeniedPreview()"))
        assertTrue(source.contains("fun CameraOverlayPermanentlyDeniedDarkPreview()"))
    }
}
