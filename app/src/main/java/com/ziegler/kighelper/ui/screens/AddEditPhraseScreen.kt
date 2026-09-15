package com.ziegler.kighelper.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.addedit.AudioImportSection
import com.ziegler.kighelper.ui.screens.addedit.ImageImportSection
import com.ziegler.kighelper.ui.screens.addedit.PhraseColorSection
import com.ziegler.kighelper.ui.screens.addedit.PhraseMediaImporter
import com.ziegler.kighelper.ui.screens.addedit.VideoImportSection

private const val TAB_TEXT = "text"
private const val TAB_IMAGE = "image"
private const val TAB_VIDEO = "video"

/**
 * 添加/编辑短语表单。
 * 只接收数据和回调，避免页面直接依赖 ViewModel；媒体文件读写集中在 [PhraseMediaImporter]，
 * 各内容区块抽为独立组件（[ImageImportSection]/[VideoImportSection]/[AudioImportSection]/[PhraseColorSection]）。
 * 播报内容通过 文字/图片/视频 三个 Tab 互斥设置；切换 Tab 仅改变编辑视图，内容在点击保存时按所选 Tab 写入并清理其他类型。
 *
 * @param phrase 待编辑的短语，null 表示新增模式
 * @param isEditMode 是否为编辑模式
 * @param groups 可选分组列表
 * @param initialGroupId 初始选中的分组 ID
 * @param onSave 保存回调，提供标签、播报内容、分组ID、音频/图片/视频路径和自定义颜色
 * @param onBack 返回回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPhraseScreen(
    phrase: Phrase?,
    isEditMode: Boolean,
    groups: List<PhraseGroup>,
    initialGroupId: String? = null,
    onSave: (label: String, speech: String, groupId: String, audioPath: String?, cardColor: Long?, imagePath: String?, videoPath: String?) -> Unit,
    onBack: () -> Unit
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
    var audioPath by rememberSaveable(phrase?.id) { mutableStateOf(phrase?.audioPath) }
    var audioFileName by rememberSaveable(phrase?.id) {
        mutableStateOf(audioPath?.let { PhraseMediaImporter.fileName(it) })
    }

    // 图片状态
    var imagePath by rememberSaveable(phrase?.id) { mutableStateOf(phrase?.imagePath) }
    var imageFileName by rememberSaveable(phrase?.id) {
        mutableStateOf(imagePath?.let { PhraseMediaImporter.fileName(it) })
    }

    // 视频状态
    var videoPath by rememberSaveable(phrase?.id) { mutableStateOf(phrase?.videoPath) }
    var videoFileName by rememberSaveable(phrase?.id) {
        mutableStateOf(videoPath?.let { PhraseMediaImporter.fileName(it) })
    }

    // 颜色状态
    var cardColor by rememberSaveable(phrase?.id) {
        mutableLongStateOf(phrase?.cardColor ?: 0L)
    }
    var hasCustomColor by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.cardColor != null)
    }

    // 媒体选择器：复制到内部存储后回填路径
    val importAndSet =
        { uri: android.net.Uri, dirName: String, setPath: (Pair<String, String>) -> Unit ->
            PhraseMediaImporter.importIntoInternalStorage(context, uri, dirName, phrase?.id)
                ?.let(setPath)
        }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            importAndSet(it, "images") { imported ->
                imagePath = imported.first; imageFileName = imported.second
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            importAndSet(it, "videos") { imported ->
                videoPath = imported.first; videoFileName = imported.second
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            importAndSet(it, "audio") { imported ->
                audioPath = imported.first; audioFileName = imported.second
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
                PhraseMediaImporter.deleteMediaFile(imagePath)
                PhraseMediaImporter.deleteMediaFile(videoPath)
            }

            TAB_IMAGE -> {
                PhraseMediaImporter.deleteMediaFile(videoPath)
            }

            TAB_VIDEO -> {
                PhraseMediaImporter.deleteMediaFile(imagePath)
                PhraseMediaImporter.deleteMediaFile(audioPath)
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
                            PhraseMediaImporter.deleteMediaFile(imagePath)
                            imagePath = null
                            imageFileName = null
                        })

                    TAB_VIDEO -> VideoImportSection(
                        videoPath = videoPath,
                        videoFileName = videoFileName,
                        onPick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                        onRemove = {
                            PhraseMediaImporter.deleteMediaFile(videoPath)
                            videoPath = null
                            videoFileName = null
                        })
                }
            }

            // 音频导入区域（独立于内容 Tab，视频 Tab 隐藏；保存为视频时清除音频）
            if (selectedTab != TAB_VIDEO) {
                AudioImportSection(
                    audioFileName = audioFileName,
                    onPick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    onRemove = {
                        PhraseMediaImporter.deleteMediaFile(audioPath)
                        audioPath = null
                        audioFileName = null
                    })
            }

            // 卡片颜色区域
            PhraseColorSection(
                hasCustomColor = hasCustomColor, cardColor = cardColor, onChange = { has, color ->
                    hasCustomColor = has
                    cardColor = color
                })

            Spacer(modifier = Modifier.height(8.dp))
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