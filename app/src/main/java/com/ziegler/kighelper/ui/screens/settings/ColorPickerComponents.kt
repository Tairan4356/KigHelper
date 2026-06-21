package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.ui.theme.PresetColorNames
import com.ziegler.kighelper.ui.theme.PresetColors

@Composable
fun ColorModeSelector(
    colorMode: Int,
    onColorModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row {
            SettingRadioButton(
                label = "跟随系统",
                selected = colorMode == 0,
                onClick = { onColorModeChange(0) },
                modifier = Modifier.weight(1f)
            )
            SettingRadioButton(
                label = "预设颜色",
                selected = colorMode == 1,
                onClick = { onColorModeChange(1) },
                modifier = Modifier.weight(1f)
            )
            SettingRadioButton(
                label = "自定义",
                selected = colorMode == 2,
                onClick = { onColorModeChange(2) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PresetColorGrid(
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (rowIndex in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (colIndex in 0..3) {
                    val index = rowIndex * 4 + colIndex
                    if (index < PresetColors.size) {
                        PresetColorItem(
                            color = PresetColors[index],
                            name = PresetColorNames[index],
                            selected = selectedIndex == index,
                            onClick = { onColorSelected(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetColorItem(
    color: Color,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CustomColorSelector(
    customColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(customColor.toInt()))
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Button(onClick = onClick) {
            Text("选择颜色")
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    val red = ((selectedColor shr 16) and 0xFF).toInt()
    val green = ((selectedColor shr 8) and 0xFF).toInt()
    val blue = (selectedColor and 0xFF).toInt()
    var hexInput by remember { mutableStateOf(String.format("%06X", initialColor.toInt() and 0xFFFFFF)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 颜色预览
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(selectedColor.toInt()))
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 颜色编码输入
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("#", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                            if (filtered.length <= 6) {
                                hexInput = filtered
                                if (filtered.length == 6) {
                                    val colorLong = filtered.toLong(16) or 0xFF000000
                                    selectedColor = colorLong
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // RGB 滑条
                ColorChannelSlider(
                    label = "R",
                    value = red,
                    color = Color.Red,
                    onValueChange = { newRed ->
                        selectedColor = 0xFF000000 or (newRed.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
                        hexInput = String.format("%06X", selectedColor.toInt() and 0xFFFFFF)
                    }
                )
                ColorChannelSlider(
                    label = "G",
                    value = green,
                    color = Color.Green,
                    onValueChange = { newGreen ->
                        selectedColor = 0xFF000000 or (red.toLong() shl 16) or (newGreen.toLong() shl 8) or blue.toLong()
                        hexInput = String.format("%06X", selectedColor.toInt() and 0xFFFFFF)
                    }
                )
                ColorChannelSlider(
                    label = "B",
                    value = blue,
                    color = Color.Blue,
                    onValueChange = { newBlue ->
                        selectedColor = 0xFF000000 or (red.toLong() shl 16) or (green.toLong() shl 8) or newBlue.toLong()
                        hexInput = String.format("%06X", selectedColor.toInt() and 0xFFFFFF)
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(selectedColor) }) {
                Text("确定")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    color: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$value",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(32.dp)
        )
    }
}
