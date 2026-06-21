package com.ziegler.kighelper.ui.components

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
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

private const val PREFS_NAME = "permission_dialog_state"
private const val KEY_NOTIFICATION_DIALOG_SHOWN = "notification_dialog_shown"
private const val KEY_OVERLAY_DIALOG_SHOWN = "overlay_dialog_shown"

@Composable
fun PermissionHandler() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val notificationPermissionResult = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "通知权限未开启，锁屏快捷控制可能受限", Toast.LENGTH_SHORT)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        // Android 13+ needs explicit notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                val dialogShown = prefs.getBoolean(KEY_NOTIFICATION_DIALOG_SHOWN, false)
                if (!dialogShown) {
                    notificationPermissionResult.launch(Manifest.permission.POST_NOTIFICATIONS)
                    prefs.edit().putBoolean(KEY_NOTIFICATION_DIALOG_SHOWN, true).apply()
                }
            }
        }

        // Overlay permission: show dialog only on first launch, toast on subsequent
        if (!WindowConfig.canDrawOverlays(context)) {
            val dialogShown = prefs.getBoolean(KEY_OVERLAY_DIALOG_SHOWN, false)
            if (!dialogShown) {
                showDialog = true
            } else {
                Toast.makeText(
                    context, "锁屏显示权限未开启，部分功能可能受限", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                prefs.edit().putBoolean(KEY_OVERLAY_DIALOG_SHOWN, true).apply()
                Toast.makeText(
                    context, "已忽略权限，部分功能可能无法在锁屏生效", Toast.LENGTH_SHORT
                ).show()
            },
            title = { Text("需要锁屏显示权限") },
            text = { Text("为了能在锁屏时使用，请开启「显示在其他应用上」或「锁屏显示」权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    prefs.edit().putBoolean(KEY_OVERLAY_DIALOG_SHOWN, true).apply()
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
                    prefs.edit().putBoolean(KEY_OVERLAY_DIALOG_SHOWN, true).apply()
                    Toast.makeText(
                        context, "已忽略权限，部分功能可能无法在锁屏生效", Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text("取消")
                }
            })
    }
}
