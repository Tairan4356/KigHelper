package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * 将竖屏横向组件整体顺时针旋转 90°。
 *
 * 外层按旋转后的宽高占位，内部仍用原始尺寸测量并绘制，避免组件被压缩。
 */
@Composable
internal fun RotatedControl(
    originalWidth: Dp,
    originalHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(width = originalHeight, height = originalWidth),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(width = originalWidth, height = originalHeight)
                .graphicsLayer { rotationZ = -90f }) {
            content()
        }
    }
}
