package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 外部相机模式的引导卡片；照片由外部应用保存，应用只负责拉起相机。
 */
@Composable
internal fun ExternalCameraCard(
    appLabel: String?, onLaunch: () -> Unit, enabled: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "已选择 ${appLabel ?: "系统相机"}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onLaunch, enabled = enabled) {
                Text("启动相机")
            }
        }
    }
}
