package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ColorModeSelector(
    colorMode: Int, onColorModeChange: (Int) -> Unit, modifier: Modifier = Modifier
) {
    SettingConnectedButtonGroup(
        options = listOf("跟随系统", "预设颜色", "自定义"),
        selectedIndex = colorMode,
        onSelected = onColorModeChange,
        modifier = modifier
    )
}