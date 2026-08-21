package com.ziegler.kighelper.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid
import com.ziegler.kighelper.ui.screens.settings.ColorModeSelector
import com.ziegler.kighelper.ui.screens.settings.DarkModeOptions

@Composable
fun ThemeStep(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "选择主题",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "自定义应用的外观风格",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "颜色模式",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        DarkModeOptions(
            selectedMode = settings.darkMode,
            onModeSelected = settingsViewModel::updateDarkMode,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "主题颜色",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        ColorModeSelector(
            colorMode = settings.colorMode,
            onColorModeChange = settingsViewModel::updateColorMode,
            modifier = Modifier.fillMaxWidth()
        )

        if (settings.colorMode == 1) {
            Spacer(modifier = Modifier.height(12.dp))
            PresetColorGrid(
                selectedIndex = settings.presetColorIndex,
                onColorSelected = settingsViewModel::updatePresetColorIndex
            )
        }

        if (settings.colorMode == 2) {
            Spacer(modifier = Modifier.height(12.dp))
            CustomColorSelector(
                customColor = settings.customColor,
                onClick = { showColorPicker = true }
            )
        }

        if (showColorPicker) {
            ColorPickerDialog(
                initialColor = settings.customColor,
                onColorSelected = { color ->
                    settingsViewModel.updateCustomColor(color)
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }
    }
}
