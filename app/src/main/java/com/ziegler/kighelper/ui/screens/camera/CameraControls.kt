package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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

private val CONTROL_BAR_HEIGHT = 100.dp
private val LANDSCAPE_CONTROLS_MAX_WIDTH = 360.dp

/**
 * 拍照操作栏。
 *
 * 竖屏使用固定高度的横向栏；横屏右侧栏改为纵向排列，并限制最大宽度。
 */
@Composable
internal fun CameraControls(
    mode: CameraMode,
    flashOn: Boolean,
    enabled: Boolean,
    externalCamera: Boolean = false,
    vertical: Boolean = false,
    onFlashChange: (Boolean) -> Unit,
    onPickGallery: () -> Unit,
    onSwitchCamera: () -> Unit,
    onCapture: () -> Unit
) {
    if (vertical) {
        Column(
            modifier = Modifier
                .heightIn(max = LANDSCAPE_CONTROLS_MAX_WIDTH)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            if (externalCamera) {
                LaunchCameraButton(onCapture = onCapture)
            } else {
                FlashButton(
                    flashOn = flashOn, enabled = enabled, onFlashChange = onFlashChange
                )
                ShutterButton(enabled = enabled, onCapture = onCapture)
                SecondaryButton(
                    mode = mode,
                    enabled = enabled,
                    onPickGallery = onPickGallery,
                    onSwitchCamera = onSwitchCamera
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CONTROL_BAR_HEIGHT)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (externalCamera) {
            LaunchCameraButton(onCapture = onCapture)
            return@Row
        }

        FlashButton(flashOn = flashOn, enabled = enabled, onFlashChange = onFlashChange)
        ShutterButton(enabled = enabled, onCapture = onCapture)
        SecondaryButton(
            mode = mode,
            enabled = enabled,
            onPickGallery = onPickGallery,
            onSwitchCamera = onSwitchCamera
        )
    }
}

@Composable
private fun LaunchCameraButton(onCapture: () -> Unit) {
    FilledIconButton(
        onClick = onCapture, modifier = Modifier.size(72.dp)
    ) {
        Icon(Icons.Filled.RocketLaunch, contentDescription = "启动相机")
    }
}

@Composable
private fun FlashButton(
    flashOn: Boolean, enabled: Boolean, onFlashChange: (Boolean) -> Unit
) {
    IconButton(
        onClick = { onFlashChange(!flashOn) }, enabled = enabled
    ) {
        Icon(
            imageVector = if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
            contentDescription = if (flashOn) "关闭闪光灯" else "打开闪光灯"
        )
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onCapture: () -> Unit) {
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
}

@Composable
private fun SecondaryButton(
    mode: CameraMode, enabled: Boolean, onPickGallery: () -> Unit, onSwitchCamera: () -> Unit
) {
    if (mode == CameraMode.SCAN) {
        IconButton(
            onClick = onPickGallery, enabled = enabled
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = "从相册选择二维码")
        }
    } else {
        IconButton(
            onClick = onSwitchCamera, enabled = enabled
        ) {
            Icon(Icons.Filled.SwitchCamera, contentDescription = "切换镜头")
        }
    }
}
