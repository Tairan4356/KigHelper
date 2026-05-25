package com.ziegler.kighelper.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.VoiceProfile
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.ui.VoiceViewModel
import com.ziegler.kighelper.ui.VoicePresetImportResult
import com.ziegler.kighelper.utils.ModelInstallResult
import com.ziegler.kighelper.utils.OfflineVoiceModelInstaller
import com.ziegler.kighelper.utils.OfflineVoiceModelFormat
import com.ziegler.kighelper.utils.OfflineVoiceModelManager
import com.ziegler.kighelper.utils.OfflineVoiceModelStatus
import com.ziegler.kighelper.utils.RemoteVoiceModelCatalogEntry
import com.ziegler.kighelper.utils.KigvpkModelParams
import com.ziegler.kighelper.utils.KigvpkParamsManager
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    viewModel: VoiceViewModel, onBack: () -> Unit, onPreview: (String) -> Unit
) {
    val navigationBarPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val context = LocalContext.current
    val profile = viewModel.activeProfile
    val modelManager = remember(context) { OfflineVoiceModelManager(context) }
    val modelInstaller = remember(context) { OfflineVoiceModelInstaller(context) }
    var modelRefreshKey by remember { mutableIntStateOf(0) }
    val modelStatuses = remember(modelRefreshKey) { modelManager.getModelStatuses() }
    val remoteModelCatalog = remember { modelManager.getRemoteModelCatalog() }
    var downloadUrl by remember { mutableStateOf("") }
    var installMessage by remember { mutableStateOf<String?>(null) }
    var presetMessage by remember { mutableStateOf<String?>(null) }
    var isInstalling by remember { mutableStateOf(false) }
    var pendingInstallAction by remember { mutableStateOf<ModelInstallAction?>(null) }
    var selectedImportFormat by remember { mutableStateOf(OfflineVoiceModelFormat.VITS) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }
    val activeModelStatus =
        modelStatuses.firstOrNull { it.pack.id == modelManager.normalizeModelId(profile.modelId) }
            ?: modelStatuses.firstOrNull()
    val isKigvpk = activeModelStatus?.pack?.format == OfflineVoiceModelFormat.KIGVPK
    val kigvpkParamsManager = remember(context) { KigvpkParamsManager(context) }
    var kigvpkParams by remember(modelRefreshKey, activeModelStatus?.pack?.id) {
        mutableStateOf(activeModelStatus?.let {
            kigvpkParamsManager.loadDefaults(
                it.directory, it.pack.id
            )
        } ?: KigvpkModelParams())
    }
    val displayNoiseScale = profile.kigvpkNoiseScale ?: kigvpkParams.noiseScale
    val displayNoiseW = profile.kigvpkNoiseW ?: kigvpkParams.noiseW
    val displayLengthScale = profile.kigvpkLengthScale ?: kigvpkParams.lengthScale
    val displaySentenceSilenceSec = profile.kigvpkSentenceSilenceSec ?: kigvpkParams.sentenceSilenceSec
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val archiveImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showModelPicker = true
            isInstalling = true
            installMessage = "正在导入模型压缩包（可能需要数分钟）"
            coroutineScope.launch {
                installMessage = when (val result = modelInstaller.importFromArchiveFile(
                    archiveUri = uri, format = selectedImportFormat
                )) {
                    is ModelInstallResult.Success -> {
                        result.modelId?.let { modelId ->
                            val status = modelManager.getModelStatus(modelId)
                            viewModel.updateActiveProfile(
                                engine = VoiceEngineType.OFFLINE_NEURAL,
                                modelId = modelId,
                                speakerId = profile.speakerId.coerceIn(
                                    0, (status?.pack?.speakerCount ?: 1) - 1
                                )
                            )
                        }
                        result.message
                    }

                    is ModelInstallResult.Partial -> {
                        result.modelId?.let { modelId ->
                            viewModel.updateActiveProfile(
                                engine = VoiceEngineType.OFFLINE_NEURAL,
                                modelId = modelId,
                                speakerId = 0
                            )
                        }
                        result.message
                    }

                    is ModelInstallResult.Failure -> result.message
                }
                modelRefreshKey++
                isInstalling = false
            }
        } else {
            showModelPicker = true
            installMessage = "未选择模型压缩包"
        }
    }
    val configImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = context.readTextFromUri(uri)
            presetMessage = when {
                content == null -> "配置文件读取失败"
                else -> when (val result = viewModel.importProfile(content, modelManager)) {
                    VoicePresetImportResult.Success -> "配置文件已导入"
                    VoicePresetImportResult.InvalidFile -> "配置文件无效"
                    is VoicePresetImportResult.MissingModel -> {
                        val modelName = result.modelName ?: result.modelId ?: "未知模型"
                        "预设引用的端侧模型未安装或不可用：$modelName。请先安装对应模型后再导入。"
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("全局音色设置") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }, actions = {
                IconButton(
                    onClick = {
                        configImportLauncher.launch(
                            arrayOf(
                                "application/json", "text/*", "*/*"
                            )
                        )
                    }) {
                    Icon(Icons.Filled.FileOpen, "导入配置")
                }

                IconButton(
                    onClick = {
                        context.shareVoicePresetFile(
                            title = profile.name,
                            content = viewModel.exportActiveProfile(modelManager)
                        )
                    }) {
                    Icon(Icons.Filled.Share, "分享预设")
                }
            }, scrollBehavior = scrollBehavior
            )
        }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding), contentPadding = PaddingValues(
                start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navigationBarPadding
            ), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "合成引擎",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                EngineSelector(
                    selected = profile.engineOrDefault, onSelect = { engine ->
                        viewModel.updateActiveProfile(
                            engine = engine,
                            modelId = if (engine == VoiceEngineType.OFFLINE_NEURAL) {
                                profile.modelId ?: modelStatuses.firstOrNull()?.pack?.id
                            } else {
                                null
                            }
                        )
                    })
            }
            if (profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL) {
                item {
                    OfflineModelStatusCard(
                        modelRootPath = modelManager.modelRootPath,
                        activeModelStatus = activeModelStatus,
                        installMessage = installMessage,
                        onClick = { showModelPicker = true })
                }
            }
            if (profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL && activeModelStatus?.pack?.supportsSpeakerSelection == true) {
                item {
                    VoiceSlider(
                        title = "说话人",
                        valueText = "${
                            profile.speakerId.coerceIn(
                                0, activeModelStatus.pack.speakerCount - 1
                            )
                        } / ${activeModelStatus.pack.speakerCount - 1}",
                        value = profile.speakerId.coerceIn(
                            0, activeModelStatus.pack.speakerCount - 1
                        ).toFloat(),
                        valueRange = 0f..(activeModelStatus.pack.speakerCount - 1).toFloat(),
                        steps = (activeModelStatus.pack.speakerCount - 2).coerceAtLeast(0),
                        onValueChange = {
                            viewModel.updateActiveProfile(speakerId = it.roundToInt())
                        })
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "声线参数", style = MaterialTheme.typography.labelLarge
                )
            }
            item {
                VoicePresetSummaryCard(
                    activeProfile = profile,
                    importMessage = presetMessage,
                    onClick = { showPresetPicker = true })
            }
            item {
                OutlinedTextField(
                    value = profile.name,
                    onValueChange = { viewModel.updateActiveProfile(name = it) },
                    label = { Text("预设名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedButton(
                    onClick = viewModel::resetActiveProfileParameters,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重置当前声线参数")
                }
            }
            if (isKigvpk) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Text("KIGVPK 参数", style = MaterialTheme.typography.labelLarge) }
                item {
                    VoiceSlider(
                        title = "语调起伏",
                        valueText = "${(displayNoiseScale * 100).roundToInt()}%",
                        value = displayNoiseScale,
                        valueRange = 0.3f..1.5f,
                        onValueChange = { viewModel.updateActiveProfile(kigvpkNoiseScale = it) })
                }
                item {
                    VoiceSlider(
                        title = "语调力度",
                        valueText = "${(displayNoiseW * 100).roundToInt()}%",
                        value = displayNoiseW,
                        valueRange = 0.3f..1.5f,
                        onValueChange = { viewModel.updateActiveProfile(kigvpkNoiseW = it) })
                }
                item {
                    VoiceSlider(
                        title = "模型语速",
                        valueText = "${(displayLengthScale * 100).roundToInt()}%",
                        value = displayLengthScale,
                        valueRange = 0.5f..2.0f,
                        onValueChange = { viewModel.updateActiveProfile(kigvpkLengthScale = it) })
                }
                item {
                    VoiceSlider(
                        title = "句末停顿",
                        valueText = "${(displaySentenceSilenceSec * 1000).roundToInt()}ms",
                        value = displaySentenceSilenceSec,
                        valueRange = 0f..1f,
                        onValueChange = { viewModel.updateActiveProfile(kigvpkSentenceSilenceSec = it) })
                }
            } else {
                item {
                    VoiceSlider(
                        title = "年龄",
                        valueText = when {
                            profile.age < 0.35f -> "更年轻"
                            profile.age > 0.68f -> "更成熟"
                            else -> "自然"
                        },
                        value = profile.age,
                        valueRange = 0f..1f,
                        onValueChange = { viewModel.updateActiveProfile(age = it) })
                }
                item {
                    VoiceSlider(
                        title = "语速",
                        valueText = "${(profile.speechRate * 100).roundToInt()}%",
                        value = profile.speechRate,
                        valueRange = 0.75f..1.25f,
                        onValueChange = { viewModel.updateActiveProfile(speechRate = it) })
                }
                item {
                    VoiceSlider(
                        title = "音高",
                        valueText = "${(profile.pitch * 100).roundToInt()}%",
                        value = profile.pitch,
                        valueRange = 0.85f..1.15f,
                        onValueChange = { viewModel.updateActiveProfile(pitch = it) })
                }
                item {
                    VoiceSlider(
                        title = "温暖度",
                        valueText = "${(profile.warmth * 100).roundToInt()}%",
                        value = profile.warmth,
                        valueRange = 0f..1f,
                        onValueChange = { viewModel.updateActiveProfile(warmth = it) })
                }
                item {
                    VoiceSlider(
                        title = "表现力",
                        valueText = "${(profile.expressiveness * 100).roundToInt()}%",
                        value = profile.expressiveness,
                        valueRange = 0f..1f,
                        onValueChange = { viewModel.updateActiveProfile(expressiveness = it) })
                }
            }
            item {
                Button(
                    onClick = { onPreview(PREVIEW_TEXT) }, modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("试听当前音色")
                }
            }
        }

    }

    val installAction = pendingInstallAction
    if (installAction != null) {
        ModelComplianceDialog(onDismiss = { pendingInstallAction = null }, onConfirm = {
            pendingInstallAction = null
            when (installAction) {
                is ModelInstallAction.ImportArchive -> {
                    archiveImportLauncher.launch("*/*")
                }

                is ModelInstallAction.DownloadArchive -> {
                    isInstalling = true
                    installMessage = "正在下载并安装模型包..."
                    coroutineScope.launch {
                        installMessage =
                            when (val result = modelInstaller.downloadAndInstallArchive(
                                url = downloadUrl, format = selectedImportFormat
                            )) {
                                is ModelInstallResult.Success -> {
                                    result.modelId?.let { modelId ->
                                        viewModel.updateActiveProfile(
                                            engine = VoiceEngineType.OFFLINE_NEURAL,
                                            modelId = modelId,
                                            speakerId = 0
                                        )
                                    }
                                    result.message
                                }

                                is ModelInstallResult.Partial -> {
                                    result.modelId?.let { modelId ->
                                        viewModel.updateActiveProfile(
                                            engine = VoiceEngineType.OFFLINE_NEURAL,
                                            modelId = modelId,
                                            speakerId = 0
                                        )
                                    }
                                    result.message
                                }

                                is ModelInstallResult.Failure -> result.message
                            }
                        modelRefreshKey++
                        isInstalling = false
                    }
                }
            }
        })
    }

    if (showPresetPicker) {
        VoicePresetPickerDialog(
            profiles = viewModel.profiles,
            activeProfileId = profile.id,
            canDelete = viewModel.profiles.size > 1,
            onDismiss = { showPresetPicker = false },
            onSelect = { id ->
                viewModel.setActiveProfile(id)
                showPresetPicker = false
            },
            onDuplicate = viewModel::duplicateActiveProfile,
            onDelete = viewModel::deleteProfile
        )
    }

    if (showModelPicker) {
        ModelPickerDialog(
            activeModelId = profile.modelId,
            remoteModelCatalog = remoteModelCatalog,
            modelStatuses = modelStatuses,
            isInstalling = isInstalling,
            installMessage = installMessage,
            selectedImportFormat = selectedImportFormat,
            downloadUrl = downloadUrl,
            onImportFormatSelect = { selectedImportFormat = it },
            onDownloadUrlChange = { downloadUrl = it },
            onImportClick = { pendingInstallAction = ModelInstallAction.ImportArchive },
            onDownloadClick = { pendingInstallAction = ModelInstallAction.DownloadArchive },
            onDismiss = { showModelPicker = false },
            onSelect = { entry ->
                viewModel.updateActiveProfile(
                    engine = VoiceEngineType.OFFLINE_NEURAL,
                    modelId = entry.pack.id,
                    speakerId = profile.speakerId.coerceIn(0, entry.pack.speakerCount - 1)
                )
                showModelPicker = false
            },
            onSelectStatus = { status ->
                viewModel.updateActiveProfile(
                    engine = VoiceEngineType.OFFLINE_NEURAL,
                    modelId = status.pack.id,
                    speakerId = profile.speakerId.coerceIn(0, status.pack.speakerCount - 1)
                )
                showModelPicker = false
            },
            onDeleteModel = { status ->
                val deleted = modelManager.deleteModel(status.pack.id)
                if (deleted) {
                    installMessage = "${status.pack.name} 已删除"
                    modelRefreshKey++
                    if (profile.modelId == status.pack.id) {
                        val fallback = modelManager.getModelStatuses()
                            .firstOrNull { it.pack.id != status.pack.id }
                        viewModel.updateActiveProfile(
                            engine = VoiceEngineType.OFFLINE_NEURAL,
                            modelId = fallback?.pack?.id,
                            speakerId = 0
                        )
                    }
                } else {
                    installMessage = "${status.pack.name} 删除失败"
                }
            },
            onInstall = { entry ->
                isInstalling = true
                coroutineScope.launch {
                    installMessage = when (val result = modelInstaller.installRemoteModel(entry)) {
                        is ModelInstallResult.Success -> {
                            viewModel.updateActiveProfile(
                                engine = VoiceEngineType.OFFLINE_NEURAL,
                                modelId = entry.pack.id,
                                speakerId = profile.speakerId.coerceIn(
                                    0, entry.pack.speakerCount - 1
                                )
                            )
                            result.message
                        }

                        is ModelInstallResult.Partial -> result.message
                        is ModelInstallResult.Failure -> result.message
                    }
                    modelRefreshKey++
                    isInstalling = false
                }
            })
    }


}

@Composable
private fun EngineSelector(
    selected: VoiceEngineType, onSelect: (VoiceEngineType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VoiceEngineType.entries.forEach { engine ->
            FilterChip(
                selected = selected == engine,
                onClick = { onSelect(engine) },
                label = { Text(engine.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OfflineModelStatusCard(
    modelRootPath: String,
    activeModelStatus: OfflineVoiceModelStatus?,
    installMessage: String?,
    onClick: () -> Unit
) {
    val isReady = activeModelStatus?.isReady == true
    val isPartial = activeModelStatus?.isPartiallyInstalled == true
    val compatibilityIssue = activeModelStatus?.runtimeCompatibilityIssue

    Card(
        modifier = Modifier.fillMaxWidth(), onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "点击选择或导入模型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    isReady -> "当前模型：${activeModelStatus.pack.name} · ${activeModelStatus.pack.format.label}"
                    isPartial -> "已导入 ONNX 权重，但缺少：${activeModelStatus.missingFiles.joinToString()}"
                    else -> "模型未安装"
                }, style = MaterialTheme.typography.bodySmall
            )
            if (compatibilityIssue != null) {
                Text(
                    text = compatibilityIssue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (isPartial) {
                Text(
                    text = "缺少文本前端文件时无法把中文转换为模型 token，端侧推理会自动回退系统 TTS。",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (installMessage != null) {
                Text(
                    text = installMessage, style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ModelPickerDialog(
    activeModelId: String?,
    remoteModelCatalog: List<RemoteVoiceModelCatalogEntry>,
    modelStatuses: List<OfflineVoiceModelStatus>,
    isInstalling: Boolean,
    installMessage: String?,
    selectedImportFormat: OfflineVoiceModelFormat,
    downloadUrl: String,
    onImportFormatSelect: (OfflineVoiceModelFormat) -> Unit,
    onDownloadUrlChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (RemoteVoiceModelCatalogEntry) -> Unit,
    onSelectStatus: (OfflineVoiceModelStatus) -> Unit,
    onDeleteModel: (OfflineVoiceModelStatus) -> Unit,
    onInstall: (RemoteVoiceModelCatalogEntry) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择端侧模型") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    text = "端侧引擎使用Sherpa-ONNX，支持VITS、Piper和Kokoro格式的模型包。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            items(remoteModelCatalog, key = { it.pack.id }) { entry ->
                val status = modelStatuses.firstOrNull { it.pack.id == entry.pack.id }
                RemoteModelItem(
                    entry = entry,
                    selected = entry.pack.id == activeModelId,
                    installed = status?.isReady == true,
                    partiallyInstalled = status?.isPartiallyInstalled == true,
                    isInstalling = isInstalling,
                    onSelect = { onSelect(entry) },
                    onInstall = { onInstall(entry) })
            }
            val remoteModelIds = remoteModelCatalog.map { it.pack.id }.toSet()
            val customModelStatuses = modelStatuses.filter { it.pack.id !in remoteModelIds }
            if (customModelStatuses.isNotEmpty()) {
                item {
                    Text(
                        text = "用户导入模型",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(customModelStatuses, key = { it.pack.id }) { status ->
                    ImportedModelItem(
                        status = status,
                        selected = status.pack.id == activeModelId,
                        isInstalling = isInstalling,
                        onSelect = { onSelectStatus(status) },
                        onDelete = { onDeleteModel(status) })
                }
            }
            item {
                Text(
                    text = "手动安装",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Text(
                    text = "模型格式（格式错误的模型可能会导致程序崩溃）",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            item {
                ImportFormatSelector(
                    selected = selectedImportFormat, onSelect = onImportFormatSelect
                )
            }
            item {
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = onDownloadUrlChange,
                    label = { Text("模型压缩包下载地址") },
                    enabled = !isInstalling,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onImportClick,
                        enabled = !isInstalling,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("导入压缩包")
                    }
                    OutlinedButton(
                        onClick = onDownloadClick,
                        enabled = !isInstalling,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("下载压缩包")
                    }
                }
            }
            if (isInstalling) {
                item {
                    Text(
                        text = "正在安装模型包...", style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (installMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = installMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("关闭")
        }
    })
}

@Composable
private fun ImportedModelItem(
    status: OfflineVoiceModelStatus,
    selected: Boolean,
    isInstalling: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val compatibilityIssue = status.runtimeCompatibilityIssue
                Text(status.pack.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = when {
                        compatibilityIssue != null -> compatibilityIssue
                        status.isReady -> "已安装 · ${status.pack.format.label}"
                        status.isPartiallyInstalled -> "缺少：${status.missingFiles.joinToString()}"
                        else -> "未安装完整"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onSelect,
                enabled = !isInstalling && status.isReady && status.runtimeCompatibilityIssue == null
            ) {
                Text(if (selected) "已选择" else "选择")
            }
            IconButton(
                onClick = onDelete, enabled = !isInstalling
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除模型",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ImportFormatSelector(
    selected: OfflineVoiceModelFormat, onSelect: (OfflineVoiceModelFormat) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            OfflineVoiceModelFormat.VITS,
            OfflineVoiceModelFormat.PIPER,
            OfflineVoiceModelFormat.KOKORO
        ).forEach { format ->
            FilterChip(
                selected = selected == format,
                onClick = { onSelect(format) },
                label = { Text(format.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val kigvpk = OfflineVoiceModelFormat.KIGVPK
    FilterChip(
        selected = selected == kigvpk,
        onClick = { onSelect(kigvpk) },
        label = { Text(kigvpk.label + "（KIGTTS 训练器格式）") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RemoteModelItem(
    entry: RemoteVoiceModelCatalogEntry,
    selected: Boolean,
    installed: Boolean,
    partiallyInstalled: Boolean,
    isInstalling: Boolean,
    onSelect: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.pack.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        entry.pack.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = if (installed) onSelect else onInstall,
                    enabled = !isInstalling,
                ) {
                    Text(
                        when {
                            installed && selected -> "已选择"
                            installed -> "选择"
                            partiallyInstalled -> "补齐文件"
                            else -> "一键安装"
                        }
                    )
                }
            }
            Text(
                if (partiallyInstalled) {
                    "已导入 ONNX，但仍缺少：${
                        entry.pack.requiredFiles.filterNot { it == "config.json" }.joinToString()
                    }"
                } else {
                    "${entry.pack.format.label} · 说话人：${entry.pack.speakerCount}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }
    }
}

@Composable
private fun ModelComplianceDialog(
    onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("模型使用合规声明") }, text = {
        Text(
            "继续操作前，请确认你导入或下载的模型来源合法，许可允许在本应用中离线端侧推理，并且不包含未经授权的声音克隆或受限制数据。"
        )
    }, confirmButton = {
        TextButton(onClick = onConfirm) {
            Text("我已确认")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}

private sealed class ModelInstallAction {
    data object ImportArchive : ModelInstallAction()
    data object DownloadArchive : ModelInstallAction()
}

@Composable
private fun VoicePresetSummaryCard(
    activeProfile: VoiceProfile, importMessage: String?, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "选择或导入声线预设",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "已选择的声线预设：${activeProfile.name}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = activeProfile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (importMessage != null) {
                Text(
                    text = importMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun VoicePresetPickerDialog(
    profiles: List<VoiceProfile>,
    activeProfileId: String,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择声线预设") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profiles, key = { it.id }) { item ->
                VoicePresetItem(
                    profile = item,
                    selected = item.id == activeProfileId,
                    canDelete = canDelete,
                    onClick = { onSelect(item.id) },
                    onDelete = { onDelete(item.id) })
            }
            item {
                OutlinedButton(
                    onClick = onDuplicate, modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("复制当前预设")
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("关闭")
        }
    })
}

@Composable
private fun VoicePresetItem(
    profile: VoiceProfile,
    selected: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), onClick = onClick, colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete, enabled = canDelete
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除预设",
                    tint = if (canDelete) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps
        )
    }
}

private fun android.content.Context.shareVoicePresetFile(title: String, content: String) {
    val safeName =
        title.replace(Regex("[^A-Za-z0-9_\\-\\u4e00-\\u9fa5]"), "_").ifBlank { "voice_preset" }
    val file = File(cacheDir, "voice_presets/$safeName.kigvoice.json").apply {
        parentFile?.mkdirs()
        writeText(content, Charsets.UTF_8)
    }
    val uri = FileProvider.getUriForFile(
        this, "$packageName.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "KigHelper 音色预设：$title")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "分享音色配置文件"))
}

private fun android.content.Context.readTextFromUri(uri: Uri): String? {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }.getOrNull()
}

private const val PREVIEW_TEXT = "这是当前自定义音色的试听效果。"
