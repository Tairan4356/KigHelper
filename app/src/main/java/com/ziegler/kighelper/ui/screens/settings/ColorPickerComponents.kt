package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid

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
