package com.dbcheck.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.ui.common.findActivity
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.theme.DbCheckTheme
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

@Suppress("LongMethod")
@Composable
fun CameraOverlayRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraOverlayViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shareChooserTitle = stringResource(R.string.camera_overlay_share_chooser)
    var hasRequestedCameraPermission by rememberSaveable { mutableStateOf(false) }
    var permissionStatus by remember {
        mutableStateOf(
            cameraPermissionRequest(
                context = context,
                activity = activity,
                hasRequestedCameraPermission = hasRequestedCameraPermission,
            ).status,
        )
    }
    var previewUnavailable by rememberSaveable { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) {
            hasRequestedCameraPermission = true
            previewUnavailable = false
            permissionStatus =
                cameraPermissionRequest(
                    context = context,
                    activity = activity,
                    hasRequestedCameraPermission = true,
                ).status
        }

    CameraOverlayPermissionEffects(
        state =
            CameraOverlayPermissionEffectsState(
                context = context,
                activity = activity,
                lifecycleOwner = lifecycleOwner,
                hasRequestedCameraPermission = hasRequestedCameraPermission,
            ),
        actions =
            CameraOverlayPermissionEffectsActions(
                onHasRequestedCameraPermissionChange = {
                    hasRequestedCameraPermission = it
                },
                onPermissionStatusChange = {
                    permissionStatus = it
                },
                onLaunchPermissionRequest = permissionLauncher::launch,
                onPreviewUnavailableChange = {
                    previewUnavailable = it
                },
            ),
    )
    CameraOverlayPhotoShareEffect(
        context = context,
        shareChooserTitle = shareChooserTitle,
        photoShareIntents = viewModel.photoShareIntents,
        onPhotoCaptureError = viewModel::onPhotoCaptureFailed,
    )
    val captureRefs =
        CameraOverlayCaptureRefs(
            imageCapture = imageCapture,
            videoCapture = videoCapture,
            activeRecording = activeRecording,
        )
    val captureUpdates =
        CameraOverlayCaptureUpdates(
            onImageCaptureChange = {
                imageCapture = it
            },
            onVideoCaptureChange = {
                videoCapture = it
            },
            onActiveRecordingChange = {
                activeRecording = it
            },
            onPreviewUnavailableChange = {
                previewUnavailable = it
            },
        )
    val captureHostActions =
        CameraOverlayCaptureHostActions(
            onPhotoCapture = { capture ->
                startCameraOverlayPhotoCapture(
                    imageCapture = capture,
                    context = context,
                    viewModel = viewModel,
                )
            },
            onVideoToggle = { captures, onActiveRecordingChange ->
                toggleCameraOverlaySilentVideoCapture(
                    captures = captures,
                    context = context,
                    viewModel = viewModel,
                    onActiveRecordingChange = onActiveRecordingChange,
                )
            },
        )

    CameraOverlayScreen(
        permissionStatus = permissionStatus,
        onClose = onBack,
        onRequestPermission = {
            hasRequestedCameraPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onOpenSettings = {
            context.openAppSettings()
        },
        modifier = modifier,
        previewContent = {
            CameraOverlayPreviewContent(
                previewUnavailable = previewUnavailable,
                lifecycleOwner = lifecycleOwner,
                captures = captureRefs,
                updates = captureUpdates,
            )
        },
        overlayContent = {
            CameraOverlayInteractiveContent(
                previewUnavailable = previewUnavailable,
                uiState = overlayUiState,
                captures = captureRefs,
                updates = captureUpdates,
                actions = captureHostActions,
            )
        },
    )
}

private data class CameraOverlayPermissionEffectsState(
    val context: Context,
    val activity: Activity?,
    val lifecycleOwner: LifecycleOwner,
    val hasRequestedCameraPermission: Boolean,
)

private data class CameraOverlayPermissionEffectsActions(
    val onHasRequestedCameraPermissionChange: (Boolean) -> Unit,
    val onPermissionStatusChange: (CameraPermissionStatus) -> Unit,
    val onLaunchPermissionRequest: (String) -> Unit,
    val onPreviewUnavailableChange: (Boolean) -> Unit,
)

private data class CameraOverlayCaptureRefs(
    val imageCapture: ImageCapture?,
    val videoCapture: VideoCapture<Recorder>?,
    val activeRecording: Recording?,
)

private data class CameraOverlayCaptureUpdates(
    val onImageCaptureChange: (ImageCapture?) -> Unit,
    val onVideoCaptureChange: (VideoCapture<Recorder>?) -> Unit,
    val onActiveRecordingChange: (Recording?) -> Unit,
    val onPreviewUnavailableChange: (Boolean) -> Unit,
)

private data class CameraOverlayCaptureHostActions(
    val onPhotoCapture: (ImageCapture?) -> Unit,
    val onVideoToggle: (CameraOverlayCaptureRefs, (Recording?) -> Unit) -> Unit,
)

@Composable
private fun CameraOverlayPermissionEffects(
    state: CameraOverlayPermissionEffectsState,
    actions: CameraOverlayPermissionEffectsActions,
) {
    LaunchedEffect(state.context, state.activity, state.hasRequestedCameraPermission) {
        val request =
            cameraPermissionRequest(
                context = state.context,
                activity = state.activity,
                hasRequestedCameraPermission = state.hasRequestedCameraPermission,
            )
        actions.onPermissionStatusChange(request.status)
        if (request.shouldLaunchPermissionRequest) {
            actions.onHasRequestedCameraPermissionChange(true)
            actions.onLaunchPermissionRequest(request.permission)
        }
    }

    DisposableEffect(state.lifecycleOwner, state.context, state.activity, state.hasRequestedCameraPermission) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val permissionStatus =
                        cameraPermissionRequest(
                            context = state.context,
                            activity = state.activity,
                            hasRequestedCameraPermission = state.hasRequestedCameraPermission,
                        ).status
                    actions.onPermissionStatusChange(permissionStatus)
                    if (permissionStatus == CameraPermissionStatus.Granted) {
                        actions.onPreviewUnavailableChange(false)
                    }
                }
            }
        state.lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            state.lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun CameraOverlayPhotoShareEffect(
    context: Context,
    shareChooserTitle: String,
    photoShareIntents: Flow<Intent>,
    onPhotoCaptureError: () -> Unit,
) {
    val currentOnPhotoCaptureError by rememberUpdatedState(onPhotoCaptureError)

    LaunchedEffect(shareChooserTitle, photoShareIntents) {
        photoShareIntents.collect { intent ->
            runCatching {
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            }.onFailure {
                currentOnPhotoCaptureError()
            }
        }
    }
}

@Composable
private fun CameraOverlayPreviewContent(
    previewUnavailable: Boolean,
    lifecycleOwner: LifecycleOwner,
    captures: CameraOverlayCaptureRefs,
    updates: CameraOverlayCaptureUpdates,
) {
    if (previewUnavailable) {
        CameraPreviewUnavailableContent()
    } else {
        CameraXPreviewContent(
            lifecycleOwner = lifecycleOwner,
            onPreviewUnavailable = {
                updates.onImageCaptureChange(null)
                updates.onVideoCaptureChange(null)
                captures.activeRecording?.stop()
                updates.onActiveRecordingChange(null)
                updates.onPreviewUnavailableChange(true)
            },
            onImageCaptureReady = updates.onImageCaptureChange,
            onVideoCaptureReady = { capture ->
                if (capture == null) {
                    captures.activeRecording?.stop()
                    updates.onActiveRecordingChange(null)
                }
                updates.onVideoCaptureChange(capture)
            },
        )
    }
}

@Composable
private fun BoxScope.CameraOverlayInteractiveContent(
    previewUnavailable: Boolean,
    uiState: CameraOverlayUiState,
    captures: CameraOverlayCaptureRefs,
    updates: CameraOverlayCaptureUpdates,
    actions: CameraOverlayCaptureHostActions,
) {
    if (previewUnavailable) return

    CameraOverlayReadout(modifier = Modifier.align(Alignment.BottomStart), state = uiState)
    CameraOverlayCaptureControls(
        state =
            CameraOverlayCaptureControlsState(
                photoEnabled =
                    captures.imageCapture != null &&
                        !uiState.isCapturingPhoto &&
                        !uiState.isRecordingVideo,
                videoEnabled = captures.videoCapture != null && !uiState.isCapturingPhoto,
                isRecordingVideo = uiState.isRecordingVideo,
                captureFailed = uiState.captureFailed,
                videoCaptureFailed = uiState.videoCaptureFailed,
            ),
        actions =
            CameraOverlayCaptureControlsActions(
                onPhotoCapture = {
                    actions.onPhotoCapture(captures.imageCapture)
                },
                onVideoToggle = {
                    actions.onVideoToggle(captures, updates.onActiveRecordingChange)
                },
            ),
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(DbCheckTheme.spacing.space6),
    )
}

private fun toggleCameraOverlaySilentVideoCapture(
    captures: CameraOverlayCaptureRefs,
    context: Context,
    viewModel: CameraOverlayViewModel,
    onActiveRecordingChange: (Recording?) -> Unit,
) {
    val activeRecording = captures.activeRecording
    if (activeRecording != null) {
        activeRecording.stop()
        return
    }

    onActiveRecordingChange(
        startCameraOverlaySilentVideoCapture(
            videoCapture = captures.videoCapture,
            context = context,
            viewModel = viewModel,
            onRecordingFinalized = {
                onActiveRecordingChange(null)
            },
        ),
    )
}

@Composable
internal fun CameraOverlayScreen(
    permissionStatus: CameraPermissionStatus,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable BoxScope.() -> Unit = { CameraStaticPreview() },
    overlayContent: @Composable BoxScope.() -> Unit = {
        CameraOverlayReadout(modifier = Modifier.align(Alignment.BottomStart))
    },
) {
    val colors = DbCheckTheme.colorScheme

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.material.background),
    ) {
        when (permissionStatus) {
            CameraPermissionStatus.Granted -> {
                previewContent()
                overlayContent()
            }

            CameraPermissionStatus.ShouldRequest,
            CameraPermissionStatus.Denied,
            CameraPermissionStatus.PermanentlyDenied,
            -> CameraPermissionDeniedContent(
                permissionStatus = permissionStatus,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(DbCheckTheme.spacing.space6),
            )
        }

        CameraOverlayCloseButton(
            onClose = onClose,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(DbCheckTheme.spacing.space3),
        )
    }
}

@Composable
internal fun CameraStaticPreview(modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val previewDescription = stringResource(R.string.a11y_camera_overlay_static_preview)

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { contentDescription = previewDescription },
    ) {
        drawRect(CameraPreviewBackground)
        drawRect(
            color = CameraPreviewBand.copy(alpha = 0.58f),
            topLeft = Offset(x = 0f, y = size.height * 0.14f),
            size = Size(width = size.width, height = size.height * 0.24f),
        )
        drawCircle(
            color = colors.material.primary.copy(alpha = 0.14f),
            radius = size.minDimension * 0.28f,
            center = Offset(x = size.width * 0.72f, y = size.height * 0.34f),
        )
        drawCircle(
            color = colors.material.secondary.copy(alpha = 0.10f),
            radius = size.minDimension * 0.34f,
            center = Offset(x = size.width * 0.22f, y = size.height * 0.72f),
        )

        val gridColor = CameraPreviewOnSurface.copy(alpha = 0.14f)
        repeat(2) { index ->
            val fraction = (index + 1) / 3f
            drawLine(
                color = gridColor,
                start = Offset(x = size.width * fraction, y = 0f),
                end = Offset(x = size.width * fraction, y = size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = gridColor,
                start = Offset(x = 0f, y = size.height * fraction),
                end = Offset(x = size.width, y = size.height * fraction),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

@Composable
internal fun CameraXPreviewContent(
    lifecycleOwner: LifecycleOwner,
    onPreviewUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture?) -> Unit = {},
    onVideoCaptureReady: (VideoCapture<Recorder>?) -> Unit = {},
) {
    val context = LocalContext.current
    val currentOnImageCaptureReady by rememberUpdatedState(onImageCaptureReady)
    val currentOnVideoCaptureReady by rememberUpdatedState(onVideoCaptureReady)
    val currentOnPreviewUnavailable by rememberUpdatedState(onPreviewUnavailable)
    val previewView =
        remember(context) {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
    )

    DisposableEffect(context, lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var preview: Preview? = null
        var imageCapture: ImageCapture? = null
        var videoCapture: VideoCapture<Recorder>? = null
        val listener =
            Runnable {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    preview =
                        Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    imageCapture = ImageCapture.Builder().build()
                    val recorder =
                        Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.from(
                                    Quality.HD,
                                    FallbackStrategy.higherQualityOrLowerThan(Quality.HD),
                                ),
                            )
                            .build()
                    videoCapture = VideoCapture.withOutput(recorder)
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        videoCapture,
                    )
                    currentOnImageCaptureReady(imageCapture)
                    currentOnVideoCaptureReady(videoCapture)
                }.onFailure {
                    currentOnImageCaptureReady(null)
                    currentOnVideoCaptureReady(null)
                    currentOnPreviewUnavailable()
                }
            }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = preview
                    val imageCapture = imageCapture
                    val videoCapture = videoCapture
                    cameraProvider.unbind(*listOfNotNull(preview, imageCapture, videoCapture).toTypedArray())
                    currentOnImageCaptureReady(null)
                    currentOnVideoCaptureReady(null)
                }
            }
        }
    }
}

@Composable
internal fun CameraPreviewUnavailableContent(modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(CameraPreviewBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 320.dp)
                    .padding(spacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = colors.material.primaryContainer,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.camera_overlay_unavailable_title),
                style = DbCheckTheme.typography.headlineMd,
                color = CameraPreviewOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.space6),
            )
            Text(
                text = stringResource(R.string.camera_overlay_unavailable_description),
                style = DbCheckTheme.typography.bodyMd,
                color = CameraPreviewOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.space3),
            )
        }
    }
}

@Composable
private fun CameraOverlayReadout(modifier: Modifier = Modifier, state: CameraOverlayUiState = CameraOverlayUiState()) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val statusText =
        stringResource(
            when (state.status) {
                CameraOverlayReadoutStatus.READY -> R.string.camera_overlay_status_ready
                CameraOverlayReadoutStatus.LIVE -> R.string.camera_overlay_status_live
            },
        )
    val dbText =
        state.currentDb?.roundToInt()?.let {
            stringResource(R.string.camera_overlay_db_value, it)
        } ?: stringResource(R.string.camera_overlay_db_unavailable)
    val timestampText = cameraOverlayTimestampText(state.timestampMs)
    val readoutDescription =
        stringResource(
            R.string.a11y_camera_overlay_readout,
            statusText,
            dbText,
            state.levelLabel,
            timestampText,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(spacing.space6)
                .semantics {
                    contentDescription = readoutDescription
                },
    ) {
        Text(
            text = statusText,
            style = DbCheckTheme.typography.labelMd,
            color = colors.material.primaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = dbText,
            style = DbCheckTheme.typography.displayMd,
            color = CameraPreviewOnSurface,
        )
        Text(
            text = state.levelLabel,
            style = DbCheckTheme.typography.bodyMd,
            color = CameraPreviewOnSurfaceVariant,
        )
        Text(
            text = timestampText,
            style = DbCheckTheme.typography.labelMd,
            color = CameraPreviewOnSurfaceVariant,
            modifier = Modifier.padding(top = spacing.space1),
        )
    }
}

@Composable
private fun cameraOverlayTimestampText(timestampMs: Long?): String = timestampMs?.let {
        stringResource(R.string.camera_overlay_timestamp_value, formatCameraOverlayTimestamp(it))
    } ?: stringResource(R.string.camera_overlay_timestamp_unavailable)

@Composable
private fun CameraOverlayCaptureControls(
    state: CameraOverlayCaptureControlsState,
    actions: CameraOverlayCaptureControlsActions,
    modifier: Modifier = Modifier,
) {
    val spacing = DbCheckTheme.spacing

    Column(
        modifier = modifier.widthIn(max = 240.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = stringResource(R.string.camera_overlay_video_privacy),
            style = DbCheckTheme.typography.labelMd,
            color = CameraPreviewOnSurfaceVariant,
            textAlign = TextAlign.End,
        )
        if (state.captureFailed) {
            CameraCaptureErrorText(modifier = Modifier.padding(top = spacing.space2))
        }
        if (state.videoCaptureFailed) {
            CameraVideoCaptureErrorText(modifier = Modifier.padding(top = spacing.space2))
        }
        CameraVideoCaptureButton(
            onToggle = actions.onVideoToggle,
            enabled = state.videoEnabled,
            isRecordingVideo = state.isRecordingVideo,
            modifier = Modifier.padding(top = spacing.space3),
        )
        CameraCaptureButton(
            onCapture = actions.onPhotoCapture,
            enabled = state.photoEnabled,
            modifier = Modifier.padding(top = spacing.space3),
        )
    }
}

private data class CameraOverlayCaptureControlsState(
    val photoEnabled: Boolean,
    val videoEnabled: Boolean,
    val isRecordingVideo: Boolean,
    val captureFailed: Boolean,
    val videoCaptureFailed: Boolean,
)

private data class CameraOverlayCaptureControlsActions(val onPhotoCapture: () -> Unit, val onVideoToggle: () -> Unit)

@Composable
private fun CameraCaptureButton(onCapture: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    IconButton(
        onClick = onCapture,
        enabled = enabled,
        modifier =
            modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        colors.material.primary
                    } else {
                        colors.material.surface.copy(alpha = 0.56f)
                    },
                ),
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = stringResource(R.string.a11y_capture_camera_overlay_photo),
            tint =
                if (enabled) {
                    colors.material.onPrimary
                } else {
                    colors.material.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun CameraVideoCaptureButton(
    onToggle: () -> Unit,
    enabled: Boolean,
    isRecordingVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val description =
        stringResource(
            if (isRecordingVideo) {
                R.string.a11y_stop_camera_overlay_video
            } else {
                R.string.a11y_start_camera_overlay_video
            },
        )

    IconButton(
        onClick = onToggle,
        enabled = enabled,
        modifier =
            modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isRecordingVideo -> colors.material.error
                        enabled -> colors.material.primary
                        else -> colors.material.surface.copy(alpha = 0.56f)
                    },
                ),
    ) {
        Icon(
            imageVector = if (isRecordingVideo) Icons.Filled.Stop else Icons.Outlined.Videocam,
            contentDescription = description,
            tint =
                if (enabled) {
                    if (isRecordingVideo) colors.material.onError else colors.material.onPrimary
                } else {
                    colors.material.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun CameraCaptureErrorText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.camera_overlay_capture_failed),
        style = DbCheckTheme.typography.labelMd,
        color = CameraPreviewOnSurface,
        textAlign = TextAlign.End,
        modifier = modifier.widthIn(max = 220.dp),
    )
}

@Composable
private fun CameraVideoCaptureErrorText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.camera_overlay_video_failed),
        style = DbCheckTheme.typography.labelMd,
        color = CameraPreviewOnSurface,
        textAlign = TextAlign.End,
        modifier = modifier.widthIn(max = 220.dp),
    )
}

private fun startCameraOverlayPhotoCapture(
    imageCapture: ImageCapture?,
    context: Context,
    viewModel: CameraOverlayViewModel,
) {
    val capture = imageCapture
    if (capture == null) {
        viewModel.onPhotoCaptureFailed()
        return
    }
    val outputFile =
        runCatching {
            viewModel.createPhotoCaptureFile()
        }.getOrElse {
            viewModel.onPhotoCaptureFailed()
            return
        }
    viewModel.onPhotoCaptureStarted()
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                viewModel.onPhotoCaptured(outputFile)
            }

            override fun onError(exception: ImageCaptureException) {
                viewModel.onPhotoCaptureFailed()
            }
        },
    )
}

private fun startCameraOverlaySilentVideoCapture(
    videoCapture: VideoCapture<Recorder>?,
    context: Context,
    viewModel: CameraOverlayViewModel,
    onRecordingFinalized: () -> Unit,
): Recording? {
    val capture = videoCapture
    if (capture == null) {
        viewModel.onVideoRecordingFailed()
        return null
    }
    val outputFile =
        runCatching {
            viewModel.createSilentVideoFile()
        }.getOrNull()
    return if (outputFile == null) {
        viewModel.onVideoRecordingFailed()
        null
    } else {
        startCameraOverlayRecording(
            capture = capture,
            context = context,
            outputFile = outputFile,
            viewModel = viewModel,
            onRecordingFinalized = onRecordingFinalized,
        )
    }
}

private fun startCameraOverlayRecording(
    capture: VideoCapture<Recorder>,
    context: Context,
    outputFile: java.io.File,
    viewModel: CameraOverlayViewModel,
    onRecordingFinalized: () -> Unit,
): Recording? {
    val outputOptions = FileOutputOptions.Builder(outputFile).build()
    viewModel.onVideoRecordingStarted()
    return runCatching {
        capture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    onRecordingFinalized()
                    if (event.hasError()) {
                        viewModel.onVideoRecordingFailed()
                    } else {
                        viewModel.onVideoRecordingFinished()
                    }
                }
            }
    }.getOrElse {
        onRecordingFinalized()
        viewModel.onVideoRecordingFailed()
        null
    }
}

private fun cameraPermissionRequest(
    context: Context,
    activity: Activity?,
    hasRequestedCameraPermission: Boolean,
): CameraPermissionRequest = CameraPermissionPolicy.resolve(
    cameraGranted = hasCameraPermission(context),
    hasRequestedCameraPermission = hasRequestedCameraPermission,
    shouldShowCameraRationale =
        activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false,
)

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

private val CameraPreviewBackground = Color(0xFF0B1114)
private val CameraPreviewBand = Color(0xFF26343D)
private val CameraPreviewOnSurface = Color(0xFFF2F5F1)
private val CameraPreviewOnSurfaceVariant = Color(0xFFC8D0CA)

@Composable
private fun CameraPermissionDeniedContent(
    permissionStatus: CameraPermissionStatus,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val isPermanentlyDenied = permissionStatus == CameraPermissionStatus.PermanentlyDenied

    Column(
        modifier = modifier.widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = colors.material.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.camera_overlay_permission_title),
            style = DbCheckTheme.typography.headlineMd,
            color = colors.material.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.space6),
        )
        Text(
            text =
                stringResource(
                    if (isPermanentlyDenied) {
                        R.string.camera_overlay_permission_settings_description
                    } else {
                        R.string.camera_overlay_permission_description
                    },
                ),
            style = DbCheckTheme.typography.bodyMd,
            color = colors.material.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.space3),
        )
        DbCheckButton(
            text =
                stringResource(
                    if (isPermanentlyDenied) {
                        R.string.action_open_settings
                    } else {
                        R.string.action_try_again
                    },
                ),
            onClick = if (isPermanentlyDenied) onOpenSettings else onRequestPermission,
            style = DbCheckButtonStyle.Primary,
            modifier = Modifier.padding(top = spacing.space8),
        )
    }
}

@Composable
private fun CameraOverlayCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme

    IconButton(
        onClick = onClose,
        modifier =
            modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.material.surface.copy(alpha = 0.72f)),
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.a11y_close_camera_overlay),
            tint = colors.material.onSurface,
        )
    }
}
