package com.ziegler.kighelper.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.SettingsRepository
import com.ziegler.kighelper.data.camera.CAMERA_LOG_TAG
import com.ziegler.kighelper.data.camera.CameraAppResolver
import com.ziegler.kighelper.data.camera.CameraCaptureFiles
import com.ziegler.kighelper.data.camera.CameraMode
import com.ziegler.kighelper.data.camera.CameraPhotoSaver
import com.ziegler.kighelper.data.camera.CameraProvider
import com.ziegler.kighelper.data.scan.ScanHistoryRepository
import com.ziegler.kighelper.data.scan.ScannedContent
import com.ziegler.kighelper.data.scan.ScannedContentClassifier
import com.ziegler.kighelper.data.scan.toScanRecord
import com.ziegler.kighelper.data.socialshare.SocialCardQrCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 相机页面的状态与业务流程入口。
 *
 * 相机绑定、拍照和照片写入分别交给 CameraController 与 CameraPhotoSaver；
 * 这里只负责设置变化、扫码结果和用户操作串成可观察状态。
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val cameraAppResolver: CameraAppResolver,
    private val cameraPhotoSaver: CameraPhotoSaver,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    private val initialSettings = settingsRepository.currentSettings
    private val _state = MutableStateFlow(
        CameraUiState(
            source = cameraAppResolver.resolve(initialSettings),
            writePhotoLocation = initialSettings.writePhotoLocation,
            livePhotoEnabled = initialSettings.livePhotoEnabled,
            hdrEnabled = !initialSettings.cameraCompatibilityMode
        )
    )
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CameraEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

    init {
        // 设置变化会立即反映到当前页面，例如切换相机来源或兼容模式。
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val resolved = cameraAppResolver.resolve(settings)
                val previous = _state.value.source
                _state.update {
                    it.copy(
                        source = resolved,
                        writePhotoLocation = settings.writePhotoLocation,
                        livePhotoEnabled = settings.livePhotoEnabled,
                        hdrEnabled = !settings.cameraCompatibilityMode,
                        resumeRequired = it.resumeRequired && resolved.provider == CameraProvider.BUILT_IN
                    )
                }
                if (resolved.fallback && !previous.fallback) {
                    _events.emit(CameraEvent.Message("所选相机不可用，已自动回退"))
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            CameraCaptureFiles.cleanupOld(context)
        }
    }

    /** 切换拍照/扫码时清空上一次结果，避免旧弹窗覆盖新会话。 */
    fun setMode(mode: CameraMode) {
        _state.update {
            it.copy(
                mode = mode,
                scanResult = null,
                autoLaunchPending = false,
                resumeRequired = false,
                isProcessing = false
            )
        }
    }

    /**
     * 处理实时扫码或相册解码得到的原始文本。
     *
     * 识别和写入历史都在后台线程完成；成功写入时才显示顶部提示，
     * 历史写入失败不会阻止用户查看本次识别结果。
     */
    fun onQrDetected(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) return
        if (_state.value.isProcessing || _state.value.scanResult != null || _state.value.resumeRequired) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            val content = withContext(Dispatchers.Default) {
                ScannedContentClassifier.classify(value)
            }
            val stored = runCatching {
                scanHistoryRepository.add(content.toScanRecord())
            }.isSuccess
            _state.update {
                it.copy(
                    isProcessing = false,
                    scanResult = content,
                    autoLaunchPending = content is ScannedContent.Platform
                )
            }
            if (stored) {
                _events.emit(CameraEvent.TopPrompt("已记录到时光轴"))
            }
        }
    }

    /** 外部应用返回后，扫码模式下重新武装分析器，其他模式直接关闭结果。 */
    fun onAutoLaunchResult(opened: Boolean) {
        if (opened) {
            val gateResume = _state.value.mode == CameraMode.SCAN
            _state.update {
                it.copy(
                    scanResult = null, autoLaunchPending = false, resumeRequired = gateResume
                )
            }
        } else {
            _state.update { it.copy(autoLaunchPending = false) }
        }
    }

    /** 用户关闭扫码结果弹窗后，允许下一轮识别。 */
    fun consumeScanResult() {
        _state.update {
            it.copy(
                scanResult = null, autoLaunchPending = false, resumeRequired = false
            )
        }
    }

    /** 用户点击“继续扫码”，解除外部应用返回后的扫码暂停。 */
    fun resumeScanning() {
        _state.update { it.copy(resumeRequired = false) }
    }

    /**
     * 在真正发起拍照前占用“拍摄中”状态。
     *
     * 该状态只覆盖 CameraX 回调，不覆盖后续照片写入，因此用户保存期间仍可继续取景。
     */
    fun onCaptureStarted(): Boolean {
        if (_state.value.isCapturing || _state.value.isProcessing) return false
        _state.update { it.copy(isCapturing = true) }
        Log.i(CAMERA_LOG_TAG, "UI capture started")
        return true
    }

    /**
     * 扫码模式手动快门：只占用拍摄状态，照片不会保存到相册。
     *
     * 自动识别对密集/过小二维码可能失效，用户可拍一张清晰照片后由本地解码器识别。
     */
    fun onScanCaptureStarted(): Boolean {
        if (_state.value.isCapturing || _state.value.isProcessing) return false
        _state.update { it.copy(isCapturing = true) }
        Log.i(CAMERA_LOG_TAG, "scan snapshot capture started")
        return true
    }

    /**
     * 解码扫码模式拍下的临时照片，完成后立即清理缓存文件。
     *
     * 识别成功仍复用正常的扫码结果流程（历史入库、自动拉起平台）。
     */
    fun onScanImageCaptured(file: File) {
        _state.update { it.copy(isCapturing = false, isProcessing = true) }
        viewModelScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                SocialCardQrCodec.decode(context, file.absolutePath)
            }
            CameraCaptureFiles.cleanup(context, file)
            _state.update { it.copy(isProcessing = false) }
            if (decoded.isNullOrBlank()) {
                _events.emit(CameraEvent.Message("未识别到二维码，请调整距离后重试"))
            } else {
                onQrDetected(decoded)
            }
        }
    }

    /**
     * 统一处理相机错误；HDR 拍照超时会自动降级并持久化兼容模式，下次启动不再重试 HDR。
     */
    fun reportCameraError(message: String) {
        Log.e(CAMERA_LOG_TAG, "camera error: $message")
        val switchedToCompatibility = _state.value.hdrEnabled && message.startsWith("拍照超时")
        if (switchedToCompatibility) {
            settingsRepository.updateCameraCompatibilityMode(true)
        }
        _state.update {
            it.copy(
                isProcessing = false,
                isCapturing = false,
                hdrEnabled = if (switchedToCompatibility) false else it.hdrEnabled
            )
        }
        _events.tryEmit(
            CameraEvent.Message(
                if (switchedToCompatibility) {
                    "拍照超时，已切换兼容模式，请重试"
                } else {
                    message
                }
            )
        )
    }

    /**
     * 接收 CameraX 已落盘的缓存文件，并异步完成元数据、实况视频和相册写入。
     *
     * @param file 临时 JPEG；无论保存结果如何都会由 [CameraPhotoSaver] 清理。
     * @param videoFile 临时 MP4；普通拍照时为 null。
     */
    fun onBuiltInPhotoCaptured(file: File, videoFile: File? = null) {
        _state.update { it.copy(isCapturing = false) }
        Log.i(CAMERA_LOG_TAG, "photo captured, saving in background")
        val writeLocation = _state.value.writePhotoLocation
        viewModelScope.launch {
            val result = cameraPhotoSaver.save(
                photoFile = file, videoFile = videoFile, writeLocation = writeLocation
            )
            result.onSuccess { saved ->
                Log.i(CAMERA_LOG_TAG, "photo saved: ${saved.displayName}")
                _events.emit(CameraEvent.PhotoSaved(saved))
            }.onFailure {
                _events.emit(CameraEvent.Message("照片保存失败"))
            }
        }
    }

    /** 从相册图片解码二维码；解码与识别都放在后台线程，避免阻塞预览。 */
    fun onGalleryImageSelected(uri: Uri) {
        if (_state.value.isProcessing || _state.value.isCapturing) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            val decoded = withContext(Dispatchers.IO) {
                SocialCardQrCodec.decode(context, uri.toString())
            }
            _state.update { it.copy(isProcessing = false) }
            if (decoded.isNullOrBlank()) {
                _events.emit(CameraEvent.Message("相册图片中未识别到二维码"))
            } else {
                onQrDetected(decoded)
            }
        }
    }

    /** 外部相机连拍 Intent；第三方源优先使用用户选中的应用。 */
    fun createExternalBurstIntent(): Intent? {
        val source = _state.value.source
        if (source.provider == CameraProvider.BUILT_IN) return null
        val app = if (source.provider == CameraProvider.THIRD_PARTY) source.app else null
        return cameraAppResolver.buildBurstIntent(app)
    }

    /** 连拍 Intent 不可用时的普通启动 Intent。 */
    fun createExternalLaunchIntent(): Intent? {
        val source = _state.value.source
        if (source.provider == CameraProvider.BUILT_IN) return null
        val app = source.app ?: return null
        return cameraAppResolver.buildLaunchIntent(app)
    }

    /** 外部相机启动失败时恢复按钮状态并提示。 */
    fun onExternalLaunchFailure() {
        _state.update { it.copy(isProcessing = false) }
        _events.tryEmit(CameraEvent.Message("无法打开相机应用"))
    }
}
