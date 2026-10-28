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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.camera.CameraMode

/**
 * 拍照/扫码切换器。
 *
 * 使用 Expressive ButtonGroup；窄屏时由组件自动折叠为菜单。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CameraModeSelector(
    selected: CameraMode, enabled: Boolean, onSelected: (CameraMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        ButtonGroup(
            modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.spacedBy(
                ButtonGroupDefaults.ConnectedSpaceBetween
            ), overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            }) {
            CameraMode.entries.forEachIndexed { index, mode ->
                val label = if (mode == CameraMode.PHOTO) "拍照" else "扫码"
                val icon = if (mode == CameraMode.PHOTO) {
                    Icons.Filled.CameraAlt
                } else {
                    Icons.Filled.QrCodeScanner
                }
                customItem(buttonGroupContent = {
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        CameraMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()

                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = selected == mode,
                        onCheckedChange = { onSelected(mode) },
                        shapes = shapes,
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            icon, contentDescription = null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label)
                    }
                }, menuContent = { menuState ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        onSelected(mode)
                        menuState.dismiss()
                    })
                })
            }
        }
    }
}
