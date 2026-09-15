package com.ziegler.kighelper.ui.screens.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid

private const val DefaultCardColor = 0xFF6650A4L

/**
 * 卡片颜色区域：自定义开关、预设色板与取色器。
 * 状态通过 [onChange] 回传给调用方，本组件仅持有取色器弹窗的局部状态。
 *
 * @param hasCustomColor 是否启用了自定义颜色
 * @param cardColor 当前颜色值，0 表示未设置
 * @param onChange 颜色变更回调，参数为 (是否启用自定义颜色, 颜色值)
 */
@Composable
internal fun PhraseColorSection(
    hasCustomColor: Boolean,
    cardColor: Long,
    onChange: (hasCustomColor: Boolean, color: Long) -> Unit
) {
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    val resolvedColor = if (cardColor != 0L) cardColor else DefaultCardColor

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "卡片颜色",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasCustomColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                CustomColorSelector(
                    customColor = resolvedColor,
                    onClick = { showColorPicker = true },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onChange(false, 0L) }) {
                    Icon(
                        imageVector = Icons.Default.Close, contentDescription = "恢复默认颜色"
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = { onChange(true, resolvedColor) }, modifier = Modifier.fillMaxWidth()
            ) {
                Text("自定义卡片颜色")
            }
        }

        if (hasCustomColor) {
            PresetColorGrid(
                selectedIndex = -1, onColorSelected = { index ->
                    val colors = listOf(
                        0xFF6650A4L,
                        0xFF2196F3L,
                        0xFF00BCD4L,
                        0xFF4CAF50L,
                        0xFFFFEB3BL,
                        0xFFFF9800L,
                        0xFFF44336L,
                        0xFFE91E63L
                    )
                    if (index in colors.indices) {
                        onChange(true, colors[index])
                    }
                }, showNames = false
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(initialColor = resolvedColor, onColorSelected = { color ->
            onChange(true, color)
            showColorPicker = false
        }, onDismiss = { showColorPicker = false })
    }
}