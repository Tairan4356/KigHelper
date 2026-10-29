package com.ziegler.kighelper.ui.screens.camera

import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * 读取设备物理方向对应的 Surface 旋转。
 *
 * 与窗口方向不同：即使系统锁定竖屏、界面仍保持竖屏，只要用户横持设备，
 * 这里仍会返回 [Surface.ROTATION_90] 或 [Surface.ROTATION_270]，用于让
 * CameraX 输出方向正确的 4:3 照片。
 */
@Composable
internal fun rememberDeviceRotation(): Int {
    val context = LocalContext.current
    val view = LocalView.current
    var rotation by remember {
        mutableIntStateOf(view.display?.rotation ?: Surface.ROTATION_0)
    }

    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val nextRotation = when {
                    orientation >= 315 || orientation < 45 -> Surface.ROTATION_0
                    orientation < 135 -> Surface.ROTATION_270
                    orientation < 225 -> Surface.ROTATION_180
                    else -> Surface.ROTATION_90
                }
                if (nextRotation != rotation) {
                    rotation = nextRotation
                }
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose { listener.disable() }
    }

    return rotation
}
