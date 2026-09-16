package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 深色模式连接按钮组。状态全部上提，仅负责渲染与回调上抛。
 *
 * @param selectedMode 当前模式：0=跟随系统，1=浅色，2=深色。
 * @param onModeSelected 用户切换模式时回调。
 */
@Composable
fun DarkModeOptions(
    selectedMode: Int, onModeSelected: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val selectedIndex = when (selectedMode) {
        1 -> 0
        2 -> 1
        else -> 2
    }
    SettingConnectedButtonGroup(
        options = listOf("浅色", "深色", "跟随系统"),
        selectedIndex = selectedIndex,
        onSelected = { index ->
            val mode = when (index) {
                0 -> 1
                1 -> 2
                else -> 0
            }
            onModeSelected(mode)
        },
        modifier = modifier
    )
}