package com.ziegler.kighelper.ui.screens.camera

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.data.camera.CameraMode
import com.ziegler.kighelper.data.camera.CameraProvider
import com.ziegler.kighelper.data.scan.ScannedContent
import com.ziegler.kighelper.ui.CameraEvent
import com.ziegler.kighelper.ui.CameraViewModel
import com.ziegler.kighelper.ui.utils.findActivity
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics
import com.ziegler.kighelper.utils.SocialPlatformLauncher
import com.ziegler.kighelper.utils.VolumeKeyDispatcher
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 相机页的编排层：连接 ViewModel、系统权限和 CameraX 预览。
 *
 * 页面级 Composable 只保留有状态的生命周期逻辑；无状态的展示组件已拆分到同包的
 * 独立文件，便于单独调整布局而不影响拍照流程。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    contentPadding: PaddingValues,
    onOpenTimeline: () -> Unit,
    onOpenSettings: () -> Unit,
    externalLaunchRequested: Boolean = false,
    onExternalLaunchHandled: () -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val haptics = rememberPhysicalButtonHaptics()
    val controller = remember { CameraController() }
    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    var flashOn by rememberSaveable { mutableStateOf(false) }
    var lensFacing by rememberSaveable {
        mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
    }
    var hasCameraPermission by remember {
        mutableStateOf(CameraPermissions.hasCamera(context))
    }
    var requestedCameraPermission by rememberSaveable { mutableStateOf(false) }
    var cameraPermissionPermanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var pendingBuiltInCapture by rememberSaveable { mutableStateOf(false) }
    var topPrompt by remember { mutableStateOf<CameraEvent.TopPrompt?>(null) }
    var livePhotoCountdown by remember { mutableIntStateOf(0) }
    var hasLocationPermission by remember {
        mutableStateOf(CameraPermissions.hasLocation(context))
    }
    var requestedLocationPermission by rememberSaveable { mutableStateOf(false) }

    val scanMode = state.mode == CameraMode.SCAN
    val useBuiltIn = scanMode || state.source.provider == CameraProvider.BUILT_IN
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val deviceRotation = rememberDeviceRotation()

    /** 系统是否仍允许弹出相机授权框；首次请求时也返回 true。 */
    fun canRequestCameraPermission(): Boolean {
        val activity = context.findActivity() ?: return true
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.CAMERA
        )
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraPermissionPermanentlyDenied = false
        } else {
            cameraPermissionPermanentlyDenied = !canRequestCameraPermission()
            if (cameraPermissionPermanentlyDenied) {
                // 系统已禁止弹窗，直接带用户去设置，避免按钮看起来没有反应。
                openAppSettings()
            } else {
                Toast.makeText(context, "未授予相机权限，无法拍照或扫码", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 需要相机权限时优先弹窗，弹窗被禁用后改为打开应用设置。 */
    fun requestCameraPermissionOrOpenSettings() {
        if (cameraPermissionPermanentlyDenied || (requestedCameraPermission && !canRequestCameraPermission())) {
            openAppSettings()
        } else {
            requestedCameraPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result.values.any { it }
        if (!hasLocationPermission) {
            Toast.makeText(
                context, "未授予定位权限，照片将不写入位置信息", Toast.LENGTH_SHORT
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(viewModel::onGalleryImageSelected)
    }

    val burstLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 外部相机负责保存照片，返回应用时无需额外处理。
    }

    /** 直接调用 CameraX 拍照；调用前已由 [viewModel.onCaptureStarted] 占用拍摄状态。 */
    fun captureBuiltInPhoto() {
        if (state.isCapturing || state.isProcessing) return
        if (!viewModel.onCaptureStarted()) return
        if (state.livePhotoEnabled) {
            livePhotoCountdown = 2
        }
        controller.capture(
            context = context,
            livePhotoEnabled = state.livePhotoEnabled,
            onSaved = viewModel::onBuiltInPhotoCaptured,
            onError = viewModel::reportCameraError
        )
    }

    /** 扫码快门：拍一张临时照片交给 ViewModel 解码，不写入相册。 */
    fun captureScanSnapshot() {
        if (state.isCapturing || state.isProcessing) return
        if (!viewModel.onScanCaptureStarted()) return
        controller.capture(
            context = context,
            livePhotoEnabled = false,
            onSaved = { file, _ -> viewModel.onScanImageCaptured(file) },
            onError = viewModel::reportCameraError
        )
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val shouldCapture = pendingBuiltInCapture
        pendingBuiltInCapture = false
        if (granted && shouldCapture) {
            captureBuiltInPhoto()
        } else if (!granted) {
            Toast.makeText(context, "未授予存储权限，无法保存照片", Toast.LENGTH_SHORT).show()
        }
    }

    /** Android 9 及以下首次拍照前请求存储权限，授权成功后继续原拍摄。 */
    fun requestBuiltInCapture() {
        if (CameraPermissions.needsLegacyWritePermission(context)) {
            pendingBuiltInCapture = true
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            captureBuiltInPhoto()
        }
    }

    /** 优先打开系统连拍界面；部分 ROM 不支持时退化为普通启动 Intent。 */
    fun launchExternalBurst() {
        val intent = viewModel.createExternalBurstIntent()
        if (intent == null) {
            viewModel.reportCameraError("未找到可用的相机应用")
            return
        }
        try {
            burstLauncher.launch(intent)
        } catch (_: Exception) {
            val fallback = viewModel.createExternalLaunchIntent()
            if (fallback == null) {
                viewModel.onExternalLaunchFailure()
            } else {
                try {
                    burstLauncher.launch(fallback)
                    Toast.makeText(
                        context, "已直接打开相机应用", Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    viewModel.onExternalLaunchFailure()
                }
            }
        }
    }

    // 只有内置相机需要 CAMERA 权限；外部相机由目标应用自行申请。
    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !requestedCameraPermission && useBuiltIn) {
            requestedCameraPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 从系统设置或外部相机返回时，权限状态可能已经变化。
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasCameraPermission = CameraPermissions.hasCamera(context)
        hasLocationPermission = CameraPermissions.hasLocation(context)
        if (hasCameraPermission) {
            cameraPermissionPermanentlyDenied = false
        } else if (requestedCameraPermission) {
            cameraPermissionPermanentlyDenied = !canRequestCameraPermission()
        }
    }

    // 外部相机由目标应用自行申请相机权限，本应用没有相机权限时也应能直接拉起。
    LaunchedEffect(
        externalLaunchRequested, hasCameraPermission, state.source.provider, state.mode
    ) {
        val canLaunchExternal =
            state.mode == CameraMode.PHOTO && state.source.provider != CameraProvider.BUILT_IN
        if (externalLaunchRequested && (hasCameraPermission || canLaunchExternal)) {
            onExternalLaunchHandled()
            if (canLaunchExternal) {
                launchExternalBurst()
            }
        }
    }

    // 外部相机模式下如果停留在扫码页，会因缺少相机权限而无法退出；自动回到拍照并提供外部启动。
    LaunchedEffect(hasCameraPermission, state.source.provider, state.mode) {
        if (!hasCameraPermission && state.source.provider != CameraProvider.BUILT_IN && state.mode == CameraMode.SCAN) {
            viewModel.setMode(CameraMode.PHOTO)
        }
    }

    // 首次启用位置写入且缺少权限时请求定位；拒绝后照片仍会正常保存，只是不带 GPS。
    LaunchedEffect(
        state.writePhotoLocation, state.mode, state.source.provider, hasCameraPermission
    ) {
        if (state.writePhotoLocation && state.mode == CameraMode.PHOTO && state.source.provider == CameraProvider.BUILT_IN && hasCameraPermission && !hasLocationPermission && !requestedLocationPermission) {
            requestedLocationPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 音量键通过 Activity 分发到当前页面；Compose 闭包需要读取最新状态。
    val latestState by rememberUpdatedState(state)
    val latestHasCameraPermission by rememberUpdatedState(hasCameraPermission)
    val latestCapture by rememberUpdatedState {
        requestBuiltInCapture()
    }
    DisposableEffect(Unit) {
        VolumeKeyDispatcher.handler = {
            val currentState = latestState
            if (latestHasCameraPermission && currentState.mode == CameraMode.PHOTO && currentState.source.provider == CameraProvider.BUILT_IN && !currentState.isProcessing && !currentState.isCapturing && currentState.scanResult == null) {
                latestCapture()
                true
            } else {
                false
            }
        }
        onDispose {
            VolumeKeyDispatcher.handler = null
        }
    }

    // 实况照片的 2 秒倒计时覆盖后录阶段，不阻塞真正拍照时机。
    LaunchedEffect(livePhotoCountdown) {
        if (livePhotoCountdown > 0) {
            delay(1_000L.milliseconds)
            livePhotoCountdown -= 1
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CameraEvent.Message -> snackBarHostState.showSnackbar(event.text)
                is CameraEvent.TopPrompt -> topPrompt = event
                is CameraEvent.PhotoSaved -> {
                    topPrompt = CameraEvent.TopPrompt(
                        text = "已保存到相册", showTimelineAction = true
                    )
                }
            }
        }
    }

    // 有扫码结果或等待用户“继续扫码”时保持提示，避免用户还没看到就自动消失。
    LaunchedEffect(topPrompt, state.scanResult, state.resumeRequired) {
        if (topPrompt != null && state.scanResult == null && !state.resumeRequired) {
            delay(3_000L.milliseconds)
            topPrompt = null
        }
    }

    LaunchedEffect(state.scanResult) {
        if (state.scanResult != null) haptics()
    }

    // 识别到平台链接时自动尝试打开；打开成功后才关闭结果并重新武装扫码。
    LaunchedEffect(state.scanResult, state.autoLaunchPending) {
        val content = state.scanResult
        if (state.autoLaunchPending && content is ScannedContent.Platform) {
            val opened = SocialPlatformLauncher.openScannedContent(
                context, content.rawContent, content.match.iconKey
            )
            viewModel.onAutoLaunchResult(opened)
        }
    }

    val scanningPaused =
        state.isProcessing || state.scanResult != null || (state.resumeRequired && useBuiltIn)

    val controlsVisible = !useBuiltIn || hasCameraPermission
    val controlsEnabled =
        !state.isProcessing && !state.isCapturing && state.scanResult == null && livePhotoCountdown == 0

    val cameraPreview: @Composable BoxScope.() -> Unit = {
        when {
            !hasCameraPermission && useBuiltIn -> {
                CameraPermissionCard(
                    canRequestPermission = !cameraPermissionPermanentlyDenied,
                    onPermissionAction = ::requestCameraPermissionOrOpenSettings
                )
            }

            useBuiltIn -> {
                // 竖屏保持 3:4，横屏改为 4:3；两种方向都按比例居中，不会拉伸。
                val previewAspect = if (isLandscape) 4f / 3f else 3f / 4f
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    val previewWidth = minOf(maxWidth, maxHeight * previewAspect)
                    Box(
                        modifier = Modifier
                            .width(previewWidth)
                            .aspectRatio(previewAspect)
                            .clipToBounds()
                    ) {
                        BuiltInCameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            lensFacing = lensFacing,
                            scanMode = scanMode,
                            livePhotoEnabled = state.livePhotoEnabled && !scanMode,
                            preferHdr = state.hdrEnabled,
                            scanningPaused = scanningPaused,
                            flashOn = flashOn,
                            targetRotation = deviceRotation,
                            controller = controller,
                            onQrDetected = viewModel::onQrDetected,
                            onCameraError = viewModel::reportCameraError
                        )
                        if (scanMode) {
                            ScanFrameHint(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.6f)
                            )
                        }
                    }
                }
            }

            else -> {
                ExternalCameraCard(app = state.source.app)
            }
        }

        if (livePhotoCountdown > 0 && state.mode == CameraMode.PHOTO && useBuiltIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "实况",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = livePhotoCountdown.toString(),
                        color = Color.White,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (state.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在识别…", color = Color.White
                    )
                }
            }
        }
    }

    val modeSelector: @Composable () -> Unit = {
        // 没有相机权限时隐藏模式切换，避免用户进入无法使用的扫码取景。
        if (hasCameraPermission) {
            CameraModeSelector(
                selected = state.mode, enabled = controlsEnabled, onSelected = { mode ->
                    viewModel.setMode(mode)
                    // 外部相机没有内部取景页，点“拍照”就直接拉起。
                    if (mode == CameraMode.PHOTO && state.source.provider != CameraProvider.BUILT_IN) {
                        launchExternalBurst()
                    }
                })
        }
    }

    val captureControls: @Composable () -> Unit = {
        if (controlsVisible) {
            CameraControls(
                mode = state.mode,
                flashOn = flashOn,
                enabled = controlsEnabled,
                externalCamera = !useBuiltIn,
                vertical = isLandscape,
                onFlashChange = { flashOn = it },
                onPickGallery = { galleryLauncher.launch("image/*") },
                onSwitchCamera = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                onCapture = {
                    when {
                        !useBuiltIn -> launchExternalBurst()
                        state.mode == CameraMode.PHOTO -> requestBuiltInCapture()
                        else -> captureScanSnapshot()
                    }
                })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            topBar = {
                if (!isLandscape) {
                    // 提示悬浮在 TopAppBar 内部居中。
                    Box {
                        TopAppBar(
                            title = {
                            Text(
                                text = if (scanMode) "扫码" else "拍照",
                                fontWeight = FontWeight.SemiBold
                            )
                        }, actions = {
                            IconButton(onClick = onOpenTimeline) {
                                Icon(
                                    Icons.Filled.PhotoLibrary, contentDescription = "时光轴"
                                )
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = "设置")
                            }
                        }, colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ), windowInsets = WindowInsets(0, 0, 0, 0)
                        )
                        AnimatedVisibility(
                            visible = topPrompt != null, modifier = Modifier.align(Alignment.Center)
                        ) {
                            topPrompt?.let { prompt ->
                                CameraTopPromptBanner(prompt = prompt)
                            }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackBarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { scaffoldPadding ->
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp)
                            .clipToBounds()
                    ) {
                        cameraPreview()
                        topPrompt?.let { prompt ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp)
                            ) {
                                CameraTopPromptBanner(prompt = prompt)
                            }
                        }
                    }

                    if (hasCameraPermission || controlsVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasCameraPermission) {
                                RotatedControl(
                                    originalWidth = 260.dp, originalHeight = 56.dp
                                ) {
                                    modeSelector()
                                }
                            }
                            Box(modifier = Modifier.width(112.dp)) {
                                captureControls()
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clipToBounds()
                    ) {
                        cameraPreview()
                    }
                    modeSelector()
                    captureControls()
                }
            }
        }
    }

    state.scanResult?.takeIf { !state.autoLaunchPending }?.let { content ->
        ScanResultDialog(
            content = content, onDismiss = viewModel::consumeScanResult, dismissLabel = "继续扫码"
        )
    }
}
