package com.ziegler.kighelper.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ziegler.kighelper.data.camera.CameraAppInfo
import com.ziegler.kighelper.data.camera.CameraProvider
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid
import com.ziegler.kighelper.ui.screens.settings.ColorModeSelector
import com.ziegler.kighelper.ui.screens.settings.DarkModeOptions
import com.ziegler.kighelper.ui.screens.settings.FontManagementDialog
import com.ziegler.kighelper.ui.screens.settings.FontTypeSelector
import com.ziegler.kighelper.ui.screens.settings.PlaybackDeviceSelector
import com.ziegler.kighelper.ui.screens.settings.SettingConnectedButtonGroup
import com.ziegler.kighelper.ui.screens.settings.SettingSection
import com.ziegler.kighelper.ui.screens.settings.SettingSlider
import com.ziegler.kighelper.ui.screens.settings.SettingSwitch
import com.ziegler.kighelper.ui.theme.FontType
import com.ziegler.kighelper.utils.WindowConfig
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class CameraPreset(
    val label: String, val packageName: String, val activityName: String
)

private val cameraPresets = listOf(
    CameraPreset(
        "AOSP 相机", "com.android.camera2", "com.android.camera.CameraActivity"
    ), CameraPreset(
        "Open Camera", "net.sourceforge.opencamera", "net.sourceforge.opencamera.MainActivity"
    ), CameraPreset(
        "Google Camera", "com.google.android.GoogleCamera", "com.android.camera.CameraLauncher"
    ), CameraPreset(
        "三星相机", "com.sec.android.app.camera", "com.sec.android.app.camera.Camera"
    ), CameraPreset(
        "小米相机", "com.android.camera", "com.android.camera.Camera"
    ), CameraPreset(
        "华为相机", "com.huawei.camera", "com.huawei.camera.controller.CameraAbility"
    )
)

private fun snapToNearestWeight(value: Int, weights: List<Int>): Int {
    return weights.minByOrNull { kotlin.math.abs(it - value) } ?: 400
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, onBack: () -> Unit, onTestDevice: (Int) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val playbackDevices by viewModel.playbackDevices.collectAsStateWithLifecycle()
    val cameraApps by viewModel.cameraApps.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val fontCatalog by viewModel.fontCatalog.collectAsStateWithLifecycle()
    val installedFonts by viewModel.installedFonts.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isLoadingCatalog by viewModel.isLoadingCatalog.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showFontManagementDialog by remember { mutableStateOf(false) }
    var showCameraPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCameraApps()
    }

    val fontFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.importFont(it)
            scope.launch {
                snackbarHostState.showSnackbar("字体导入成功")
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("偏好设置") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }, scrollBehavior = scrollBehavior)
        }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingSection(title = "字体", icon = Icons.Filled.TextFormat) {
                    FontTypeSelector(
                        selectedType = settings.fontType, onTypeSelected = { type, baseName ->
                            viewModel.updateFontType(type)
                            viewModel.selectCustomFont(baseName)
                            val builtinCount = FontType.entries.size
                            if (type < builtinCount) {
                                val weights = FontType.entries[type].availableWeights
                                viewModel.updateFontWeight(
                                    snapToNearestWeight(settings.fontWeight, weights)
                                )
                            }
                        }, installedFonts = installedFonts
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { showFontManagementDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.TextFormat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("字体管理")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val builtinCount = FontType.entries.size
                    val isBuiltin = settings.fontType < builtinCount
                    if (isBuiltin) {
                        val currentFontType = FontType.entries[settings.fontType]
                        val weights = currentFontType.availableWeights
                        if (weights.size > 1) {
                            SettingSlider(
                                title = "字重",
                                value = weights.indexOf(
                                    snapToNearestWeight(settings.fontWeight, weights)
                                ).toFloat(),
                                onValueChange = { index ->
                                    viewModel.updateFontWeight(weights[index.roundToInt()])
                                },
                                valueRange = 0f..(weights.size - 1).toFloat(),
                                steps = weights.size - 2
                            )
                        }
                    } else {
                        val selectedCustomFont = settings.selectedCustomFont
                        if (selectedCustomFont != null) {
                            val customFont =
                                installedFonts.find { it.baseName == selectedCustomFont }
                            if (customFont != null && customFont.weights.size > 1) {
                                val weightValues = customFont.weights.map { it.weight }
                                SettingSlider(
                                    title = "字重",
                                    value = weightValues.indexOf(
                                        snapToNearestWeight(settings.fontWeight, weightValues)
                                    ).toFloat(),
                                    onValueChange = { index ->
                                        viewModel.updateFontWeight(weightValues[index.roundToInt()])
                                    },
                                    valueRange = 0f..(weightValues.size - 1).toFloat(),
                                    steps = weightValues.size - 2
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingSlider(
                        title = "文字显示区字体大小",
                        value = settings.fontSize,
                        onValueChange = viewModel::updateFontSize,
                        valueRange = 0.8f..2.0f,
                        valueText = "${(settings.fontSize * 100).roundToInt()}%"
                    )
                }
            }

            item {
                SettingSection(title = "外观", icon = Icons.Filled.Palette) {
                    Text(
                        "颜色模式",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    DarkModeOptions(
                        selectedMode = settings.darkMode, onModeSelected = viewModel::updateDarkMode
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "主题颜色",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    var showColorPicker by remember { mutableStateOf(false) }
                    ColorModeSelector(
                        colorMode = settings.colorMode,
                        onColorModeChange = viewModel::updateColorMode
                    )
                    if (settings.colorMode == 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PresetColorGrid(
                            selectedIndex = settings.presetColorIndex,
                            onColorSelected = viewModel::updatePresetColorIndex
                        )
                    }
                    if (settings.colorMode == 2) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CustomColorSelector(
                            customColor = settings.customColor,
                            onClick = { showColorPicker = true })
                    }
                    if (showColorPicker) {
                        ColorPickerDialog(
                            initialColor = settings.customColor,
                            onColorSelected = { color ->
                                viewModel.updateCustomColor(color)
                                showColorPicker = false
                            },
                            onDismiss = { showColorPicker = false })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingSwitch(
                        title = "反转文字显示区颜色",
                        subtitle = "文字显示区背景为浅色，文字颜色为深色",
                        checked = settings.displayColorInverted,
                        onCheckedChange = viewModel::updateDisplayColorInverted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "显示区提示词",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = settings.displayHintText,
                        onValueChange = viewModel::updateDisplayHintText,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        placeholder = { Text("点击下面按钮文字在此显示") })
                    Text(
                        "未显示短语时的提示词，留空则不显示",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                SettingSection(title = "系统", icon = Icons.Filled.Tune) {
                    Text(
                        "播放设备",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    PlaybackDeviceSelector(
                        selectedDeviceId = settings.playbackDeviceId,
                        devices = playbackDevices,
                        onDeviceSelected = viewModel::updatePlaybackDevice,
                        onTestDevice = onTestDevice
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "选择应用播放语音或音频时使用的输出设备",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "功能",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SettingSwitch(
                        title = "触感反馈",
                        subtitle = "点击短语时震动",
                        checked = settings.hapticFeedback,
                        onCheckedChange = viewModel::updateHapticFeedback
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSwitch(
                        title = "预见式返回动画",
                        subtitle = "Android 14+ 手势返回时页面跟随手势动画",
                        checked = settings.predictiveBackEnabled,
                        onCheckedChange = viewModel::updatePredictiveBackEnabled
                    )
                }
            }

            item {
                SettingSection(title = "相机", icon = Icons.Filled.PhotoCamera) {
                    Text(
                        "拍照使用",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SettingConnectedButtonGroup(
                        options = listOf("内置", "系统相机", "第三方"),
                        selectedIndex = settings.cameraProvider.ordinal,
                        onSelected = { index ->
                            val provider = CameraProvider.entries.getOrElse(index) {
                                CameraProvider.BUILT_IN
                            }
                            viewModel.updateCameraProvider(provider)
                            if (provider == CameraProvider.THIRD_PARTY) {
                                showCameraPicker = true
                            }
                        })
                    if (settings.cameraProvider == CameraProvider.THIRD_PARTY) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val selectedCamera = cameraApps.firstOrNull {
                            it.packageName == settings.thirdPartyCameraPackage
                        }
                        val activityInvalid =
                            settings.thirdPartyCameraActivity?.isNotBlank() == true && !viewModel.isCameraActivityAvailable(
                                settings.thirdPartyCameraPackage, settings.thirdPartyCameraActivity
                            )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedCamera?.label ?: "未选择第三方应用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedCamera == null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = selectedCamera?.packageName
                                        ?: "应用不可用时会使用系统相机",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { showCameraPicker = true }) {
                                Text("选择")
                            }
                        }
                        if (settings.thirdPartyCameraPackage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = settings.thirdPartyCameraActivity.orEmpty(),
                                onValueChange = { value ->
                                    viewModel.updateThirdPartyCamera(
                                        settings.thirdPartyCameraPackage, value.ifBlank { null })
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Activity 名称") },
                                placeholder = { Text("留空使用应用默认启动 Activity") },
                                singleLine = true,
                                isError = activityInvalid
                            )
                            if (activityInvalid) {
                                Text(
                                    "Activity 不存在，将使用应用默认启动 Activity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    if (settings.cameraProvider == CameraProvider.BUILT_IN) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingSwitch(
                            title = "实况照片",
                            subtitle = "保存拍照后 2 秒动态画面",
                            checked = settings.livePhotoEnabled,
                            onCheckedChange = viewModel::updateLivePhotoEnabled
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingSwitch(
                            title = "记录拍摄地点",
                            subtitle = "写入拍照时的地理坐标",
                            checked = settings.writePhotoLocation,
                            onCheckedChange = viewModel::updateWritePhotoLocation
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingSwitch(
                            title = "兼容模式",
                            subtitle = "拍照异常时开启此模式",
                            checked = settings.cameraCompatibilityMode,
                            onCheckedChange = viewModel::updateCameraCompatibilityMode
                        )
                    }
                }
            }

            item {
                SettingSection(title = "权限", icon = Icons.Filled.Security) {
                    SettingSwitch(
                        title = "通知显示",
                        subtitle = "应用置于后台时显示通知",
                        checked = settings.notificationEnabled,
                        onCheckedChange = viewModel::updateNotificationEnabled
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSwitch(
                        title = "锁屏显示",
                        subtitle = "在锁屏界面显示应用",
                        checked = settings.lockScreenEnabled,
                        onCheckedChange = viewModel::updateLockScreenEnabled
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("锁屏显示权限", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (WindowConfig.canDrawOverlays(context)) "已授予" else "未授予",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (WindowConfig.canDrawOverlays(context)) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(onClick = {
                            try {
                                context.startActivity(
                                    WindowConfig.getOverlayPermissionIntent(context)
                                )
                                val appName =
                                    context.applicationInfo.loadLabel(context.packageManager)
                                        .toString()
                                Toast.makeText(
                                    context, "请找到并开启 $appName 的权限", Toast.LENGTH_LONG
                                ).show()
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context, "无法跳转设置，请手动开启权限", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Text(if (WindowConfig.canDrawOverlays(context)) "已授予" else "去设置")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "不同机型的系统权限设置存在差异，请根据实际情况手动开启锁屏显示、应用上层或悬浮窗等相关权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showFontManagementDialog) {
        FontManagementDialog(
            fontCatalog = fontCatalog?.fonts,
            isLoadingCatalog = isLoadingCatalog,
            installedFonts = installedFonts,
            downloadState = downloadState,
            selectedFont = settings.selectedCustomFont,
            onImportFont = {
                fontFilePicker.launch(
                    arrayOf(
                        "font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf"
                    )
                )
            },
            onDownload = viewModel::downloadFont,
            onDeleteInstalled = viewModel::deleteInstalledFont,
            onDeleteOnline = viewModel::deleteFont,
            onRefreshCatalog = viewModel::loadFontCatalog,
            onDismiss = { showFontManagementDialog = false })
    }

    if (showCameraPicker) {
        CameraAppPickerDialog(
            apps = cameraApps.filterNot { it.packageName == context.packageName },
            selectedPackage = settings.thirdPartyCameraPackage,
            onSelect = { app ->
                viewModel.updateCameraProvider(CameraProvider.THIRD_PARTY)
                viewModel.updateThirdPartyCamera(
                    app.packageName, app.component.className
                )
                showCameraPicker = false
            },
            onSelectPreset = { preset ->
                viewModel.updateCameraProvider(CameraProvider.THIRD_PARTY)
                viewModel.updateThirdPartyCamera(
                    preset.packageName, preset.activityName
                )
                showCameraPicker = false
            },
            onDismiss = { showCameraPicker = false })
    }
}

@Composable
private fun CameraAppPickerDialog(
    apps: List<CameraAppInfo>,
    selectedPackage: String?,
    onSelect: (CameraAppInfo) -> Unit,
    onSelectPreset: (CameraPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val filteredPresets = remember(normalizedQuery, cameraPresets) {
        if (normalizedQuery.isEmpty()) {
            cameraPresets
        } else {
            cameraPresets.filter {
                it.label.contains(normalizedQuery, ignoreCase = true) || it.packageName.contains(
                    normalizedQuery,
                    ignoreCase = true
                ) || it.activityName.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    val filteredApps = remember(normalizedQuery, apps) {
        if (normalizedQuery.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(normalizedQuery, ignoreCase = true) || it.packageName.contains(
                    normalizedQuery,
                    ignoreCase = true
                ) || it.component.className.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择第三方应用") }, text = {
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("搜索应用名称、包名或 Activity") },
                    singleLine = true
                )
            }
            if (filteredPresets.isNotEmpty()) {
                item {
                    Text(
                        "常用相机预设",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(filteredPresets, key = { "preset:${it.packageName}" }) { preset ->
                    val installed = remember(preset.packageName) {
                        runCatching {
                            context.packageManager.getApplicationInfo(
                                preset.packageName, 0
                            )
                        }.isSuccess
                    }
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = installed) {
                            onSelectPreset(preset)
                        }
                        .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (installed) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                            Text(
                                text = if (installed) preset.packageName
                                else "${preset.packageName}（未安装）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        RadioButton(
                            selected = preset.packageName == selectedPackage,
                            onClick = null,
                            enabled = installed
                        )
                    }
                }
            }
            item {
                Text(
                    "全部可启动应用",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }
            if (filteredApps.isEmpty()) {
                item {
                    Text(
                        if (normalizedQuery.isEmpty()) "未找到可启动的应用"
                        else "未找到匹配的应用"
                    )
                }
            } else {
                items(filteredApps, key = { "app:${it.flattenedComponent}" }) { app ->
                    val icon = remember(app.flattenedComponent) {
                        runCatching {
                            context.packageManager.getActivityInfo(app.component, 0)
                                .loadIcon(context.packageManager)
                        }.getOrNull()
                    }
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(app) }
                        .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        RadioButton(
                            selected = app.packageName == selectedPackage, onClick = null
                        )
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = onDismiss) { Text("关闭") }
    })
}