package com.ziegler.kighelper.ui.screens.history

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.data.camera.GalleryPhoto
import com.ziegler.kighelper.data.camera.MediaAccessState
import com.ziegler.kighelper.data.camera.MediaPermissions
import com.ziegler.kighelper.data.camera.TimelineAssembler
import com.ziegler.kighelper.data.camera.TimelineItem
import com.ziegler.kighelper.data.scan.ScanRecord
import com.ziegler.kighelper.data.scan.ScannedContentClassifier
import com.ziegler.kighelper.ui.TimelineEvent
import com.ziegler.kighelper.ui.TimelineViewModel
import com.ziegler.kighelper.ui.screens.camera.ScanResultDialog
import com.ziegler.kighelper.ui.utils.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

/** 时光轴的两种展示方式；缩略图模式使用自适应列数的网格。 */
private enum class TimelineLayoutMode {
    LIST, THUMBNAILS
}

/**
 * 时光轴：把相册照片和扫码记录合并为统一时间线。
 *
 * 页面负责锁屏保护、媒体权限、ContentObserver 刷新、多选和系统删除确认；
 * 数据合并与删除细节分别在 TimelineAssembler 和 TimelineViewModel 中。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    onBack: () -> Unit, viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    var layoutMode by rememberSaveable {
        mutableStateOf(TimelineLayoutMode.THUMBNAILS)
    }
    val showThumbnails = layoutMode == TimelineLayoutMode.THUMBNAILS
    val scope = rememberCoroutineScope()
    val keyguardManager = remember {
        context.getSystemService(KeyguardManager::class.java)
    }

    // 锁屏时不读取相册，避免照片在通知栏或最近任务预览中泄露。
    var isLocked by remember {
        mutableStateOf(keyguardManager?.isKeyguardLocked == true)
    }

    // 权限、旧版写权限和系统删除确认都由 ViewModel 发出事件后在这里启动。
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onMediaPermissionResult()
    }
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLegacyWritePermissionResult(granted)
    }
    val approvalLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteApprovalResult(result.resultCode == Activity.RESULT_OK)
    }

    // 解锁或从系统相册返回时重新检查锁屏状态并刷新。
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isLocked = keyguardManager?.isKeyguardLocked == true
        if (!isLocked) viewModel.refresh()
    }

    // 部分 ROM 解锁后不会补发 ON_RESUME，持续检测锁屏状态变化并主动刷新。
    LaunchedEffect(Unit) {
        while (true) {
            val locked = keyguardManager?.isKeyguardLocked == true
            if (locked != isLocked) {
                isLocked = locked
                if (!locked) viewModel.refresh()
            }
            delay(500L.milliseconds)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TimelineEvent.LaunchApproval -> {
                    try {
                        approvalLauncher.launch(
                            IntentSenderRequest.Builder(event.intentSender).build()
                        )
                    } catch (_: Exception) {
                        viewModel.onDeleteApprovalResult(false)
                    }
                }

                TimelineEvent.RequestWritePermission -> {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        writePermissionLauncher.launch(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }
                }

                is TimelineEvent.Message -> {
                    snackbarHostState.showSnackbar(event.text)
                }
            }
        }
    }

    // 解锁后首次进入时请求相册权限；用户拒绝后由页面卡片引导去系统设置。
    LaunchedEffect(isLocked) {
        if (!isLocked) {
            val access = MediaPermissions.current(context)
            if (access == MediaAccessState.DENIED && !viewModel.hasRequestedMediaPermission) {
                viewModel.markMediaPermissionRequested()
                mediaPermissionLauncher.launch(MediaPermissions.requestedPermissions)
            }
        }
    }

    // 相册在其他应用中被修改时自动刷新；锁屏期间不注册监听。
    DisposableEffect(isLocked) {
        if (isLocked) {
            return@DisposableEffect onDispose { }
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                viewModel.refresh()
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    // 锁屏状态优先于正常列表，避免任何照片或扫码记录出现在预览中。
    if (isLocked) {
        LockedTimelineScreen(
            onBack = onBack, onUnlock = {
                context.findActivity()?.let { activity ->
                    keyguardManager?.requestDismissKeyguard(activity, null)
                }
            })
        return
    }

    // 筛选和分组都是纯计算，仅在输入变化时重建。
    val visibleItems = remember(state.photos, state.scans, state.filter) {
        TimelineAssembler.merge(state.photos, state.scans, state.filter)
    }
    val groupedItems = remember(visibleItems) {
        visibleItems.groupBy { item ->
            if (item.timestamp <= 0L) null else {
                Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
    }
    // 查看器只切换当前筛选结果中的照片，顺序与时光轴一致。
    val visiblePhotos = remember(visibleItems) {
        visibleItems.filterIsInstance<TimelineItem.Photo>().map { it.photo }
    }
    // 多选时返回键先退出多选，而不是直接离开时光轴。
    BackHandler(enabled = state.selectionMode) {
        viewModel.exitSelection()
    }

    // 查看器与两个确认弹窗由页面本地状态管理，不进入全局 ViewModel。
    var selectedPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var selectedScan by remember { mutableStateOf<ScanRecord?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, floatingActionButton = {
            if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    }) {
                    Icon(Icons.Filled.VerticalAlignTop, contentDescription = "回到顶部")
                }
            }
        }, topBar = {
            TopAppBar(
                title = {
                Text(
                    text = if (state.selectionMode) {
                        "已选择 ${state.selection.totalCount} 项"
                    } else {
                        "时光轴"
                    }, fontWeight = FontWeight.SemiBold
                )
            }, navigationIcon = {
                IconButton(onClick = {
                    if (state.selectionMode) viewModel.exitSelection() else onBack()
                }) {
                    Icon(
                        imageVector = if (state.selectionMode) {
                            Icons.Filled.Close
                        } else {
                            Icons.AutoMirrored.Filled.ArrowBack
                        }, contentDescription = if (state.selectionMode) "退出多选" else "返回"
                    )
                }
            }, actions = {
                if (state.selectionMode) {
                    val allSelected =
                        visibleItems.isNotEmpty() && state.selection.totalCount == visibleItems.size
                    TextButton(
                        onClick = {
                            if (allSelected) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAll(visibleItems)
                            }
                        }, enabled = visibleItems.isNotEmpty() && !state.isDeleting
                    ) {
                        Text(if (allSelected) "取消全选" else "全选")
                    }
                } else {
                    TextButton(
                        onClick = viewModel::enterSelectionMode,
                        enabled = visibleItems.isNotEmpty()
                    ) {
                        Text("选择")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (layoutMode == TimelineLayoutMode.LIST) {
                                DropdownMenuItem(leadingIcon = {
                                    Icon(
                                        Icons.Filled.GridView, contentDescription = null
                                    )
                                }, text = { Text("缩略图模式") }, onClick = {
                                    layoutMode = TimelineLayoutMode.THUMBNAILS
                                    showMenu = false
                                })
                            } else {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.List,
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text("列表模式") },
                                    onClick = {
                                        layoutMode = TimelineLayoutMode.LIST
                                        showMenu = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete, contentDescription = null
                                )
                            },
                                text = { Text("清空扫码记录") },
                                enabled = state.scans.isNotEmpty(),
                                onClick = {
                                    showMenu = false
                                    showClearConfirm = true
                                })
                        }
                    }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
            )
        }, bottomBar = {
            if (state.selectionMode) {
                SelectionBottomBar(
                    count = state.selection.totalCount,
                    enabled = !state.isDeleting,
                    onDelete = { showDeleteConfirm = true })
            }
        }) { padding ->
            LazyVerticalGrid(
                columns = if (showThumbnails) {
                    GridCells.Adaptive(minSize = 96.dp)
                } else {
                    GridCells.Fixed(1)
                },
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.selectionMode) {
                    item(
                        key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                        TimelineFilterRow(
                            selected = state.filter,
                            photoCount = state.photos.size,
                            scanCount = state.scans.size,
                            enabled = !state.isDeleting,
                            onSelected = viewModel::setFilter
                        )
                    }

                    when (state.mediaAccess) {
                        MediaAccessState.DENIED -> {
                            item(
                                key = "media_permission", span = { GridItemSpan(maxLineSpan) }) {
                                MediaPermissionCard(onRequest = {
                                    viewModel.markMediaPermissionRequested()
                                    mediaPermissionLauncher.launch(
                                        MediaPermissions.requestedPermissions
                                    )
                                }, onOpenSettings = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts(
                                                "package", context.packageName, null
                                            )
                                        )
                                    )
                                })
                            }
                        }

                        MediaAccessState.PARTIAL -> {
                            item(
                                key = "media_partial", span = { GridItemSpan(maxLineSpan) }) {
                                PartialAccessBanner(
                                    onManage = {
                                        mediaPermissionLauncher.launch(
                                            MediaPermissions.requestedPermissions
                                        )
                                    })
                            }
                        }

                        MediaAccessState.FULL -> Unit
                    }

                    if (state.isLoading) {
                        item(
                            key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("正在读取相册…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    state.errorMessage?.let { message ->
                        item(
                            key = "error", span = { GridItemSpan(maxLineSpan) }) {
                            Card {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = message,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    IconButton(onClick = viewModel::refresh) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "重试")
                                    }
                                }
                            }
                        }
                    }
                }

                groupedItems.forEach { (date, group) ->
                    item(
                        key = "date:${date ?: "unknown"}", span = { GridItemSpan(maxLineSpan) }) {
                        DateHeader(date)
                    }
                    items(group, key = { it.stableKey }) { item ->
                        val selected = when (item) {
                            is TimelineItem.Photo -> item.photo.id in state.selection.photoIds
                            is TimelineItem.Scan -> item.record.id in state.selection.scanIds
                        }
                        val onClick = {
                            if (state.selectionMode) {
                                viewModel.toggleSelection(item)
                            } else {
                                when (item) {
                                    is TimelineItem.Photo -> selectedPhoto = item.photo
                                    is TimelineItem.Scan -> selectedScan = item.record
                                }
                            }
                        }
                        val onLongClick = {
                            if (state.selectionMode) {
                                viewModel.toggleSelection(item)
                            } else {
                                viewModel.enterSelection(item)
                            }
                        }
                        if (showThumbnails) {
                            TimelineThumbnail(
                                item = item,
                                selectionMode = state.selectionMode,
                                selected = selected,
                                onClick = onClick,
                                onLongClick = onLongClick
                            )
                        } else {
                            TimelineRow(
                                item = item,
                                selectionMode = state.selectionMode,
                                selected = selected,
                                onClick = onClick,
                                onLongClick = onLongClick
                            )
                        }
                    }
                }

                if (visibleItems.isEmpty() && !state.isLoading) {
                    item(
                        key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyTimeline()
                    }
                }
            }
        }

        // 系统删除确认期间阻止用户继续操作列表。
        if (state.isDeleting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    selectedPhoto?.let { photo ->
        TimelinePhotoViewer(
            photos = visiblePhotos, initialPhotoId = photo.id, onDismiss = { selectedPhoto = null })
    }

    // 历史记录只存摘要，打开时再分类恢复完整详情。
    selectedScan?.let { record ->
        val content = remember(record) {
            ScannedContentClassifier.fromRecord(record)
        }
        ScanResultDialog(
            content = content, onDismiss = { selectedScan = null }, dismissLabel = "关闭"
        )
    }

    // 删除前先说明影响范围；Android 11+ 之后系统还会再弹一次删除确认。
    if (showDeleteConfirm) {
        val photoCount = state.selection.photoCount
        val scanCount = state.selection.scanCount
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除所选内容") },
            text = {
                Text(
                    buildString {
                        append("将删除 $photoCount 张图片和 $scanCount 条扫码记录。\n\n")
                        if (photoCount > 0) {
                            append("图片会从本地相册永久删除")
                        }
                        if (photoCount > 0 && scanCount > 0) {
                            append("，")
                        }
                        if (scanCount > 0) {
                            append("扫码记录会从时光轴移除")
                        }
                        append("，无法恢复。")
                    })
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.requestDeleteSelected()
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            })
    }

    // 清空扫码历史只影响 JSON 记录，不删除相册照片。
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空扫码记录") },
            text = {
                Text(
                    "将清空 ${state.scans.size} 条扫码记录。\n\n此操作无法恢复。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearScanHistory()
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            })
    }
}
