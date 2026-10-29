package com.ziegler.kighelper.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.utils.WindowConfig

/** 通知、锁屏和悬浮窗权限设置。 */
@Composable
internal fun SettingsPermissionSection(
    settings: AppSettings, viewModel: SettingsViewModel
) {
    val context = LocalContext.current
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
                    color = if (WindowConfig.canDrawOverlays(context)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            TextButton(onClick = {
                try {
                    context.startActivity(WindowConfig.getOverlayPermissionIntent(context))
                    val appName =
                        context.applicationInfo.loadLabel(context.packageManager).toString()
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
