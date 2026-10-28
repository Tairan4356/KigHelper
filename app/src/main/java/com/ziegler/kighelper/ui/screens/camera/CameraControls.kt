package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.camera.CameraMode

/**
 * 内置相机底部操作栏。
 *
 * 拍照模式显示快门，扫码模式把主操作换成相册导入；闪光灯和镜头切换两种模式都保留。
 */
@Composable
internal fun CameraControls(
    mode: CameraMode,
    flashOn: Boolean,
    enabled: Boolean,
    onFlashChange: (Boolean) -> Unit,
    onPickGallery: () -> Unit,
    onSwitchCamera: () -> Unit,
    onCapture: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onFlashChange(!flashOn) },
            enabled = enabled
        ) {
            Icon(
                imageVector = if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = if (flashOn) "关闭闪光灯" else "打开闪光灯"
            )
        }

        if (mode == CameraMode.PHOTO) {
            FilledIconButton(
                onClick = onCapture,
                enabled = enabled,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "拍照",
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            FilledTonalButton(
                onClick = onPickGallery,
                enabled = enabled
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("相册")
            }
        }

        IconButton(
            onClick = onSwitchCamera,
            enabled = enabled
        ) {
            Icon(Icons.Filled.SwitchCamera, contentDescription = "切换镜头")
        }
    }
}
