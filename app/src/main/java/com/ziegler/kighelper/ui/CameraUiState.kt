package com.ziegler.kighelper.ui

import com.ziegler.kighelper.data.camera.CameraMode
import com.ziegler.kighelper.data.camera.ResolvedCameraSource
import com.ziegler.kighelper.data.camera.SavedPhoto
import com.ziegler.kighelper.data.scan.ScannedContent

/**
 * 相机页面唯一的数据源。
 *
 * 页面只读取状态并转发用户操作，所有会异步变化的状态都由 [CameraViewModel] 更新，
 * 避免拍照、扫码和外部相机返回时各自维护一套互相同步的界面状态。
 */
data class CameraUiState(
    val source: ResolvedCameraSource,
    val mode: CameraMode = CameraMode.PHOTO,
    /** 扫码识别、相册解码等短时任务，会暂停预览上的扫码分析。 */
    val isProcessing: Boolean = false,
    /** 从点击拍照到 CameraX 返回文件为止；保存阶段不再阻塞下一次操作。 */
    val isCapturing: Boolean = false,
    /** 兼容模式关闭时启用 HDR 会话，开启时使用普通会话。 */
    val hdrEnabled: Boolean = true,
    val scanResult: ScannedContent? = null,
    /** 已识别出平台内容，正在等待外部应用打开结果。 */
    val autoLaunchPending: Boolean = false,
    /** 外部应用返回后暂停扫码，防止同一二维码被立即重复记录。 */
    val resumeRequired: Boolean = false,
    val writePhotoLocation: Boolean = false,
    val livePhotoEnabled: Boolean = false
)

/**
 * 相机页面向 UI 发送的一次性事件。
 *
 * 使用事件而不是把提示文本塞进 [CameraUiState]，可以避免配置变更后重复弹出提示。
 */
sealed interface CameraEvent {
    data class PhotoSaved(val photo: SavedPhoto) : CameraEvent

    /**
     * 显示在 TopAppBar 下方的短提示。
     *
     * @param showTimelineAction 是否提供“查看时光轴”操作。
     */
    data class TopPrompt(
        val text: String, val showTimelineAction: Boolean = false
    ) : CameraEvent

    data class Message(val text: String) : CameraEvent
}
