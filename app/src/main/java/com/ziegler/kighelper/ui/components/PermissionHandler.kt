package com.ziegler.kighelper.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ziegler.kighelper.utils.WindowConfig

@Composable
fun PermissionHandler() {
    val context = LocalContext.current

    val notificationPermissionResult = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "通知权限未开启，快捷控制可能受限", Toast.LENGTH_SHORT)
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
                // 仅 toast 提示，不再弹 dialog
                Toast.makeText(
                    context,
                    "通知权限未开启，快捷控制可能受限",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Overlay permission: 仅 toast 提示，不再弹 dialog
        if (!WindowConfig.canDrawOverlays(context)) {
            Toast.makeText(
                context, "锁屏显示权限未开启，部分功能可能受限", Toast.LENGTH_SHORT
            ).show()
        }
    }
}
