package com.ziegler.kighelper.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.data.camera.CameraProvider
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.screens.camera.CameraPermissions
import com.ziegler.kighelper.ui.screens.settings.CameraAppPickerDialog
import com.ziegler.kighelper.ui.screens.settings.FontManagementDialog
import com.ziegler.kighelper.ui.screens.settings.SettingsAppearanceSection
import com.ziegler.kighelper.ui.screens.settings.SettingsCameraSection
import com.ziegler.kighelper.ui.screens.settings.SettingsFontSection
import com.ziegler.kighelper.ui.screens.settings.SettingsPermissionSection
import com.ziegler.kighelper.ui.screens.settings.SettingsSystemSection
import com.ziegler.kighelper.ui.utils.findActivity
import kotlinx.coroutines.launch

/**
 * 设置页编排层。
 *
 * 各设置分区已拆分到 `ui/screens/settings`，页面只负责收集状态、
 * 处理字体/相机选择弹窗和定位权限请求。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, onBack: () -> Unit, onTestDevice: (Int) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val playbackDevices by viewModel.playbackDevices.collectAsStateWithLifecycle()
    val cameraApps by viewModel.cameraApps.collectAsStateWithLifecycle()
    val fontCatalog by viewModel.fontCatalog.collectAsStateWithLifecycle()
    val installedFonts by viewModel.installedFonts.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isLoadingCatalog by viewModel.isLoadingCatalog.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showFontManagementDialog by remember { mutableStateOf(false) }
    var showCameraPicker by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(CameraPermissions.hasLocation(context))
    }
    var locationPermissionRequested by remember { mutableStateOf(false) }
    var pendingEnableLocation by remember { mutableStateOf(false) }

    /** 权限弹窗仍可用时返回 true；第一次请求也视为可用。 */
    fun canRequestLocationPermission(): Boolean {
        val activity = context.findActivity() ?: return true
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result.values.any { it }
        if (hasLocationPermission) {
            if (pendingEnableLocation) {
                viewModel.updateWritePhotoLocation(true)
                pendingEnableLocation = false
            }
        } else {
            pendingEnableLocation = false
            if (!canRequestLocationPermission()) {
                pendingEnableLocation = true
                openAppSettings()
            } else {
                Toast.makeText(
                    context, "未授予定位权限，无法写入拍摄地点", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** 打开“记录拍摄地点”前检查权限；弹窗被禁用时跳到系统设置。 */
    fun requestLocationForPhotoSetting(enabled: Boolean) {
        if (!enabled) {
            pendingEnableLocation = false
            viewModel.updateWritePhotoLocation(false)
            return
        }
        if (hasLocationPermission) {
            viewModel.updateWritePhotoLocation(true)
            return
        }
        pendingEnableLocation = true
        if (locationPermissionRequested && !canRequestLocationPermission()) {
            openAppSettings()
        } else {
            locationPermissionRequested = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 从系统设置授权返回时补上用户刚才想开启的开关。
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasLocationPermission = CameraPermissions.hasLocation(context)
        if (hasLocationPermission && pendingEnableLocation) {
            viewModel.updateWritePhotoLocation(true)
            pendingEnableLocation = false
        }
    }

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
            TopAppBar(
                title = { Text("偏好设置") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }, scrollBehavior = scrollBehavior
            )
        }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsFontSection(
                    settings = settings,
                    installedFonts = installedFonts,
                    viewModel = viewModel,
                    onShowFontManagement = { showFontManagementDialog = true })
            }
            item {
                SettingsAppearanceSection(settings = settings, viewModel = viewModel)
            }
            item {
                SettingsSystemSection(
                    settings = settings,
                    playbackDevices = playbackDevices,
                    viewModel = viewModel,
                    onTestDevice = onTestDevice
                )
            }
            item {
                SettingsCameraSection(
                    settings = settings,
                    cameraApps = cameraApps,
                    viewModel = viewModel,
                    onShowCameraPicker = { showCameraPicker = true },
                    onWriteLocationChange = ::requestLocationForPhotoSetting
                )
            }
            item {
                SettingsPermissionSection(settings = settings, viewModel = viewModel)
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
                viewModel.updateThirdPartyCamera(app.packageName, app.component.className)
                showCameraPicker = false
            },
            onSelectPreset = { preset ->
                viewModel.updateCameraProvider(CameraProvider.THIRD_PARTY)
                viewModel.updateThirdPartyCamera(preset.packageName, preset.activityName)
                showCameraPicker = false
            },
            onDismiss = { showCameraPicker = false })
    }
}
