package com.ziegler.kighelper.ui

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.camera.DirectDeleteAttempt
import com.ziegler.kighelper.data.camera.GalleryPhoto
import com.ziegler.kighelper.data.camera.MediaAccessState
import com.ziegler.kighelper.data.camera.MediaPermissions
import com.ziegler.kighelper.data.camera.MediaStorePhotoDeleter
import com.ziegler.kighelper.data.camera.MediaStorePhotoRepository
import com.ziegler.kighelper.data.camera.TimelineFilter
import com.ziegler.kighelper.data.camera.TimelineItem
import com.ziegler.kighelper.data.camera.TimelineSelection
import com.ziegler.kighelper.data.scan.ScanHistoryRepository
import com.ziegler.kighelper.data.scan.ScanRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 时光轴页面状态；照片与扫码记录分开保存，最终由页面合并展示。 */
data class TimelineUiState(
    val photos: List<GalleryPhoto> = emptyList(),
    val scans: List<ScanRecord> = emptyList(),
    val mediaAccess: MediaAccessState = MediaAccessState.DENIED,
    val filter: TimelineFilter = TimelineFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectionMode: Boolean = false,
    val selection: TimelineSelection = TimelineSelection(),
    val isDeleting: Boolean = false
)

/**
 * 需要由 Activity/Composable 处理的一次性事件。
 *
 * 删除确认、旧版存储权限和 Snackbar 都带有系统 UI，不能只放在 UI 状态里。
 */
sealed interface TimelineEvent {
    data class LaunchApproval(val intentSender: IntentSender) : TimelineEvent
    data object RequestWritePermission : TimelineEvent
    data class Message(val text: String) : TimelineEvent
}

/**
 * 时光轴状态与照片/扫码记录的删除流程。
 *
 * 照片删除因系统版本分为三条路径：Android 11+ 批量确认、Android 10 单张可恢复
 * 异常、Android 9 及以下旧版写权限；扫码记录则直接操作本地 JSON 仓库。
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val photoRepository: MediaStorePhotoRepository,
    private val photoDeleter: MediaStorePhotoDeleter,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        TimelineUiState(scans = scanHistoryRepository.current)
    )
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TimelineEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TimelineEvent> = _events.asSharedFlow()

    /** 避免每次回到页面都重复弹系统相册权限框。 */
    var hasRequestedMediaPermission: Boolean = false
        private set

    /** 保存等待系统确认的删除请求，授权返回后继续执行。 */
    private var pendingDeletion: PendingDeletion? = null

    init {
        // 扫码历史可能在相机页新增；持续同步，并清掉已不存在的选中 ID。
        viewModelScope.launch {
            scanHistoryRepository.records.collect { scans ->
                _state.update {
                    it.copy(
                        scans = scans, selection = it.selection.copy(
                            scanIds = it.selection.scanIds.intersect(
                                scans.map { record -> record.id }.toSet()
                            )
                        )
                    )
                }
            }
        }
    }

    fun markMediaPermissionRequested() {
        hasRequestedMediaPermission = true
    }

    fun onMediaPermissionResult() {
        refresh()
    }

    /**
     * 重新读取权限和相册。
     *
     * 无权限时清空照片但保留扫码记录；有权限时同时清理已删除照片的选中状态。
     */
    fun refresh() {
        viewModelScope.launch {
            val access = MediaPermissions.current(context)
            _state.update {
                it.copy(
                    mediaAccess = access,
                    isLoading = access != MediaAccessState.DENIED && it.photos.isEmpty()
                )
            }
            if (access == MediaAccessState.DENIED) {
                _state.update {
                    it.copy(
                        photos = emptyList(),
                        isLoading = false,
                        errorMessage = null,
                        selection = it.selection.copy(photoIds = emptySet())
                    )
                }
                return@launch
            }
            val photos = runCatching { photoRepository.loadPhotos() }.getOrElse { error ->
                _state.update {
                    it.copy(
                        photos = emptyList(),
                        isLoading = false,
                        errorMessage = error.message ?: "无法读取相册"
                    )
                }
                return@launch
            }
            val validPhotoIds = photos.map { photo -> photo.id }.toSet()
            _state.update {
                it.copy(
                    photos = photos,
                    isLoading = false,
                    errorMessage = null,
                    selection = it.selection.copy(
                        photoIds = it.selection.photoIds.intersect(validPhotoIds)
                    )
                )
            }
        }
    }

    /** 多选模式下不允许切换筛选，避免选中项与可见列表不一致。 */
    fun setFilter(filter: TimelineFilter) {
        if (_state.value.selectionMode) return
        _state.update { it.copy(filter = filter) }
    }

    /** 进入多选但保留当前筛选；顶部“选择”按钮使用。 */
    fun enterSelectionMode() {
        _state.update { it.copy(selectionMode = true) }
    }

    /** 长按某条内容进入多选，并立即选中该条。 */
    fun enterSelection(item: TimelineItem) {
        _state.update {
            it.copy(
                selectionMode = true, selection = TimelineSelection().toggle(item)
            )
        }
    }

    /** 多选模式下点击行或复选框时切换选中状态。 */
    fun toggleSelection(item: TimelineItem) {
        _state.update {
            if (!it.selectionMode) it else it.copy(selection = it.selection.toggle(item))
        }
    }

    /** 退出多选并清空所有选择。 */
    fun exitSelection() {
        _state.update {
            it.copy(selectionMode = false, selection = TimelineSelection())
        }
    }

    fun selectAll(items: List<TimelineItem>) {
        _state.update {
            it.copy(selectionMode = true, selection = TimelineSelection().selectAll(items))
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selection = TimelineSelection()) }
    }

    /**
     * 清空扫码历史，不删除任何照片。
     *
     * 记录数量在启动协程前读取，避免删除过程中状态变化导致提示数字不准确。
     */
    fun clearScanHistory() {
        val count = _state.value.scans.size
        if (count == 0) return
        viewModelScope.launch {
            val result = runCatching { scanHistoryRepository.clear() }
            _state.update {
                it.copy(selectionMode = false, selection = TimelineSelection())
            }
            _events.emit(
                if (result.isSuccess) {
                    TimelineEvent.Message("已清空 $count 条扫码记录")
                } else {
                    TimelineEvent.Message("清空扫码记录失败")
                }
            )
        }
    }

    /**
     * 删除当前选中内容。
     *
     * 先按系统能力选择删除路径；扫码记录会跟随照片删除一起处理，照片删除失败时
     * 仍会尝试删除扫码记录。
     */
    fun requestDeleteSelected() {
        val state = _state.value
        if (state.isDeleting || state.selection.isEmpty) return

        val selectedPhotoIds = state.selection.photoIds
        val selectedPhotos = state.photos.filter { it.id in selectedPhotoIds }
        val selectedScanIds = state.selection.scanIds

        // 没有照片时无需系统权限，直接删除扫码记录。
        if (selectedPhotos.isEmpty()) {
            viewModelScope.launch {
                val result = runCatching { scanHistoryRepository.delete(selectedScanIds) }
                _state.update {
                    it.copy(selectionMode = false, selection = TimelineSelection())
                }
                _events.emit(
                    if (result.isSuccess) {
                        TimelineEvent.Message("已删除 ${selectedScanIds.size} 条扫码记录")
                    } else {
                        TimelineEvent.Message("删除扫码记录失败")
                    }
                )
            }
            return
        }

        val uris = selectedPhotos.map { it.uri }
        // Android 11+：一次系统确认可删除多张，Activity 返回后走 onDeleteApprovalResult。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intentSender = photoDeleter.createSystemDeleteRequest(uris)
            if (intentSender == null) {
                _events.tryEmit(TimelineEvent.Message("无法发起删除请求"))
                return
            }
            pendingDeletion = PendingDeletion.System(
                scanIds = selectedScanIds, photoCount = uris.size
            )
            _state.update { it.copy(isDeleting = true) }
            _events.tryEmit(TimelineEvent.LaunchApproval(intentSender))
            return
        }

        // Android 9 及以下直接删除公共目录前需要旧版写权限。
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !photoDeleter.hasLegacyWritePermission()) {
            pendingDeletion = PendingDeletion.LegacyWrite(
                uris = uris, scanIds = selectedScanIds, photoCount = uris.size
            )
            _events.tryEmit(TimelineEvent.RequestWritePermission)
            return
        }

        startDirectDelete(uris, selectedScanIds)
    }

    /**
     * 处理系统批量删除或单张删除的确认结果。
     *
     * 批准后继续完成剩余扫码记录删除；取消则恢复界面并刷新。
     */
    fun onDeleteApprovalResult(approved: Boolean) {
        when (val pending = pendingDeletion) {
            is PendingDeletion.System -> {
                pendingDeletion = null
                if (approved) {
                    finishDeletion(
                        scanIds = pending.scanIds, photoCount = pending.photoCount, failedPhotos = 0
                    )
                } else {
                    cancelDeletion()
                }
            }

            is PendingDeletion.Recoverable -> {
                pendingDeletion = null
                if (approved) {
                    startDirectDelete(pending.uris, pending.scanIds)
                } else {
                    cancelDeletion()
                }
            }

            is PendingDeletion.LegacyWrite, null -> Unit
        }
    }

    /** 旧版存储权限返回后继续删除；拒绝时保留原照片并提示。 */
    fun onLegacyWritePermissionResult(granted: Boolean) {
        val pending = pendingDeletion as? PendingDeletion.LegacyWrite ?: return
        pendingDeletion = null
        if (granted) {
            startDirectDelete(pending.uris, pending.scanIds)
        } else {
            pendingDeletion = null
            _state.update { it.copy(isDeleting = false) }
            _events.tryEmit(TimelineEvent.Message("未授予存储权限，未删除"))
            refresh()
        }
    }

    /**
     * 逐张直接删除。
     *
     * 遇到 Android 10 的可恢复安全异常时暂停剩余删除，等待用户确认后重新执行；
     * 最终根据本轮成功和失败数量生成提示。
     */
    private fun startDirectDelete(uris: List<Uri>, scanIds: Set<String>) {
        _state.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            var failed = 0
            for (uri in uris) {
                when (val attempt = photoDeleter.deleteOne(uri)) {
                    DirectDeleteAttempt.Deleted -> Unit
                    is DirectDeleteAttempt.NeedsApproval -> {
                        pendingDeletion = PendingDeletion.Recoverable(
                            uris = uris, scanIds = scanIds
                        )
                        _events.emit(TimelineEvent.LaunchApproval(attempt.intentSender))
                        return@launch
                    }

                    DirectDeleteAttempt.Failed -> failed += 1
                }
            }
            finishDeletion(
                scanIds = scanIds,
                photoCount = (uris.size - failed).coerceAtLeast(0),
                failedPhotos = failed
            )
        }
    }

    /** 删除照片后同步清理扫码记录、刷新列表并发出结果提示。 */
    private fun finishDeletion(
        scanIds: Set<String>, photoCount: Int, failedPhotos: Int
    ) {
        viewModelScope.launch {
            val scanDeleteResult = if (scanIds.isNotEmpty()) {
                runCatching { scanHistoryRepository.delete(scanIds) }
            } else {
                Result.success(Unit)
            }
            _state.update {
                it.copy(
                    selectionMode = false, selection = TimelineSelection(), isDeleting = false
                )
            }
            refresh()
            val failedSuffix = if (failedPhotos > 0) {
                "，${failedPhotos} 张图片删除失败"
            } else {
                ""
            }
            val scanSuffix = if (scanIds.isNotEmpty() && scanDeleteResult.isSuccess) {
                "、${scanIds.size} 条扫码记录"
            } else {
                ""
            }
            val scanFailureSuffix = if (scanIds.isNotEmpty() && scanDeleteResult.isFailure) {
                "，扫码记录删除失败"
            } else {
                ""
            }
            _events.emit(
                TimelineEvent.Message(
                    "已删除 $photoCount 张图片$scanSuffix$failedSuffix$scanFailureSuffix"
                )
            )
        }
    }

    /** 用户取消系统删除确认时恢复界面并刷新，避免列表停留在删除中。 */
    private fun cancelDeletion() {
        pendingDeletion = null
        _state.update { it.copy(isDeleting = false) }
        _events.tryEmit(TimelineEvent.Message("已取消删除"))
        refresh()
    }

    /**
     * 等待系统回调的删除上下文。
     *
     * 三种类型对应不同 Android 版本的删除流程，返回时据此决定继续、取消或忽略。
     */
    private sealed interface PendingDeletion {
        data class System(
            val scanIds: Set<String>, val photoCount: Int
        ) : PendingDeletion

        data class Recoverable(
            val uris: List<Uri>, val scanIds: Set<String>
        ) : PendingDeletion

        data class LegacyWrite(
            val uris: List<Uri>, val scanIds: Set<String>, val photoCount: Int
        ) : PendingDeletion
    }
}
