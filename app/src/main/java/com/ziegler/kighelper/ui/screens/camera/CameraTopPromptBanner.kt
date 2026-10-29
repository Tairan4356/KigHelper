package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.ui.CameraEvent

/**
 * 悬浮在 TopAppBar 中央的轻量成功提示。
 *
 * 使用紧凑圆角背景覆盖标题区域；提示消失后标题和操作按钮自然恢复。
 */
@Composable
internal fun CameraTopPromptBanner(
    prompt: CameraEvent.TopPrompt
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prompt.text,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
