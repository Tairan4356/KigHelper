package com.ziegler.kighelper.ui.screens.camera

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 内置相机的 Compose 入口。
 *
 * 这里负责 PreviewView、生命周期、模式切换和点击对焦；CameraX 用例创建、HDR 回退
 * 和拍照分别由 [CameraSessionBinder] 与 [CameraController] 完成。
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
    targetRotation: Int,
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
    val latestTargetRotation by rememberUpdatedState(targetRotation)

    // focusTapId 用于在连续点击同一位置时重新触发圆点动画。
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusTapId by remember { mutableIntStateOf(0) }
    val focusAnimation = remember { Animatable(0f) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView }, modifier = Modifier.fillMaxSize()
        )

        // 取景区域上的点击层：拍照模式按点击位置对焦，扫码模式固定中心对焦。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scanMode, previewView) {
                    detectTapGestures { offset ->
                        val viewWidth = previewView.width
                        val viewHeight = previewView.height
                        if (viewWidth <= 0 || viewHeight <= 0) return@detectTapGestures
                        // MeteringPointFactory 会处理 FILL_CENTER 裁剪后的坐标换算。
                        val factory = previewView.meteringPointFactory
                        if (scanMode) {
                            controller.focus(
                                factory.createPoint(viewWidth / 2f, viewHeight / 2f)
                            )
                        } else {
                            val x = offset.x.coerceIn(0f, viewWidth - 1f)
                            val y = offset.y.coerceIn(0f, viewHeight - 1f)
                            focusPoint = Offset(x, y)
                            focusTapId += 1
                            controller.focus(factory.createPoint(x, y))
                        }
                    }
                }
        )

        // 对焦圆点只跟随拍照模式的点击位置，扫码模式不会进入这个分支。
        focusPoint?.let { point ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = focusAnimation.value
                val alpha = 1f - progress
                val radius = 22.dp.toPx() * (0.8f + 0.2f * progress)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f * alpha),
                    radius = radius,
                    center = point,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * alpha),
                    radius = radius,
                    center = point,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }

    // 每次点击都重新播放缩放/淡出动画，结束后移除圆点。
    LaunchedEffect(focusTapId) {
        if (focusTapId == 0) return@LaunchedEffect
        focusAnimation.snapTo(0f)
        focusAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        focusPoint = null
    }

    // 切到扫码模式时立即清掉尚未消失的圆点。
    LaunchedEffect(scanMode) {
        if (scanMode) focusPoint = null
    }

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
                targetRotation = latestTargetRotation,
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
            controller.setTargetRotation(latestTargetRotation)
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

    LaunchedEffect(targetRotation) {
        controller.setTargetRotation(targetRotation)
    }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            controller.detach()
        }
    }
}
