package com.ziegler.kighelper.ui.screens.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.ui.screens.settings.SettingConnectedButtonGroup
import com.ziegler.kighelper.utils.OfflineVoiceModelFormat

/**
 * 引擎选择器组件
 */
@Composable
fun EngineSelector(
    selected: VoiceEngineType, onSelect: (VoiceEngineType) -> Unit
) {
    val options = VoiceEngineType.entries
    SettingConnectedButtonGroup(
        options = options.map { it.label },
        selectedIndex = options.indexOf(selected).coerceAtLeast(0),
        onSelected = { onSelect(options[it]) })
}

/**
 * 语音滑块组件
 */
@Composable
fun VoiceSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Slider(
            value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps
        )
    }
}

/**
 * 导入格式选择器组件
 */
@Composable
fun ImportFormatSelector(
    selected: OfflineVoiceModelFormat, onSelect: (OfflineVoiceModelFormat) -> Unit
) {
    val row1 = listOf(OfflineVoiceModelFormat.VITS, OfflineVoiceModelFormat.PIPER)
    val row2 = listOf(OfflineVoiceModelFormat.KOKORO, OfflineVoiceModelFormat.KIGVPK)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingConnectedButtonGroup(
            options = row1.map { it.label },
            selectedIndex = row1.indexOf(selected),
            onSelected = { onSelect(row1[it]) })
        SettingConnectedButtonGroup(
            options = row2.map { it.label },
            selectedIndex = row2.indexOf(selected),
            onSelected = { onSelect(row2[it]) })
        if (selected == OfflineVoiceModelFormat.KIGVPK) {
            Text(
                text = "KIGTTS 训练器格式",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
