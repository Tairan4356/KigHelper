package com.ziegler.kighelper.ui.screens.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.ziegler.kighelper.data.camera.CAMERA_LOG_TAG
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 一次成功绑定后的 CameraX 对象集合，由 [CameraController] 接管。 */
internal data class CameraSession(
    val camera: Camera,
    val imageCapture: ImageCapture,
    val analyzer: QrImageAnalyzer?,
    val videoCapture: VideoCapture<Recorder>?
)

/**
 * 负责创建 CameraX 用例并绑定到生命周期。
 *
 * 将 SessionConfig/HDR 兼容回退集中在这里，Compose 层只需要处理预览 View 和错误。
 */
internal object CameraSessionBinder {

    /** 等待 CameraProvider 初始化；调用方应处于主线程。 */
    suspend fun awaitProvider(context: Context): ProcessCameraProvider {
        return ProcessCameraProvider.getInstance(context).awaitOnMain(context)
    }

    /**
     * 创建预览、拍照（扫码时还有分析器、实况时还有录像）并完成绑定。
     *
     * @param preferHdr 是否优先尝试 ULTRA_HDR 会话；失败会自动回退到普通会话。
     */
    fun bind(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        provider: ProcessCameraProvider,
        previewView: PreviewView,
        lensFacing: Int,
        scanMode: Boolean,
        livePhotoEnabled: Boolean,
        preferHdr: Boolean,
        analysisExecutor: ExecutorService,
        onQrDetected: (String) -> Unit
    ): CameraSession {
        // 每次切换镜头/模式都重建完整的用例集合，防止旧分析器继续回调。
        provider.unbindAll()

        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }
        val imageCapture = ImageCapture.Builder().build()
        val useCases = mutableListOf<UseCase>(preview, imageCapture)

        val analyzer = if (scanMode) {
            QrImageAnalyzer(onQrDetected).also { qrAnalyzer ->
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build()
                    .apply {
                        setAnalyzer(analysisExecutor, qrAnalyzer)
                    }
                useCases += analysis
            }
        } else {
            null
        }

        val videoCapture = if (!scanMode && livePhotoEnabled) {
            val recorder = Recorder.Builder().setAspectRatio(AspectRatio.RATIO_4_3).build()
            VideoCapture.withOutput(recorder).also { useCases += it }
        } else {
            null
        }

        val selector = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        val camera = bindCamera(
            provider = provider,
            lifecycleOwner = lifecycleOwner,
            selector = selector,
            useCases = useCases,
            preferHdr = preferHdr
        )
        return CameraSession(
            camera = camera,
            imageCapture = imageCapture,
            analyzer = analyzer,
            videoCapture = videoCapture
        )
    }

    /**
     * 先验证 HDR 会话是否受支持，再尝试绑定；任何一步失败都回退到普通绑定。
     *
     * 相机 HAL 对 SessionConfig 的支持并不总是可靠，因此这里保留两级保护：
     * 1. 不支持时直接不创建 HDR 会话；
     * 2. 支持但 bind 抛异常时，用同一组用例重新绑定。
     */
    private fun bindCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        useCases: List<UseCase>,
        preferHdr: Boolean
    ): Camera {
        if (!preferHdr) {
            Log.i(CAMERA_LOG_TAG, "using compatibility camera session without HDR")
            return provider.bindToLifecycle(
                lifecycleOwner, selector, *useCases.toTypedArray()
            )
        }

        val sessionConfig = SessionConfig.Builder(*useCases.toTypedArray())
            .setRequiredFeatureGroup(GroupableFeature.IMAGE_ULTRA_HDR).build()
        sessionConfig.setFeatureSelectionListener { features ->
            Log.i(CAMERA_LOG_TAG, "selected features: $features")
        }
        val sessionSupported = runCatching {
            provider.getCameraInfo(selector, sessionConfig).isSessionConfigSupported(sessionConfig)
        }.onFailure {
            Log.w(CAMERA_LOG_TAG, "isSessionConfigSupported failed", it)
        }.getOrDefault(false)
        Log.i(CAMERA_LOG_TAG, "session config supported: $sessionSupported")

        if (!sessionSupported) {
            return provider.bindToLifecycle(
                lifecycleOwner, selector, *useCases.toTypedArray()
            )
        }
        return runCatching {
            provider.bindToLifecycle(lifecycleOwner, selector, sessionConfig)
        }.onFailure {
            Log.w(CAMERA_LOG_TAG, "session config bind failed, retrying without features", it)
        }.getOrElse {
            provider.bindToLifecycle(
                lifecycleOwner, selector, *useCases.toTypedArray()
            )
        }
    }
}

private suspend fun <T> ListenableFuture<T>.awaitOnMain(context: Context): T {
    return suspendCancellableCoroutine { continuation ->
        addListener({
            try {
                continuation.resume(get())
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
