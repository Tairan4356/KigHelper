package com.ziegler.kighelper.ui.screens.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.utils.OfflineVoiceModelFormat

/**
 * 引擎选择器组件
 */
@Composable
fun EngineSelector(
    selected: VoiceEngineType,
    onSelect: (VoiceEngineType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VoiceEngineType.entries.forEach { engine ->
            FilterChip(
                selected = selected == engine,
                onClick = { onSelect(engine) },
                label = { Text(engine.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
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
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

/**
 * 导入格式选择器组件
 */
@Composable
fun ImportFormatSelector(
    selected: OfflineVoiceModelFormat,
    onSelect: (OfflineVoiceModelFormat) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            OfflineVoiceModelFormat.VITS,
            OfflineVoiceModelFormat.PIPER,
            OfflineVoiceModelFormat.KOKORO
        ).forEach { format ->
            FilterChip(
                selected = selected == format,
                onClick = { onSelect(format) },
                label = { Text(format.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val kigvpk = OfflineVoiceModelFormat.KIGVPK
    FilterChip(
        selected = selected == kigvpk,
        onClick = { onSelect(kigvpk) },
        label = { Text(kigvpk.label + "（KIGTTS 训练器格式）") },
        modifier = Modifier.fillMaxWidth()
    )
}
