package com.ziegler.kighelper.ui.screens.addedit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 视频导入区域：展示文件名与移除/导入按钮。
 *
 * @param videoPath 已导入视频的绝对路径，null 表示未导入
 * @param videoFileName 已导入视频的文件名
 * @param onPick 点击导入按钮时触发（由外部启动文件选择器）
 * @param onRemove 点击移除按钮时触发（由外部删除文件并清空状态）
 */
@Composable
internal fun VideoImportSection(
    videoPath: String?, videoFileName: String?, onPick: () -> Unit, onRemove: () -> Unit
) {
    if (videoPath != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = videoFileName ?: "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "移除视频")
            }
        }
        Text(
            text = "设置视频后将全屏播放并使用视频声音，播报内容可为空",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedButton(
            onClick = onPick, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Videocam, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导入视频文件")
        }
    }
}