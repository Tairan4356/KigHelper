package com.ziegler.kighelper.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid
import java.io.File

private const val TAB_TEXT = "text"
private const val TAB_IMAGE = "image"
private const val TAB_VIDEO = "video"

/**
 * 添加/编辑短语表单。
 * 只接收数据和回调，避免页面直接依赖 ViewModel。
 * 播报内容通过 文字/图片/视频 三个 Tab 互斥设置；切换 Tab 仅改变编辑视图，内容在点击保存时按所选 Tab 写入并清理其他类型。
 *
 * @param phrase 待编辑的短语，null 表示新增模式
 * @param isEditMode 是否为编辑模式
 * @param groups 可选分组列表
 * @param initialGroupId 初始选中的分组 ID
 * @param onSave 保存回调，提供标签、播报内容、分组ID、音频/图片/视频路径和自定义颜色
 * @param onBack 返回回调
 * @param onAudioImported 音频导入回调，返回导入后的内部文件路径
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPhraseScreen(
    phrase: Phrase?,
    isEditMode: Boolean,
    groups: List<PhraseGroup>,
    initialGroupId: String? = null,
    onSave: (label: String, speech: String, groupId: String, audioPath: String?, cardColor: Long?, imagePath: String?, videoPath: String?) -> Unit,
    onBack: () -> Unit,
    onAudioImported: ((Uri, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val initialTab = remember(phrase?.id) { phraseContentTab(phrase) }

    var label by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.label.orEmpty())
    }
    var speech by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.speech.orEmpty())
    }
    var selectedGroupId by rememberSaveable(phrase?.id, initialGroupId) {
        mutableStateOf(phrase?.groupId ?: initialGroupId ?: PhraseGroup.DEFAULT_ID)
    }
    var groupMenuExpanded by remember { mutableStateOf(false) }

    // 内容类型 Tab：文字/图片/视频 互斥（内容仅在选择保存后写入，切换 Tab 不删除）
    var selectedTab by rememberSaveable(phrase?.id) { mutableStateOf(initialTab) }

    // 音频状态（独立于 Tab，仅视频 Tab 隐藏，保存为视频时清除）
    var audioPath by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.audioPath)
    }
    var audioFileName by rememberSaveable(phrase?.id) {
        mutableStateOf(audioPath?.let { File(it).name })
    }

    // 图片状态
    var imagePath by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.imagePath)
    }
    var imageFileName by rememberSaveable(phrase?.id) {
        mutableStateOf(imagePath?.let { File(it).name })
    }

    // 视频状态
    var videoPath by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.videoPath)
    }
    var videoFileName by rememberSaveable(phrase?.id) {
        mutableStateOf(videoPath?.let { File(it).name })
    }

    // 颜色状态
    var cardColor by rememberSaveable(phrase?.id) {
        mutableLongStateOf(phrase?.cardColor ?: 0L)
    }
    var hasCustomColor by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.cardColor != null)
    }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    // 媒体选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val imported = importMediaFile(context, it, "images", phrase?.id)
            if (imported != null) {
                imagePath = imported.first
                imageFileName = imported.second
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val imported = importMediaFile(context, it, "videos", phrase?.id)
            if (imported != null) {
                videoPath = imported.first
                videoFileName = imported.second
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val imported = importMediaFile(context, it, "audio", phrase?.id)
            if (imported != null) {
                audioPath = imported.first
                audioFileName = imported.second
            }
        }
    }

    // 切换内容 Tab：仅切换选中项，保留各内容，保存时按 Tab 应用互斥写入
    fun switchTab(target: String) {
        if (target == selectedTab) return
        selectedTab = target
    }

    val groupSnapshot = groups.toList()
    val sortedGroups = remember(groupSnapshot) {
        groupSnapshot.distinctBy { it.id }.sortedBy { it.order }
    }
    val selectedGroupName =
        sortedGroups.firstOrNull { it.id == selectedGroupId }?.name ?: PhraseGroup.DEFAULT_NAME

    LaunchedEffect(sortedGroups, selectedGroupId) {
        if (sortedGroups.isNotEmpty() && sortedGroups.none { it.id == selectedGroupId }) {
            selectedGroupId = sortedGroups.firstOrNull { it.id == PhraseGroup.DEFAULT_ID }?.id
                ?: sortedGroups.first().id
        }
    }

    val contentFilled = when (selectedTab) {
        TAB_TEXT -> speech.isNotBlank()
        TAB_IMAGE -> imagePath != null
        TAB_VIDEO -> videoPath != null
        else -> false
    }

    fun performSave() {
        // 保存时应用 Tab 互斥：删除被替换的媒体文件，只写入当前 Tab 的内容
        when (selectedTab) {
            TAB_TEXT -> {
                imagePath?.let { File(it).delete() }
                videoPath?.let { File(it).delete() }
            }

            TAB_IMAGE -> {
                videoPath?.let { File(it).delete() }
            }

            TAB_VIDEO -> {
                imagePath?.let { File(it).delete() }
                audioPath?.let { File(it).delete() }
            }
        }
        val finalColor = if (hasCustomColor && cardColor != 0L) cardColor else null
        val (finalSpeech, finalImage, finalVideo) = when (selectedTab) {
            TAB_TEXT -> Triple(speech, null, null)
            TAB_IMAGE -> Triple("", imagePath, null)
            else -> Triple("", null, videoPath)
        }
        val finalAudio = if (selectedTab == TAB_VIDEO) null else audioPath
        onSave(
            label, finalSpeech, selectedGroupId, finalAudio, finalColor, finalImage, finalVideo
        )
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (isEditMode) "编辑短语" else "添加短语") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
            }
        })
    }, bottomBar = {
        Button(
            onClick = { performSave() },
            enabled = label.isNotBlank() && contentFilled,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("保存")
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = groupMenuExpanded,
                onExpandedChange = { groupMenuExpanded = !groupMenuExpanded }) {
                OutlinedTextField(
                    value = selectedGroupName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分组") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = groupMenuExpanded,
                    onDismissRequest = { groupMenuExpanded = false }) {
                    sortedGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) }, onClick = {
                            selectedGroupId = group.id
                            groupMenuExpanded = false
                        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("按钮标签") },
                modifier = Modifier.fillMaxWidth()
            )

            // 播报内容：文字/图片/视频 三选一
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "播报内容",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrimaryTabRow(selectedTabIndex = tabIndex(selectedTab)) {
                    Tab(
                        selected = selectedTab == TAB_TEXT,
                        onClick = { switchTab(TAB_TEXT) },
                        text = { Text("文字") },
                        icon = { Icon(Icons.Default.TextFields, contentDescription = null) })
                    Tab(
                        selected = selectedTab == TAB_IMAGE,
                        onClick = { switchTab(TAB_IMAGE) },
                        text = { Text("图片") },
                        icon = { Icon(Icons.Default.Image, contentDescription = null) })
                    Tab(
                        selected = selectedTab == TAB_VIDEO,
                        onClick = { switchTab(TAB_VIDEO) },
                        text = { Text("视频") },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = null) })
                }

                when (selectedTab) {
                    TAB_TEXT -> OutlinedTextField(
                        value = speech,
                        onValueChange = { speech = it },
                        label = { Text("播报文字") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    TAB_IMAGE -> ImageImportSection(
                        imagePath = imagePath,
                        imageFileName = imageFileName,
                        onPick = { imagePickerLauncher.launch(arrayOf("image/*", "image/gif")) },
                        onRemove = {
                            imagePath?.let { File(it).delete() }
                            imagePath = null
                            imageFileName = null
                        })

                    TAB_VIDEO -> VideoImportSection(
                        videoPath = videoPath,
                        videoFileName = videoFileName,
                        onPick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                        onRemove = {
                            videoPath?.let { File(it).delete() }
                            videoPath = null
                            videoFileName = null
                        })
                }
            }

            // 音频导入区域（独立于内容 Tab，视频 Tab 隐藏；保存为视频时清除音频）
            if (selectedTab != TAB_VIDEO) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "音频文件",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (audioFileName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = audioFileName ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                audioPath?.let { File(it).delete() }
                                audioPath = null
                                audioFileName = null
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "移除音频"
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                            }, modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AudioFile, contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导入音频文件")
                        }
                    }

                    Text(
                        text = "导入音频后，点击短语将直接播放音频而非使用TTS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 卡片颜色区域
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "卡片颜色",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasCustomColor) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CustomColorSelector(
                            customColor = if (cardColor != 0L) cardColor else 0xFF6650A4,
                            onClick = { showColorPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            hasCustomColor = false
                            cardColor = 0L
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "恢复默认颜色"
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            hasCustomColor = true
                            if (cardColor == 0L) cardColor = 0xFF6650A4
                        }, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("自定义卡片颜色")
                    }
                }

                if (hasCustomColor) {
                    PresetColorGrid(
                        selectedIndex = -1, onColorSelected = { index ->
                            val colors = listOf(
                                0xFF6650A4L,
                                0xFF2196F3L,
                                0xFF00BCD4L,
                                0xFF4CAF50L,
                                0xFFFFEB3BL,
                                0xFFFF9800L,
                                0xFFF44336L,
                                0xFFE91E63L
                            )
                            if (index in colors.indices) {
                                cardColor = colors[index]
                            }
                        }, showNames = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = if (cardColor != 0L) cardColor else 0xFF6650A4,
            onColorSelected = { color ->
                cardColor = color
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false })
    }
}

@Composable
private fun ImageImportSection(
    imagePath: String?, imageFileName: String?, onPick: () -> Unit, onRemove: () -> Unit
) {
    if (imagePath != null) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = imageFileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = imageFileName ?: "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onRemove) {
                Text("移除图片")
            }
        }
        Text(
            text = "上传图片（支持 GIF）后将显示在展示区，播报内容可为空",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedButton(
            onClick = onPick, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导入图片 (支持 GIF)")
        }
    }
}

@Composable
private fun VideoImportSection(
    videoPath: String?, videoFileName: String?, onPick: () -> Unit, onRemove: () -> Unit
) {
    if (videoPath != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = videoFileName ?: "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "移除视频")
            }
        }
        Text(
            text = "设置视频后将全屏播放并使用视频声音，播报内容可为空",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedButton(
            onClick = onPick, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Videocam, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导入视频文件")
        }
    }
}

private fun phraseContentTab(phrase: Phrase?): String = when {
    phrase?.hasVideo == true -> TAB_VIDEO
    phrase?.hasImage == true -> TAB_IMAGE
    else -> TAB_TEXT
}

private fun tabIndex(tab: String): Int = when (tab) {
    TAB_IMAGE -> 1
    TAB_VIDEO -> 2
    else -> 0
}

/**
 * 将选择的媒体文件复制到内部目录，返回 (绝对路径, 文件名)；失败返回 null。
 */
private fun importMediaFile(
    context: Context, uri: Uri, dirName: String, phraseId: String?
): Pair<String, String>? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val fileName = if (cursor != null && nameIndex != null && cursor.moveToFirst()) {
        cursor.getString(nameIndex) ?: "${dirName}_${System.currentTimeMillis()}.bin"
    } else {
        "${dirName}_${System.currentTimeMillis()}.bin"
    }
    cursor?.close()

    val dir = File(context.filesDir, dirName)
    dir.mkdirs()
    val destFile = File(dir, "${phraseId ?: System.currentTimeMillis()}_$fileName")

    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath to destFile.name
    } catch (_: Exception) {
        null
    }
}