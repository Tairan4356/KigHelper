package com.ziegler.kighelper.ui.screens.addedit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * 图片导入区域：展示预览、文件名与移除/导入按钮。
 *
 * @param imagePath 已导入图片的绝对路径，null 表示未导入
 * @param imageFileName 已导入图片的文件名
 * @param onPick 点击导入按钮时触发（由外部启动文件选择器）
 * @param onRemove 点击移除按钮时触发（由外部删除文件并清空状态）
 */
@Composable
internal fun ImageImportSection(
    imagePath: String?, imageFileName: String?, onPick: () -> Unit, onRemove: () -> Unit
) {
    if (imagePath != null) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = imageFileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = imageFileName ?: "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onRemove) {
                Text("移除图片")
            }
        }
        Text(
            text = "上传图片（支持 GIF）后将显示在展示区，播报内容可为空",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedButton(
            onClick = onPick, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("导入图片 (支持 GIF)")
        }
    }
}