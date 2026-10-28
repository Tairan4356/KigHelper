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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ziegler.kighelper.data.camera.GalleryPhoto
import com.ziegler.kighelper.data.camera.MediaAccessState
import com.ziegler.kighelper.data.camera.MediaPermissions
import com.ziegler.kighelper.data.camera.TimelineAssembler
import com.ziegler.kighelper.data.camera.TimelineFilter
import com.ziegler.kighelper.data.camera.TimelineItem
import com.ziegler.kighelper.data.scan.ScanRecord
import com.ziegler.kighelper.data.scan.ScanRecordType
import com.ziegler.kighelper.data.scan.ScannedContentClassifier
import com.ziegler.kighelper.ui.TimelineEvent
import com.ziegler.kighelper.ui.TimelineViewModel
import com.ziegler.kighelper.ui.screens.camera.ScanResultDialog
import com.ziegler.kighelper.ui.utils.findActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// 时间线和分组标题使用不同格式，避免每行重复创建 Formatter。
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

/**
 * 时光轴：把相册照片和扫码记录合并为统一时间线。
 *
 * 页面负责锁屏保护、媒体权限、ContentObserver 刷新、多选和系统删除确认；
 * 数据合并与删除细节分别在 TimelineAssembler 和 TimelineViewModel 中。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    onBack: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
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
            if (access == MediaAccessState.DENIED &&
                !viewModel.hasRequestedMediaPermission
            ) {
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
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    // 锁屏状态优先于正常列表，避免任何照片或扫码记录出现在预览中。
    if (isLocked) {
        LockedTimelineScreen(
            onBack = onBack,
            onUnlock = {
                context.findActivity()?.let { activity ->
                    keyguardManager?.requestDismissKeyguard(activity, null)
                }
            }
        )
        return
    }

    // 筛选和分组都是纯计算，仅在输入变化时重建。
    val visibleItems = remember(state.photos, state.scans, state.filter) {
        TimelineAssembler.merge(state.photos, state.scans, state.filter)
    }
    val groupedItems = remember(visibleItems) {
        visibleItems.groupBy { item ->
            if (item.timestamp <= 0L) null else {
                Instant.ofEpochMilli(item.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
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
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (state.selectionMode) {
                                "已选择 ${state.selection.totalCount} 项"
                            } else {
                                "时光轴"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.selectionMode) viewModel.exitSelection() else onBack()
                        }) {
                            Icon(
                                imageVector = if (state.selectionMode) {
                                    Icons.Filled.Close
                                } else {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                },
                                contentDescription = if (state.selectionMode) "退出多选" else "返回"
                            )
                        }
                    },
                    actions = {
                        if (state.selectionMode) {
                            val allSelected = visibleItems.isNotEmpty() &&
                                state.selection.totalCount == visibleItems.size
                            TextButton(
                                onClick = {
                                    if (allSelected) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAll(visibleItems)
                                    }
                                },
                                enabled = visibleItems.isNotEmpty() && !state.isDeleting
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
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("清空扫码记录") },
                                        enabled = state.scans.isNotEmpty(),
                                        onClick = {
                                            showMenu = false
                                            showClearConfirm = true
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (state.selectionMode) {
                    SelectionBottomBar(
                        count = state.selection.totalCount,
                        enabled = !state.isDeleting,
                        onDelete = { showDeleteConfirm = true }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.selectionMode) {
                    item(key = "filters") {
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
                            item(key = "media_permission") {
                                MediaPermissionCard(
                                    onRequest = {
                                        viewModel.markMediaPermissionRequested()
                                        mediaPermissionLauncher.launch(
                                            MediaPermissions.requestedPermissions
                                        )
                                    },
                                    onOpenSettings = {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts(
                                                    "package",
                                                    context.packageName,
                                                    null
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        MediaAccessState.PARTIAL -> {
                            item(key = "media_partial") {
                                PartialAccessBanner(
                                    onManage = {
                                        mediaPermissionLauncher.launch(
                                            MediaPermissions.requestedPermissions
                                        )
                                    }
                                )
                            }
                        }

                        MediaAccessState.FULL -> Unit
                    }

                    if (state.isLoading) {
                        item(key = "loading") {
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
                        item(key = "error") {
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
                    item(key = "date:${date ?: "unknown"}") {
                        DateHeader(date)
                    }
                    items(group, key = { it.stableKey }) { item ->
                        TimelineRow(
                            item = item,
                            selectionMode = state.selectionMode,
                            selected = when (item) {
                                is TimelineItem.Photo -> item.photo.id in state.selection.photoIds
                                is TimelineItem.Scan -> item.record.id in state.selection.scanIds
                            },
                            onClick = {
                                if (state.selectionMode) {
                                    viewModel.toggleSelection(item)
                                } else {
                                    when (item) {
                                        is TimelineItem.Photo -> selectedPhoto = item.photo
                                        is TimelineItem.Scan -> selectedScan = item.record
                                    }
                                }
                            },
                            onLongClick = {
                                if (state.selectionMode) {
                                    viewModel.toggleSelection(item)
                                } else {
                                    viewModel.enterSelection(item)
                                }
                            }
                        )
                    }
                }

                if (visibleItems.isEmpty() && !state.isLoading) {
                    item(key = "empty") {
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
        TimelinePhotoViewer(photo = photo, onDismiss = { selectedPhoto = null })
    }

    // 历史记录只存摘要，打开时再分类恢复完整详情。
    selectedScan?.let { record ->
        val content = remember(record) {
            ScannedContentClassifier.fromRecord(record)
        }
        ScanResultDialog(
            content = content,
            onDismiss = { selectedScan = null },
            dismissLabel = "关闭"
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
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.requestDeleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
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
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

/** 全部/照片/扫码三个筛选 Chip；数量直接来自当前完整数据。 */
@Composable
private fun TimelineFilterRow(
    selected: TimelineFilter,
    photoCount: Int,
    scanCount: Int,
    enabled: Boolean,
    onSelected: (TimelineFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == TimelineFilter.ALL,
            onClick = { onSelected(TimelineFilter.ALL) },
            label = { Text("全部 ${photoCount + scanCount}") },
            enabled = enabled
        )
        FilterChip(
            selected = selected == TimelineFilter.PHOTOS,
            onClick = { onSelected(TimelineFilter.PHOTOS) },
            label = { Text("照片 $photoCount") },
            enabled = enabled
        )
        FilterChip(
            selected = selected == TimelineFilter.SCANS,
            onClick = { onSelected(TimelineFilter.SCANS) },
            label = { Text("扫码 $scanCount") },
            enabled = enabled
        )
    }
}

/** 完全无相册权限时的说明卡片。 */
@Composable
private fun MediaPermissionCard(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("允许访问相册", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "授权后会显示相册中的全部照片。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequest) { Text("允许访问") }
                TextButton(onClick = onOpenSettings) { Text("系统设置") }
            }
        }
    }
}

/** Android 14 仅授权部分照片时的提示，可再次打开系统照片选择器。 */
@Composable
private fun PartialAccessBanner(onManage: () -> Unit) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前仅显示已授权的照片",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onManage) { Text("管理照片访问") }
        }
    }
}

/** 日期分组标题，今天/昨天使用相对文案。 */
@Composable
private fun DateHeader(date: LocalDate?) {
    Text(
        text = dateLabel(date),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

/**
 * 单条时光轴内容。
 *
 * 短按查看详情，长按进入多选；多选状态下右侧显示复选框。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineRow(
    item: TimelineItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (item) {
                is TimelineItem.Photo -> PhotoLeading(item.photo)
                is TimelineItem.Scan -> ScanLeading(item.record)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (item) {
                        is TimelineItem.Photo -> item.photo.displayName
                        is TimelineItem.Scan -> item.record.title
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (item) {
                        is TimelineItem.Photo -> item.photo.bucketName ?: "相册"
                        is TimelineItem.Scan -> item.record.subtitle
                            ?: item.record.rawContent
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.timestamp > 0L) {
                    Text(
                        text = Instant.ofEpochMilli(item.timestamp)
                            .atZone(ZoneId.systemDefault())
                            .format(timeFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            }
        }
    }
}

/** 固定解码到 180px 的缩略图，避免列表滚动时加载原图。 */
@Composable
private fun PhotoLeading(photo: GalleryPhoto) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(photo.uri)
            .size(180)
            .build(),
        contentDescription = photo.displayName,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

/** 使用内容类型图标占位，避免扫码记录依赖网络缩略图。 */
@Composable
private fun ScanLeading(record: ScanRecord) {
    val icon = when (record.type) {
        ScanRecordType.SOCIAL_CARD -> Icons.Filled.QrCode
        ScanRecordType.PLATFORM -> Icons.Filled.Link
        ScanRecordType.TEXT -> Icons.AutoMirrored.Filled.TextSnippet
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** 多选底部操作栏；删除按钮由页面统一确认。 */
@Composable
private fun SelectionBottomBar(
    count: Int,
    enabled: Boolean,
    onDelete: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选择 $count 项",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onDelete,
                enabled = enabled && count > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("删除")
            }
        }
    }
}

/** 无照片且无扫码记录时的空态。 */
@Composable
private fun EmptyTimeline() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("暂无内容", color = MaterialTheme.colorScheme.outline)
    }
}

/** 锁屏占位页；不读取相册，只提供返回和解锁操作。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockedTimelineScreen(
    onBack: () -> Unit,
    onUnlock: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时光轴") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "时光轴已锁定，请解锁设备后查看。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("解锁") }
        }
    }
}


/** 把分组日期格式化为今天、昨天或完整日期。 */
private fun dateLabel(date: LocalDate?): String {
    if (date == null) return "未知日期"
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(dateFormatter)
    }
}
