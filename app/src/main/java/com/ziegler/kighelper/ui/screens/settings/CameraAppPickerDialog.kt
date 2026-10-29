package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ziegler.kighelper.data.camera.CameraAppInfo

/** 设置页内置的常见相机预设。 */
internal data class CameraPreset(
    val label: String, val packageName: String, val activityName: String
)

private val cameraPresets = listOf(
    CameraPreset(
        "美图秀秀", "com.mt.mtxx.mtxx", "com.mt.mtxx.camera.view.CameraActivity"
    ),
    CameraPreset(
        "醒图", "com.xt.retouch", "com.xt.retouch.camera.CameraActivity"
    ),
    CameraPreset(
        "美颜相机",
        "com.meitu.meiyancamera",
        "com.meitu.myxj.selfie.merge.activity.SelfieCameraActivity"
    ),
    CameraPreset(
        "B612 咔叽",
        "com.campmobile.snowcamera",
        "com.linecorp.b612.android.activity.ActivityCamera"
    ),
    CameraPreset(
        "轻颜", "com.gorgeous.lite", "com.light.beauty.mainpage.MainActivity"
    ),
)

/**
 * 第三方相机选择器。
 *
 * 同时展示常用预设和所有可启动应用，支持按名称、包名、Activity 搜索。
 */
@Composable
internal fun CameraAppPickerDialog(
    apps: List<CameraAppInfo>,
    selectedPackage: String?,
    onSelect: (CameraAppInfo) -> Unit,
    onSelectPreset: (CameraPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val filteredPresets = remember(normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            cameraPresets
        } else {
            cameraPresets.filter {
                it.label.contains(normalizedQuery, ignoreCase = true) || it.packageName.contains(
                    normalizedQuery, ignoreCase = true
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
                    normalizedQuery, ignoreCase = true
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
                    placeholder = { Text("搜索应用") },
                    singleLine = true
                )
            }
            if (filteredPresets.isNotEmpty()) {
                item {
                    Text(
                        "推荐预设",
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
                                text = if (installed) {
                                    preset.packageName
                                } else {
                                    "${preset.packageName}（未安装）"
                                },
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
                    "全部应用",
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
                            model = icon, contentDescription = null, modifier = Modifier.size(32.dp)
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
