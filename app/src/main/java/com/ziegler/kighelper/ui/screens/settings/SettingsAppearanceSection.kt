package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid

/** 深色模式、主题色和显示区样式设置。 */
@Composable
internal fun SettingsAppearanceSection(
    settings: AppSettings, viewModel: SettingsViewModel
) {
    SettingSection(title = "外观", icon = Icons.Filled.Palette) {
        Text(
            "颜色模式",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        DarkModeOptions(
            selectedMode = settings.darkMode, onModeSelected = viewModel::updateDarkMode
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "主题颜色",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        var showColorPicker by remember { mutableStateOf(false) }
        ColorModeSelector(
            colorMode = settings.colorMode, onColorModeChange = viewModel::updateColorMode
        )
        if (settings.colorMode == 1) {
            Spacer(modifier = Modifier.height(16.dp))
            PresetColorGrid(
                selectedIndex = settings.presetColorIndex,
                onColorSelected = viewModel::updatePresetColorIndex
            )
        }
        if (settings.colorMode == 2) {
            Spacer(modifier = Modifier.height(16.dp))
            CustomColorSelector(
                customColor = settings.customColor, onClick = { showColorPicker = true })
        }
        if (showColorPicker) {
            ColorPickerDialog(initialColor = settings.customColor, onColorSelected = { color ->
                viewModel.updateCustomColor(color)
                showColorPicker = false
            }, onDismiss = { showColorPicker = false })
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingSwitch(
            title = "反转文字显示区颜色",
            subtitle = "文字显示区背景为浅色，文字颜色为深色",
            checked = settings.displayColorInverted,
            onCheckedChange = viewModel::updateDisplayColorInverted
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "显示区提示词",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = settings.displayHintText,
            onValueChange = viewModel::updateDisplayHintText,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
            placeholder = { Text("点击下面按钮文字在此显示") })
        Text(
            "未显示短语时的提示词，留空则不显示",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
