package com.ziegler.kighelper.ui.screens.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.ziegler.kighelper.data.camera.CAMERA_LOG_TAG
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 连接 Compose 页面与当前 CameraX 会话的轻量控制器。
 *
 * 页面只持有该对象，不直接访问 Camera/ImageCapture；会话重建时通过 [attach]
 * 原子替换底层引用，拍照与扫码分析器则交给 [CameraCaptureCoordinator] 管理。
 */
internal class CameraController {
    /** 同时用于拍照回调与二维码分析，串行执行避免多个分析任务抢占解码器。 */
    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val captureCoordinator = CameraCaptureCoordinator(analysisExecutor)
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var analyzer: QrImageAnalyzer? = null
    private var flashOn: Boolean = false
    private var scanMode: Boolean = false

    internal fun attach(
        camera: Camera,
        imageCapture: ImageCapture,
        analyzer: QrImageAnalyzer?,
        videoCapture: VideoCapture<Recorder>?
    ) {
        this.camera = camera
        this.imageCapture = imageCapture
        this.analyzer = analyzer
        this.videoCapture = videoCapture
        applyFlash()
    }

    fun setFlash(on: Boolean) {
        flashOn = on
        applyFlash()
    }

    fun setScanMode(enabled: Boolean) {
        scanMode = enabled
        applyFlash()
    }

    fun setScanningEnabled(enabled: Boolean) {
        analyzer?.enabled = enabled
    }

    fun capture(
        context: Context,
        livePhotoEnabled: Boolean,
        onSaved: (File, File?) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            onError("相机尚未准备好")
            return
        }
        val recorder = videoCapture
        Log.i(CAMERA_LOG_TAG, "capture requested, livePhoto=$livePhotoEnabled, video=$recorder")
        runCatching {
            captureCoordinator.capture(
                context = context,
                imageCapture = capture,
                videoCapture = recorder,
                livePhotoEnabled = livePhotoEnabled,
                onSaved = onSaved,
                onError = onError
            )
        }.onFailure { error ->
            Log.e(CAMERA_LOG_TAG, "capture failed before callback", error)
            onError(error.message ?: "拍照失败")
        }
    }

    /** 解绑当前用例；由 Compose 的 DisposableEffect 在离开页面时调用。 */
    fun detach() {
        analyzer?.enabled = false
        analyzer = null
        captureCoordinator.cancel()
        camera = null
        imageCapture = null
        videoCapture = null
    }

    /** 释放线程池，页面彻底销毁后该实例不再可用。 */
    fun release() {
        detach()
        analysisExecutor.shutdown()
    }

    /**
     * 扫码时用常亮代替拍照闪光灯；拍照模式才让 ImageCapture 使用 FLASH_MODE_ON。
     */
    private fun applyFlash() {
        if (scanMode) {
            imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
            camera?.cameraControl?.enableTorch(flashOn)
        } else {
            camera?.cameraControl?.enableTorch(false)
            imageCapture?.flashMode = if (flashOn) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
        }
    }
}
