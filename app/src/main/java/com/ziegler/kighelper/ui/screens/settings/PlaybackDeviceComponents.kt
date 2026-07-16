package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ziegler.kighelper.data.PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID
import com.ziegler.kighelper.data.PlaybackDevice

/** "跟随系统（默认）" 选项在下拉列表中的展示文本。 */
private const val SYSTEM_DEFAULT_LABEL = "跟随系统（默认）"

/**
 * 播放设备下拉选择器。
 *
 * 状态全部上提：本组件不持有任何状态，仅根据入参渲染并通过回调上抛选择结果。
 *
 * @param selectedDeviceId 当前选中的设备 id；[PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID] 表示跟随系统。
 * @param devices 当前可用的输出设备列表。
 * @param onDeviceSelected 用户选择某项时回调，参数为设备 id（或系统默认占位 id）。
 * @param onTestDevice 用户点击试听按钮时回调，参数为设备 id（或系统默认占位 id）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackDeviceSelector(
    selectedDeviceId: Int,
    devices: List<PlaybackDevice>,
    onDeviceSelected: (Int) -> Unit,
    onTestDevice: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selectedDeviceId) {
        PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID -> SYSTEM_DEFAULT_LABEL
        else -> devices.firstOrNull { it.id == selectedDeviceId }?.displayName
            ?: SYSTEM_DEFAULT_LABEL
    }

    Row(
        modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded, onDismissRequest = { expanded = false }) {
                SettingDropdownMenuItem(
                    text = SYSTEM_DEFAULT_LABEL, onClick = {
                        onDeviceSelected(PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID)
                        expanded = false
                    })
                devices.forEach { device ->
                    SettingDropdownMenuItem(
                        text = device.displayName, onClick = {
                            onDeviceSelected(device.id)
                            expanded = false
                        })
                }
            }
        }
        IconButton(onClick = { onTestDevice(selectedDeviceId) }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "试听")
        }
    }
}
