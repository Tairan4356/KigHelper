package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 扫码取景框提示，仅用于引导，不限制实际解码区域。
 */
@Composable
internal fun ScanFrameHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(220.dp)
            .border(2.dp, Color.White.copy(alpha = 0.9f), MaterialTheme.shapes.large)
    ) {}
}