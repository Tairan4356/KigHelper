package com.ziegler.kighelper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid
import java.io.File

/**
 * 添加/编辑短语表单。
 * 只接收数据和回调，避免页面直接依赖 ViewModel。
 *
 * @param phrase 待编辑的短语，null 表示新增模式
 * @param isEditMode 是否为编辑模式
 * @param groups 可选分组列表
 * @param initialGroupId 初始选中的分组 ID
 * @param onSave 保存回调，提供标签、播报内容、分组ID、音频路径和自定义颜色
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
    onSave: (label: String, speech: String, groupId: String, audioPath: String?, cardColor: Long?) -> Unit,
    onBack: () -> Unit,
    onAudioImported: ((Uri, String) -> Unit)? = null
) {
    val context = LocalContext.current

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

    // 音频状态
    var audioPath by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.audioPath)
    }
    var audioFileName by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.audioPath?.let { File(it).name })
    }

    // 颜色状态
    var cardColor by rememberSaveable(phrase?.id) {
        mutableLongStateOf(phrase?.cardColor ?: 0L)
    }
    var hasCustomColor by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.cardColor != null)
    }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    // 音频选择器
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // 获取文件名
            val cursor = context.contentResolver.query(it, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val fileName = if (cursor != null && nameIndex != null && cursor.moveToFirst()) {
                cursor.getString(nameIndex) ?: "audio_${System.currentTimeMillis()}.mp3"
            } else {
                "audio_${System.currentTimeMillis()}.mp3"
            }
            cursor?.close()

            // 复制到内部存储
            val audioDir = File(context.filesDir, "audio")
            audioDir.mkdirs()
            val destFile = File(audioDir, "${phrase?.id ?: System.currentTimeMillis()}_$fileName")

            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                audioPath = destFile.absolutePath
                audioFileName = destFile.name
            } catch (e: Exception) {
                // 静默失败，不设置音频路径
            }
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑短语" else "添加短语") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                })
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
            OutlinedTextField(
                value = speech,
                onValueChange = { speech = it },
                label = { Text("播报内容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // 音频导入区域
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "音频文件",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (audioFileName != null) {
                    // 已导入音频
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
                            // 删除导入的音频文件
                            audioPath?.let { path ->
                                File(path).delete()
                            }
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = null
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
                        onClick = { hasCustomColor = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("自定义卡片颜色")
                    }
                }

                if (hasCustomColor) {
                    PresetColorGrid(
                        selectedIndex = -1,
                        onColorSelected = { index ->
                            val colors = listOf(
                                0xFF6650A4L, 0xFF2196F3L, 0xFF00BCD4L, 0xFF4CAF50L,
                                0xFFFFEB3BL, 0xFFFF9800L, 0xFFF44336L, 0xFFE91E63L
                            )
                            if (index in colors.indices) {
                                cardColor = colors[index]
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val finalColor = if (hasCustomColor && cardColor != 0L) cardColor else null
                    onSave(label, speech, selectedGroupId, audioPath, finalColor)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = label.isNotBlank() && speech.isNotBlank()
            ) {
                Text("保存")
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = if (cardColor != 0L) cardColor else 0xFF6650A4,
            onColorSelected = { color ->
                cardColor = color
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}
