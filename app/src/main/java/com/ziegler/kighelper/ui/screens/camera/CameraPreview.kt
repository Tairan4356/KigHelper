package com.ziegler.kighelper.ui.screens.camera

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 内置相机的 Compose 入口。
 *
 * 这里只负责 PreviewView、生命周期和模式切换；CameraX 用例创建、HDR 回退和拍照
 * 分别由 [CameraSessionBinder] 与 [CameraController] 完成。
 */
@Composable
internal fun BuiltInCameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int,
    scanMode: Boolean,
    livePhotoEnabled: Boolean,
    preferHdr: Boolean,
    scanningPaused: Boolean,
    flashOn: Boolean,
    controller: CameraController,
    onQrDetected: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    AndroidView(
        factory = { previewView }, modifier = modifier
    )

    // 镜头、模式或 HDR 设置变化时重建会话；旧会话会由 unbindAll 统一释放。
    LaunchedEffect(lensFacing, scanMode, livePhotoEnabled, preferHdr) {
        try {
            val cameraProvider = CameraSessionBinder.awaitProvider(context)
            provider = cameraProvider
            val session = CameraSessionBinder.bind(
                context = context,
                lifecycleOwner = lifecycleOwner,
                provider = cameraProvider,
                previewView = previewView,
                lensFacing = lensFacing,
                scanMode = scanMode,
                livePhotoEnabled = livePhotoEnabled,
                preferHdr = preferHdr,
                analysisExecutor = controller.analysisExecutor,
                onQrDetected = onQrDetected
            )
            controller.attach(
                camera = session.camera,
                imageCapture = session.imageCapture,
                analyzer = session.analyzer,
                videoCapture = session.videoCapture
            )
            controller.setScanMode(scanMode)
            controller.setScanningEnabled(scanMode && !scanningPaused)
            controller.setFlash(flashOn)
        } catch (error: Exception) {
            onCameraError(error.message ?: "相机启动失败")
        }
    }

    // 扫码结果弹窗/外部应用期间暂停分析，避免同一内容重复入历史。
    LaunchedEffect(scanningPaused) {
        controller.setScanningEnabled(scanMode && !scanningPaused)
    }

    LaunchedEffect(flashOn) {
        controller.setFlash(flashOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            controller.detach()
        }
    }
}
