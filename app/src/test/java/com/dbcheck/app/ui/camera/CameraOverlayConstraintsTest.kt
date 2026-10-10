package com.dbcheck.app.ui.camera

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.assertTextFits
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28], qualifiers = "w800dp-h900dp-mdpi")
class CameraOverlayConstraintsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionSettingsAtShortCombinedConstraints() = verifyPermission(CameraPermissionStatus.PermanentlyDenied)

    @Test
    fun retryAtShortCombinedConstraints() = verifyPermission(CameraPermissionStatus.Denied)

    @Test
    fun captureErrorsAtShortCombinedConstraints() = verifyCaptureErrors(320, 2f)

    @Test
    fun captureErrorsInLandscape() = verifyCaptureErrors(800, 1f)

    private fun verifyCaptureErrors(widthDp: Int, fontScale: Float) {
        var photoLabel = ""
        var videoLabel = ""
        var photoError = ""
        var videoError = ""
        var closeLabel = ""
        var readyLabel = ""
        var photos = 0
        var videos = 0
        composeTestRule.setContent {
            photoLabel = stringResource(R.string.a11y_capture_camera_overlay_photo)
            videoLabel = stringResource(R.string.a11y_start_camera_overlay_video)
            photoError = stringResource(R.string.camera_overlay_capture_failed)
            videoError = stringResource(R.string.camera_overlay_video_failed)
            closeLabel = stringResource(R.string.a11y_close_camera_overlay)
            readyLabel = stringResource(R.string.camera_overlay_status_ready)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    CameraOverlayScreen(
                        permissionStatus = CameraPermissionStatus.Granted,
                        onClose = {},
                        onRequestPermission = {},
                        onOpenSettings = {},
                        modifier = Modifier.requiredSize(widthDp.dp, 360.dp),
                        overlayContent = {
                            CameraOverlayBottomBar(
                                state = CameraOverlayUiState(),
                                controlsState = CameraOverlayCaptureControlsState(
                                    photoEnabled = true,
                                    videoEnabled = true,
                                    isRecordingVideo = false,
                                    captureFailed = true,
                                    videoCaptureFailed = true,
                                ),
                                actions = CameraOverlayCaptureControlsActions(
                                    onPhotoCapture = { photos++ },
                                    onVideoToggle = { videos++ },
                                ),
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 12.dp, vertical = 24.dp),
                            )
                        },
                    )
                }
            }
        }
        val closeBounds = composeTestRule.onNodeWithContentDescription(closeLabel).fetchSemanticsNode().boundsInRoot
        val statusBounds =
            composeTestRule.onNodeWithText(readyLabel, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("Status hidden behind Close", statusBounds.top >= closeBounds.bottom)
        listOf(photoLabel, videoLabel).forEach { label ->
            val node =
                composeTestRule.onNodeWithContentDescription(label)
                    .performScrollTo().assertIsDisplayed().fetchSemanticsNode()
            org.junit.Assert.assertTrue("Capture target clipped: $label", node.boundsInRoot.height >= 48f)
            composeTestRule.onNodeWithContentDescription(label).performClick()
        }
        assertEquals(1, photos)
        assertEquals(1, videos)
        listOf(photoError, videoError).forEach { label ->
            composeTestRule.onNodeWithText(label, useUnmergedTree = true).assertTextFits(label)
        }
    }

    private fun verifyPermission(status: CameraPermissionStatus) {
        var actionLabel = ""
        var closeLabel = ""
        var actions = 0
        var closes = 0
        composeTestRule.setContent {
            actionLabel = stringResource(
                if (status == CameraPermissionStatus.PermanentlyDenied) {
                    R.string.action_open_settings
                } else {
                    R.string.action_try_again
                },
            )
            closeLabel = stringResource(R.string.a11y_close_camera_overlay)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DbCheckTheme {
                    CameraOverlayScreen(
                        permissionStatus = status,
                        onClose = { closes++ },
                        onRequestPermission = { actions++ },
                        onOpenSettings = { actions++ },
                        modifier = Modifier.requiredSize(320.dp, 360.dp),
                    )
                }
            }
        }
        composeTestRule.onNodeWithText(actionLabel).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.onNodeWithContentDescription(closeLabel).assertIsDisplayed().performClick()
        assertEquals(1, actions)
        assertEquals(1, closes)
    }
}
