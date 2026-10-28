package com.ziegler.kighelper.ui.screens.camera

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.ziegler.kighelper.data.camera.CAMERA_LOG_TAG
import com.ziegler.kighelper.data.camera.CameraCaptureFiles
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 封装一次拍照的生命周期，避免 Activity/Composable 直接处理 CameraX 回调。
 *
 * 普通拍照只等待一个 JPEG；实况照片同时启动视频录制与静态拍照，
 * 只有两者都完成（或超时）后才把结果交给上层。
 */
internal class CameraCaptureCoordinator(
    private val analysisExecutor: ExecutorService,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    private var activeRecording: Recording? = null

    fun capture(
        context: Context,
        imageCapture: ImageCapture,
        videoCapture: VideoCapture<Recorder>?,
        livePhotoEnabled: Boolean,
        onSaved: (File, File?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (livePhotoEnabled && videoCapture != null) {
            captureLivePhoto(
                context = context,
                imageCapture = imageCapture,
                videoCapture = videoCapture,
                onSaved = onSaved,
                onError = onError
            )
        } else {
            captureStillPhoto(
                context = context, imageCapture = imageCapture, onSaved = onSaved, onError = onError
            )
        }
    }

    /** 普通拍照：超时后主动清理半成品，避免 CameraX 永久无回调时界面卡在拍摄中。 */
    private fun captureStillPhoto(
        context: Context,
        imageCapture: ImageCapture,
        onSaved: (File, File?) -> Unit,
        onError: (String) -> Unit
    ) {
        val file = CameraCaptureFiles.create(context)
        val finished = AtomicBoolean(false)
        val timeout = Runnable {
            if (finished.compareAndSet(false, true)) {
                Log.w(CAMERA_LOG_TAG, "capture timed out: ${file.name}")
                CameraCaptureFiles.cleanup(context, file)
                onError("拍照超时，请重试")
            }
        }
        mainHandler.postDelayed(timeout, CAPTURE_TIMEOUT_MS)
        Log.i(CAMERA_LOG_TAG, "takePicture start: ${file.name}")
        runCatching {
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                analysisExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(
                        outputFileResults: ImageCapture.OutputFileResults
                    ) {
                        if (!finished.compareAndSet(false, true)) return
                        mainHandler.removeCallbacks(timeout)
                        Log.i(CAMERA_LOG_TAG, "takePicture saved: ${file.length()} bytes")
                        onSaved(file, null)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (!finished.compareAndSet(false, true)) return
                        mainHandler.removeCallbacks(timeout)
                        Log.e(
                            CAMERA_LOG_TAG,
                            "takePicture error: ${exception.imageCaptureError}",
                            exception
                        )
                        CameraCaptureFiles.cleanup(context, file)
                        onError(exception.message ?: "拍照失败")
                    }
                })
        }.onFailure { error ->
            if (!finished.compareAndSet(false, true)) return@onFailure
            mainHandler.removeCallbacks(timeout)
            Log.e(CAMERA_LOG_TAG, "takePicture call failed", error)
            CameraCaptureFiles.cleanup(context, file)
            onError(error.message ?: "拍照失败")
        }
    }

    /**
     * 实况照片：先录制约 2.2 秒视频，250ms 后拍静态帧，再等待视频 Finalize。
     *
     * 视频不可用或 Finalize 失败时退化为普通照片，保证用户至少拿到一张图片；
     * 超时后如果照片已经成功，也会优先返回照片而不是报错。
     */
    private fun captureLivePhoto(
        context: Context,
        imageCapture: ImageCapture,
        videoCapture: VideoCapture<Recorder>,
        onSaved: (File, File?) -> Unit,
        onError: (String) -> Unit
    ) {
        val photoFile = CameraCaptureFiles.create(context)
        val videoFile = CameraCaptureFiles.createVideo(context)
        val photoRef = AtomicReference<File?>()
        val videoRef = AtomicReference<File?>()
        val videoDone = AtomicBoolean(false)
        val completed = AtomicBoolean(false)
        var timedOut = false

        fun finishWithPhoto(fallbackToNormal: Boolean) {
            if (!completed.compareAndSet(false, true)) return
            mainHandler.removeCallbacksAndMessages(null)
            val photo = photoRef.get()
            if (photo == null) {
                Log.w(
                    CAMERA_LOG_TAG, "live photo finished without photo, timedOut=$timedOut"
                )
                runCatching { activeRecording?.stop() }
                activeRecording = null
                CameraCaptureFiles.cleanup(context, photoFile)
                CameraCaptureFiles.cleanup(context, videoFile)
                onError(if (timedOut) "拍照超时，请重试" else "拍照失败")
                return
            }
            val video = if (fallbackToNormal) null else videoRef.get()
            if (video == null) {
                runCatching { activeRecording?.stop() }
            }
            Log.i(
                CAMERA_LOG_TAG,
                "live photo finished: photo=${photo.length()}, video=${video?.length()}"
            )
            CameraCaptureFiles.cleanup(context, if (video == null) videoFile else null)
            activeRecording = null
            onSaved(photo, video)
        }

        fun tryFinish() {
            if (photoRef.get() != null && videoDone.get()) {
                finishWithPhoto(fallbackToNormal = false)
            }
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prepared = runCatching {
            videoCapture.output.prepareRecording(
                context, FileOutputOptions.Builder(videoFile).build()
            ).start(executor) { event ->
                if (event is VideoRecordEvent.Start) {
                    Log.i(CAMERA_LOG_TAG, "live video started")
                }
                if (event is VideoRecordEvent.Finalize) {
                    Log.i(
                        CAMERA_LOG_TAG,
                        "live video finalize: error=${event.error}, " + "size=${videoFile.length()}"
                    )
                    if (!event.hasError() && videoFile.exists() && videoFile.length() > 0L) {
                        videoRef.set(videoFile)
                    }
                    videoDone.set(true)
                    tryFinish()
                }
            }
        }.getOrNull()

        if (prepared == null) {
            Log.w(CAMERA_LOG_TAG, "live video unavailable, falling back to still photo")
            CameraCaptureFiles.cleanup(context, videoFile)
            captureStillPhoto(context, imageCapture, onSaved, onError)
            return
        }
        Log.i(CAMERA_LOG_TAG, "live photo started")
        activeRecording = prepared

        mainHandler.postDelayed({
            runCatching { prepared.stop() }
        }, LIVE_PHOTO_DURATION_MS)

        mainHandler.postDelayed({
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                analysisExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(
                        outputFileResults: ImageCapture.OutputFileResults
                    ) {
                        photoRef.set(photoFile)
                        tryFinish()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (!completed.compareAndSet(false, true)) return
                        mainHandler.removeCallbacksAndMessages(null)
                        runCatching { prepared.stop() }
                        if (activeRecording === prepared) activeRecording = null
                        CameraCaptureFiles.cleanup(context, photoFile)
                        CameraCaptureFiles.cleanup(context, videoFile)
                        captureStillPhoto(context, imageCapture, onSaved, onError)
                    }
                })
        }, LIVE_PHOTO_CAPTURE_DELAY_MS)

        mainHandler.postDelayed({
            timedOut = true
            if (photoRef.get() != null) {
                videoDone.set(true)
                finishWithPhoto(fallbackToNormal = videoRef.get() == null)
            } else {
                finishWithPhoto(fallbackToNormal = true)
            }
        }, LIVE_PHOTO_TIMEOUT_MS)
    }

    /** 页面销毁或相机会话重建时取消仍在进行的录制和延时任务。 */
    fun cancel() {
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { activeRecording?.stop() }
        activeRecording = null
    }

    private companion object {
        const val CAPTURE_TIMEOUT_MS = 10_000L
        const val LIVE_PHOTO_CAPTURE_DELAY_MS = 250L
        const val LIVE_PHOTO_DURATION_MS = 2_200L
        const val LIVE_PHOTO_TIMEOUT_MS = 7_000L
    }
}
