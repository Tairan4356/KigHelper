package com.ziegler.kighelper.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ziegler.kighelper.utils.WindowConfig

/**
 * 统一处理启动时需要的系统权限提示。
 */
@Composable
fun PermissionHandler() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // 检查通知权限
    val notificationPermissionResult = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "通知权限未开启，锁屏快捷控制可能受限", Toast.LENGTH_SHORT)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        // Android 13+ 需要显式申请通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionResult.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 部分 ROM 的锁屏显示依赖悬浮窗权限
        if (!WindowConfig.canDrawOverlays(context)) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                Toast.makeText(
                    context, "已忽略权限，部分功能可能无法在锁屏生效", Toast.LENGTH_SHORT
                ).show()
            },
            title = { Text("需要锁屏显示权限") },
            text = { Text("为了能在锁屏时使用，请开启“显示在其他应用上”或“锁屏显示”权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    try {
                        context.startActivity(WindowConfig.getOverlayPermissionIntent(context))
                        Toast.makeText(
                            context, "请找到并开启 KigHelper 的权限", Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {
                        Toast.makeText(
                            context, "无法跳转设置，请手动开启权限", Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    Toast.makeText(
                        context, "已忽略权限，部分功能可能无法在锁屏生效", Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text("取消")
                }
            })
    }
}
