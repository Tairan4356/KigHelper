// 音色设置界面：展示设置项并把模型、预设相关实际操作委托给后端动作函数。
package com.ziegler.kighelper.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.ui.VoiceViewModel
import com.ziegler.kighelper.ui.screens.voice.EngineSelector
import com.ziegler.kighelper.ui.screens.voice.ModelComplianceDialog
import com.ziegler.kighelper.ui.screens.voice.ModelInstallAction
import com.ziegler.kighelper.ui.screens.voice.ModelPickerDialog
import com.ziegler.kighelper.ui.screens.voice.OfflineModelStatusCard
import com.ziegler.kighelper.ui.screens.voice.VoicePresetPickerDialog
import com.ziegler.kighelper.ui.screens.voice.VoicePresetSummaryCard
import com.ziegler.kighelper.ui.screens.voice.VoiceSlider
import com.ziegler.kighelper.ui.screens.voice.deleteVoiceModel
import com.ziegler.kighelper.ui.screens.voice.downloadModelArchive
import com.ziegler.kighelper.ui.screens.voice.importModelArchive
import com.ziegler.kighelper.ui.screens.voice.importVoicePresetConfig
import com.ziegler.kighelper.ui.screens.voice.installRemoteVoiceModel
import com.ziegler.kighelper.ui.screens.voice.shareVoicePresetFile
import com.ziegler.kighelper.utils.OfflineVoiceModelInstaller
import com.ziegler.kighelper.utils.OfflineVoiceModelFormat
import com.ziegler.kighelper.utils.OfflineVoiceModelManager
import com.ziegler.kighelper.utils.KigvpkModelParams
import com.ziegler.kighelper.utils.KigvpkParamsManager
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
    var installProgress by remember { mutableFloatStateOf(0f) }
    var pendingInstallAction by remember { mutableStateOf<ModelInstallAction?>(null) }
    var selectedImportFormat by remember { mutableStateOf(OfflineVoiceModelFormat.VITS) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }
    val activeModelStatus =
        modelStatuses.firstOrNull { it.pack.id == modelManager.normalizeModelId(profile.modelId) }
            ?: modelStatuses.firstOrNull()
    val isKigvpk = activeModelStatus?.pack?.format == OfflineVoiceModelFormat.KIGVPK
    val kigvpkParamsManager = remember(context) { KigvpkParamsManager(context) }
    val kigvpkParams = remember(modelRefreshKey, activeModelStatus?.pack?.id) {
        activeModelStatus?.let {
            kigvpkParamsManager.loadDefaults(it.directory, it.pack.id)
        } ?: KigvpkModelParams()
    }
    val displayNoiseScale = profile.kigvpkNoiseScale ?: kigvpkParams.noiseScale
    val displayNoiseW = profile.kigvpkNoiseW ?: kigvpkParams.noiseW
    val displayLengthScale = profile.kigvpkLengthScale ?: kigvpkParams.lengthScale
    val displaySentenceSilenceSec =
        profile.kigvpkSentenceSilenceSec ?: kigvpkParams.sentenceSilenceSec
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val archiveImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            showModelPicker = true
            isInstalling = true
            installProgress = 0f
            coroutineScope.launch {
                installMessage = "正在导入模型压缩包（可能需要数分钟）"
                val result = importModelArchive(
                    archiveUri = uri,
                    format = selectedImportFormat,
                    installer = modelInstaller,
                    modelManager = modelManager,
                    viewModel = viewModel,
                    currentSpeakerId = profile.speakerId,
                    onProgress = { installProgress = it })
                installMessage = result.message
                if (result.shouldRefreshModels) {
                    modelRefreshKey++
                }
                isInstalling = false
            }
        } else {
            showModelPicker = true
            installMessage = "未选择模型压缩包"
        }
    }
    val configImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            presetMessage = importVoicePresetConfig(
                context = context, uri = uri, viewModel = viewModel, modelManager = modelManager
            )
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
                            arrayOf("application/json", "text/*", "*/*")
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
            if (profile.engineOrDefault == VoiceEngineType.DISABLED) {
                item {
                    Text(
                        "语音合成已关闭",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL) {
                item {
                    OfflineModelStatusCard(
                        activeModelStatus = activeModelStatus,
                        installMessage = installMessage,
                        onClick = { showModelPicker = true })
                }
            }
            if (profile.engineOrDefault != VoiceEngineType.DISABLED) {
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
                Text("声线参数", style = MaterialTheme.typography.labelLarge)
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
    }

    pendingInstallAction?.let { installAction ->
        ModelComplianceDialog(onDismiss = { pendingInstallAction = null }, onConfirm = {
            when (installAction) {
                is ModelInstallAction.ImportArchive -> {
                    archiveImportLauncher.launch("*/*")
                }

                is ModelInstallAction.DownloadArchive -> {
                    isInstalling = true
                    installProgress = 0f
                    coroutineScope.launch {
                        installMessage = "正在下载并安装模型包..."
                        val result = downloadModelArchive(
                            url = downloadUrl,
                            format = selectedImportFormat,
                            installer = modelInstaller,
                            viewModel = viewModel,
                            onProgress = { installProgress = it })
                        installMessage = result.message
                        if (result.shouldRefreshModels) {
                            modelRefreshKey++
                        }
                        isInstalling = false
                    }
                }
            }
            pendingInstallAction = null
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
            installProgress = installProgress,
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
                val result = deleteVoiceModel(
                    status = status,
                    modelManager = modelManager,
                    viewModel = viewModel,
                    activeModelId = profile.modelId
                )
                installMessage = result.message
                if (result.shouldRefreshModels) {
                    modelRefreshKey++
                }
            },
            onInstall = { entry ->
                isInstalling = true
                installProgress = 0f
                coroutineScope.launch {
                    installMessage = "正在下载并安装模型包..."
                    val result = installRemoteVoiceModel(
                        entry = entry,
                        installer = modelInstaller,
                        viewModel = viewModel,
                        currentSpeakerId = profile.speakerId,
                        onProgress = { installProgress = it })
                    installMessage = result.message
                    if (result.shouldRefreshModels) {
                        modelRefreshKey++
                    }
                    isInstalling = false
                }
            })
    }
}

private const val PREVIEW_TEXT = "这是当前自定义音色的试听效果。"
