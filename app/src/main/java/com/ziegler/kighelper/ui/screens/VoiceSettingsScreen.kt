// 音色设置界面：展示设置项并把模型、预设相关实际操作委托给后端动作函数。
package com.ziegler.kighelper.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.data.NetworkTtsConfig
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.data.VoiceProfile
import com.ziegler.kighelper.ui.VoiceViewModel
import com.ziegler.kighelper.ui.screens.settings.SettingSection
import com.ziegler.kighelper.ui.screens.voice.EngineSelector
import com.ziegler.kighelper.ui.screens.voice.ModelComplianceDialog
import com.ziegler.kighelper.ui.screens.voice.ModelInstallAction
import com.ziegler.kighelper.ui.screens.voice.ModelPickerDialog
import com.ziegler.kighelper.ui.screens.voice.NetworkApiConfigCard
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
import com.ziegler.kighelper.utils.KigvpkModelParams
import com.ziegler.kighelper.utils.KigvpkParamsManager
import com.ziegler.kighelper.utils.OfflineVoiceModelFormat
import com.ziegler.kighelper.utils.OfflineVoiceModelInstaller
import com.ziegler.kighelper.utils.OfflineVoiceModelManager
import com.ziegler.kighelper.utils.SpeechAudioCache
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun VoiceSettingsScreen(
    viewModel: VoiceViewModel,
    onBack: () -> Unit,
    onPreview: (String) -> Unit,
    onSynthesizeOnly: suspend (String, VoiceProfile) -> java.io.File? = { _, _ -> null }
) {
    val context = LocalContext.current
    val profile = viewModel.activeProfile
    val phrases by viewModel.phrases.collectAsStateWithLifecycle()
    val autoPregenEnabled by viewModel.autoPregenEnabled.collectAsStateWithLifecycle()
    val networkConfig by viewModel.networkConfig.collectAsStateWithLifecycle()
    val modelManager = remember(context) { OfflineVoiceModelManager(context) }
    val modelInstaller = remember(context) { OfflineVoiceModelInstaller(context) }
    val audioCache = remember(context) { SpeechAudioCache(context) }
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
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheRefreshKey by remember { mutableIntStateOf(0) }
    var isPreGenerating by remember { mutableStateOf(false) }
    var preGenMessage by remember { mutableStateOf<String?>(null) }
    var netConfigMessage by remember { mutableStateOf<String?>(null) }
    val activeModelStatus =
        modelStatuses.firstOrNull { it.pack.id == modelManager.normalizeModelId(profile.modelId) }
            ?: modelStatuses.firstOrNull()
    val isKigvpk = activeModelStatus?.pack?.format == OfflineVoiceModelFormat.KIGVPK
    val supportsPregen =
        profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL ||
            profile.engineOrDefault == VoiceEngineType.CLOUD_API
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
    val cacheSize = remember(cacheRefreshKey) {
        audioCache.cacheSizeBytes()
    }
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

    LaunchedEffect(Unit) {
        snapshotFlow {
            buildString {
                append(autoPregenEnabled)
                append('|')
                phrases.forEach { append(it.id).append(':').append(it.speech).append(';') }
                append('|')
                append(profile.synthFingerprint(networkConfig))
            }
        }.distinctUntilChanged().debounce(600.milliseconds).collect {
            val supports = profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL ||
                profile.engineOrDefault == VoiceEngineType.CLOUD_API
            if (autoPregenEnabled && supports && !isPreGenerating) {
                val eligible =
                    phrases.filter { it.speech.isNotBlank() && !it.hasAudio && !it.hasVideo }
                if (eligible.isNotEmpty()) {
                    preGenMessage = "自动生成中…（已缓存短语自动跳过）"
                    eligible.forEachIndexed { index, phrase ->
                        onSynthesizeOnly(phrase.speech, profile)
                        if (index % 3 == 0 || index == eligible.lastIndex) {
                            preGenMessage = "自动生成中 ${index + 1}/${eligible.size}"
                        }
                    }
                    preGenMessage = null
                }
            }
        }
    }

    fun generateAllPresetPhraseVoices() {
        val eligible = phrases.filter { it.speech.isNotBlank() && !it.hasAudio && !it.hasVideo }
        if (eligible.isEmpty()) {
            preGenMessage = "暂无可生成的预设短语：需要包含播报文字"
            return
        }
        isPreGenerating = true
        preGenMessage = null
        coroutineScope.launch {
            var success = 0
            var failures = 0
            eligible.forEachIndexed { index, phrase ->
                val file = onSynthesizeOnly(phrase.speech, profile)
                if (file != null && file.length() > 0L) success++ else failures++
                preGenMessage = "正在生成 ${index + 1}/${eligible.size}：${phrase.label}"
            }
            preGenMessage = buildString {
                append("生成完成：成功 $success")
                if (failures > 0) append("，失败 $failures")
                append("，共 ${eligible.size} 条")
            }
            isPreGenerating = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(
            title = { Text("全局音色设置") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
            }
        }, actions = {
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Filled.MoreVert, "更多操作")
            }
            DropdownMenu(
                expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                if (profile.engineOrDefault != VoiceEngineType.DISABLED) {
                    DropdownMenuItem(
                        text = { Text("导入配置") },
                        onClick = {
                            showMoreMenu = false
                            configImportLauncher.launch(
                                arrayOf("application/json", "text/*", "*/*")
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) })
                    DropdownMenuItem(
                        text = { Text("分享预设") },
                        onClick = {
                            showMoreMenu = false
                            context.shareVoicePresetFile(
                                title = profile.name,
                                content = viewModel.exportActiveProfile(modelManager)
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) })
                }
                DropdownMenuItem(
                    text = { Text("清空语音缓存") },
                    onClick = {
                        showMoreMenu = false
                        showClearCacheDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                    })
            }
        }, scrollBehavior = scrollBehavior
        )
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = if (profile.engineOrDefault != VoiceEngineType.DISABLED) {
                        104.dp
                    } else {
                        16.dp
                    }
                ), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingSection(title = "合成引擎", icon = Icons.Filled.GraphicEq) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            if (profile.engineOrDefault == VoiceEngineType.DISABLED) {
                                Text(
                                    "语音合成已关闭",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL) {
                                OfflineModelStatusCard(
                                    activeModelStatus = activeModelStatus,
                                    installMessage = installMessage,
                                    onClick = { showModelPicker = true })
                                if (activeModelStatus?.pack?.supportsSpeakerSelection == true) {
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
                                        steps = (activeModelStatus.pack.speakerCount - 2).coerceAtLeast(
                                            0
                                        ),
                                        onValueChange = {
                                            viewModel.updateActiveProfile(speakerId = it.roundToInt())
                                        })
                                }
                            }
                            if (profile.engineOrDefault == VoiceEngineType.CLOUD_API) {
                                NetworkApiConfigCard(
                                    config = networkConfig,
                                    message = netConfigMessage,
                                    onSave = { config ->
                                        viewModel.saveNetworkConfig(config)
                                        netConfigMessage =
                                            if (config.isUsable) "配置已保存，可点击下方试听或生成预设短语语音"
                                            else "配置不完整，请填写接口地址、模型、音色和 API Key"
                                    })
                                if (!networkConfig.isUsable) {
                                    Text(
                                        "网络接口配置不完整时将自动回退系统 TTS",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                if (profile.engineOrDefault != VoiceEngineType.DISABLED) {
                    item {
                        SettingSection(title = "声线参数", icon = Icons.Filled.Tune) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                VoicePresetSummaryCard(
                                    activeProfile = profile,
                                    importMessage = presetMessage,
                                    onClick = { showPresetPicker = true })
                                OutlinedTextField(
                                    value = profile.name,
                                    onValueChange = { viewModel.updateActiveProfile(name = it) },
                                    label = { Text("预设名称") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FilledTonalButton(
                                    onClick = viewModel::resetActiveProfileParameters,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("重置当前声线参数")
                                }
                                if (!isKigvpk) {
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
                                    VoiceSlider(
                                        title = "语速",
                                        valueText = "${(profile.speechRate * 100).roundToInt()}%",
                                        value = profile.speechRate,
                                        valueRange = 0.75f..1.25f,
                                        onValueChange = {
                                            viewModel.updateActiveProfile(speechRate = it)
                                        })
                                    VoiceSlider(
                                        title = "音高",
                                        valueText = "${(profile.pitch * 100).roundToInt()}%",
                                        value = profile.pitch,
                                        valueRange = 0.85f..1.15f,
                                        onValueChange = {
                                            viewModel.updateActiveProfile(pitch = it)
                                        })
                                    VoiceSlider(
                                        title = "温暖度",
                                        valueText = "${(profile.warmth * 100).roundToInt()}%",
                                        value = profile.warmth,
                                        valueRange = 0f..1f,
                                        onValueChange = {
                                            viewModel.updateActiveProfile(warmth = it)
                                        })
                                    VoiceSlider(
                                        title = "表现力",
                                        valueText = "${(profile.expressiveness * 100).roundToInt()}%",
                                        value = profile.expressiveness,
                                        valueRange = 0f..1f,
                                        onValueChange = {
                                            viewModel.updateActiveProfile(expressiveness = it)
                                        })
                                }
                            }
                        }
                    }
                    if (isKigvpk) {
                        item {
                            SettingSection(
                                title = "KIGVPK 参数", icon = Icons.Filled.ShutterSpeed
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    VoiceSlider(
                                        title = "语调起伏",
                                        valueText = "${(displayNoiseScale * 100).roundToInt()}%",
                                        value = displayNoiseScale,
                                        valueRange = 0.3f..1.5f,
                                        onValueChange = {
                                            viewModel.updateActiveProfile(kigvpkNoiseScale = it)
                                        })
                                    VoiceSlider(
                                        title = "语调力度",
                                        valueText = "${(displayNoiseW * 100).roundToInt()}%",
                                        value = displayNoiseW,
                                        valueRange = 0.3f..1.5f,
                                        onValueChange = { viewModel.updateActiveProfile(kigvpkNoiseW = it) })
                                    VoiceSlider(
                                        title = "模型语速",
                                        valueText = "${(displayLengthScale * 100).roundToInt()}%",
                                        value = displayLengthScale,
                                        valueRange = 0.5f..2.0f,
                                        onValueChange = {
                                            viewModel.updateActiveProfile(kigvpkLengthScale = it)
                                        })
                                    VoiceSlider(
                                        title = "句末停顿",
                                        valueText = "${(displaySentenceSilenceSec * 1000).roundToInt()}ms",
                                        value = displaySentenceSilenceSec,
                                        valueRange = 0f..1f,
                                        onValueChange = {
                                            viewModel.updateActiveProfile(kigvpkSentenceSilenceSec = it)
                                        })
                                }
                            }
                        }
                    }
                    if (supportsPregen) {
                        item {
                            SettingSection(
                                title = "预设短语语音", icon = Icons.Filled.RecordVoiceOver
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "为已设置的预设短语批量合成语音并缓存，点击短语时直接播放。" +
                                            "系统 TTS 引擎不支持预生成。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = ::generateAllPresetPhraseVoices,
                                        enabled = !isPreGenerating,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isPreGenerating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.width(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            if (isPreGenerating) "正在生成…"
                                            else "一键生成全部预设短语语音"
                                        )
                                    }
                                    preGenMessage?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "短语变更时自动生成",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                "新增/修改短语或切换音色、引擎、模型、参数后自动补齐",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = autoPregenEnabled,
                                            onCheckedChange = viewModel::setAutoPregenEnabled
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (profile.engineOrDefault != VoiceEngineType.DISABLED) {
                Button(
                    onClick = { onPreview(PREVIEW_TEXT) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("试听当前音色")
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清空语音缓存") },
            text = {
                Text(
                    "将删除已生成的语音缓存（约占用 ${cacheSize.formatBytes()}）。" +
                        "删除后再次播放或一键生成会重新合成。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    audioCache.clear()
                    cacheRefreshKey++
                    showClearCacheDialog = false
                    Toast.makeText(context, "语音缓存已清空", Toast.LENGTH_SHORT).show()
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") }
            }
        )
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

private fun VoiceProfile.synthFingerprint(networkConfig: NetworkTtsConfig): String {
    return buildString {
        append(engineOrDefault.name).append('|')
        append(modelId.orEmpty()).append('|')
        append(speakerId).append('|')
        append(age).append('|')
        append(speechRate).append('|')
        append(pitch).append('|')
        append(warmth).append('|')
        append(expressiveness).append('|')
        append(kigvpkNoiseScale).append('|')
        append(kigvpkNoiseW).append('|')
        append(kigvpkLengthScale).append('|')
        append(kigvpkSentenceSilenceSec).append('|')
        if (engineOrDefault == VoiceEngineType.CLOUD_API) {
            append(networkConfig.baseUrl).append('|')
            append(networkConfig.modelId).append('|')
            append(networkConfig.voiceId)
        }
    }
}

private fun Long.formatBytes(): String {
    return when {
        this >= 1024 * 1024 -> "%.1f MB".format(this / (1024f * 1024f))
        this >= 1024 -> "%.1f KB".format(this / 1024f)
        else -> "$this B"
    }
}