package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 深色模式单选组。状态全部上提，仅负责渲染与回调上抛。
 *
 * @param selectedMode 当前模式：0=跟随系统，1=浅色，2=深色。
 * @param onModeSelected 用户切换模式时回调。
 */
@Composable
fun DarkModeOptions(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        SettingRadioButton(
            label = "浅色",
            selected = selectedMode == 1,
            onClick = { onModeSelected(1) },
            modifier = Modifier.weight(1f)
        )
        SettingRadioButton(
            label = "深色",
            selected = selectedMode == 2,
            onClick = { onModeSelected(2) },
            modifier = Modifier.weight(1f)
        )
        SettingRadioButton(
            label = "跟随系统",
            selected = selectedMode == 0,
            onClick = { onModeSelected(0) },
            modifier = Modifier.weight(1f)
        )
    }
}
