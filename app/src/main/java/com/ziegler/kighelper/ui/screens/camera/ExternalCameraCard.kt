package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ziegler.kighelper.data.camera.CameraAppInfo

/**
 * 外部相机模式的引导卡片。
 *
 * 启动操作统一放在底部固定高度的操作栏；这里展示已选应用的图标和名称，
 * 图标优先取 Activity 图标，失败时回退到应用图标。
 */
@Composable
internal fun ExternalCameraCard(app: CameraAppInfo?) {
    val context = LocalContext.current
    val appIcon = remember(app?.flattenedComponent) {
        app?.let { info ->
            runCatching {
                context.packageManager.getActivityInfo(info.component, 0)
                    .loadIcon(context.packageManager)
            }.getOrNull() ?: runCatching {
                context.packageManager.getApplicationIcon(info.packageName)
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (appIcon != null) {
                AsyncImage(
                    model = appIcon,
                    contentDescription = app?.label,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
            } else {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "已选择 ${app?.label ?: "系统相机"}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
