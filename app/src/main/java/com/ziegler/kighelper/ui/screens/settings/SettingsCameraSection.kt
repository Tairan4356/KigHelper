package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.data.camera.CameraAppInfo
import com.ziegler.kighelper.data.camera.CameraProvider
import com.ziegler.kighelper.ui.SettingsViewModel

/**
 * 相机来源与拍照选项。
 *
 * 定位权限请求由外层页面处理，本组件只负责在开关变化时回调。
 */
@Composable
internal fun SettingsCameraSection(
    settings: AppSettings,
    cameraApps: List<CameraAppInfo>,
    viewModel: SettingsViewModel,
    onShowCameraPicker: () -> Unit,
    onWriteLocationChange: (Boolean) -> Unit
) {
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
                    onShowCameraPicker()
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
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
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
                        text = selectedCamera?.packageName ?: "应用不可用时会使用系统相机",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onShowCameraPicker) {
                    Text("选择")
                }
            }
            if (settings.thirdPartyCameraPackage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = settings.thirdPartyCameraActivity.orEmpty(),
                    onValueChange = { value ->
                        viewModel.updateThirdPartyCamera(
                            settings.thirdPartyCameraPackage,
                            value.ifBlank { null })
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
                onCheckedChange = onWriteLocationChange
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
