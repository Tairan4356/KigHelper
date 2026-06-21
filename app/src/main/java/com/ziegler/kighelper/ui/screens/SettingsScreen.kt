package com.ziegler.kighelper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.ui.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("偏好设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + navigationBarPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingSection(title = "显示") {
                    _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingSlider(
                        title = "字体大小",
                        value = settings.fontSize,
                        onValueChange = viewModel::updateFontSize,
                        valueRange = 0.8f..2.0f,
                        steps = 5,
                        valueText = "${(settings.fontSize * 100).roundToInt()}%"
                    )
                }
            }

            item {
                _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingSection(title = "深色模式") {
                    DarkModeOptions(
                        selectedMode = settings.darkMode,
                        onModeSelected = viewModel::updateDarkMode
                    )
                }
            }

            item {
                var showColorPicker by remember { mutableStateOf(false) }

                _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingSection(title = "主题颜色") {
                    _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.ColorModeSelector(
                        colorMode = settings.colorMode,
                        onColorModeChange = viewModel::updateColorMode
                    )

                    if (settings.colorMode == 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.PresetColorGrid(
                            selectedIndex = settings.presetColorIndex,
                            onColorSelected = viewModel::updatePresetColorIndex
                        )
                    }

                    if (settings.colorMode == 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.CustomColorSelector(
                            customColor = settings.customColor,
                            onClick = { showColorPicker = true }
                        )
                    }
                }

                if (showColorPicker) {
                    _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.ColorPickerDialog(
                        initialColor = settings.customColor,
                        onColorSelected = { color ->
                            viewModel.updateCustomColor(color)
                            showColorPicker = false
                        },
                        onDismiss = { showColorPicker = false }
                    )
                }
            }

            item {
                _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingSection(title = "功能") {
                    _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingSwitch(
                        title = "振动反馈",
                        subtitle = "点击短语时提供触觉反馈",
                        checked = settings.hapticFeedback,
                        onCheckedChange = viewModel::updateHapticFeedback
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingSwitch(
                        title = "通知显示",
                        subtitle = "后台时显示锁屏通知",
                        checked = settings.notificationEnabled,
                        onCheckedChange = viewModel::updateNotificationEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkModeOptions(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingRadioButton(
            label = "跟随系统",
            selected = selectedMode == 0,
            onClick = { onModeSelected(0) },
            modifier = Modifier.weight(1f)
        )
        _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingRadioButton(
            label = "浅色",
            selected = selectedMode == 1,
            onClick = { onModeSelected(1) },
            modifier = Modifier.weight(1f)
        )
        _root_ide_package_.com.ziegler.kighelper.ui.screens.settings.SettingRadioButton(
            label = "深色",
            selected = selectedMode == 2,
            onClick = { onModeSelected(2) },
            modifier = Modifier.weight(1f)
        )
    }
}
