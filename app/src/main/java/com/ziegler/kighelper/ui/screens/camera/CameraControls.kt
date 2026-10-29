package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.camera.CameraMode

private val CONTROL_BAR_HEIGHT = 104.dp

/**
 * 底部操作栏。
 *
 * 高度固定为 [CONTROL_BAR_HEIGHT]。
 * 外部相机模式只保留一个“启动相机”主按钮；内置扫码模式同时保留相册和快门。
 */
@Composable
internal fun CameraControls(
    mode: CameraMode,
    flashOn: Boolean,
    enabled: Boolean,
    externalCamera: Boolean = false,
    onFlashChange: (Boolean) -> Unit,
    onPickGallery: () -> Unit,
    onSwitchCamera: () -> Unit,
    onCapture: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CONTROL_BAR_HEIGHT)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (externalCamera) {
            FilledIconButton(
                onClick = onCapture, modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Filled.RocketLaunch, contentDescription = "启动相机"
                )
            }
            return@Row
        }

        IconButton(
            onClick = { onFlashChange(!flashOn) }, enabled = enabled
        ) {
            Icon(
                imageVector = if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = if (flashOn) "关闭闪光灯" else "打开闪光灯"
            )
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            FilledIconButton(
                onClick = onCapture, enabled = enabled, modifier = Modifier.size(72.dp),
            ) {}
        }

        if (mode == CameraMode.SCAN) {
            IconButton(
                onClick = onPickGallery, enabled = enabled
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary, contentDescription = "从相册选择二维码"
                )
            }
        } else {

            IconButton(
                onClick = onSwitchCamera, enabled = enabled
            ) {
                Icon(Icons.Filled.SwitchCamera, contentDescription = "切换镜头")
            }
        }
    }
}
