package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.theme.FontType
import com.ziegler.kighelper.utils.InstalledFont
import kotlin.math.roundToInt

/** 字体、字重和文字大小设置。 */
@Composable
internal fun SettingsFontSection(
    settings: AppSettings,
    installedFonts: List<InstalledFont>,
    viewModel: SettingsViewModel,
    onShowFontManagement: () -> Unit
) {
    SettingSection(title = "字体", icon = Icons.Filled.TextFormat) {
        FontTypeSelector(
            selectedType = settings.fontType, onTypeSelected = { type, baseName ->
                viewModel.updateFontType(type)
                viewModel.selectCustomFont(baseName)
                val builtinCount = FontType.entries.size
                if (type < builtinCount) {
                    val weights = FontType.entries[type].availableWeights
                    viewModel.updateFontWeight(
                        snapToNearestWeight(settings.fontWeight, weights)
                    )
                }
            }, installedFonts = installedFonts
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onShowFontManagement, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.TextFormat, contentDescription = null, modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("字体管理")
        }
        Spacer(modifier = Modifier.height(16.dp))

        val builtinCount = FontType.entries.size
        if (settings.fontType < builtinCount) {
            val weights = FontType.entries[settings.fontType].availableWeights
            if (weights.size > 1) {
                SettingSlider(
                    title = "字重", value = weights.indexOf(
                        snapToNearestWeight(settings.fontWeight, weights)
                    ).toFloat(), onValueChange = { index ->
                        viewModel.updateFontWeight(weights[index.roundToInt()])
                    }, valueRange = 0f..(weights.size - 1).toFloat(), steps = weights.size - 2
                )
            }
        } else {
            val selectedCustomFont = settings.selectedCustomFont
            val customFont = installedFonts.find { it.baseName == selectedCustomFont }
            if (customFont != null && customFont.weights.size > 1) {
                val weightValues = customFont.weights.map { it.weight }
                SettingSlider(
                    title = "字重",
                    value = weightValues.indexOf(
                        snapToNearestWeight(settings.fontWeight, weightValues)
                    ).toFloat(),
                    onValueChange = { index ->
                        viewModel.updateFontWeight(weightValues[index.roundToInt()])
                    },
                    valueRange = 0f..(weightValues.size - 1).toFloat(),
                    steps = weightValues.size - 2
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingSlider(
            title = "文字显示区字体大小",
            value = settings.fontSize,
            onValueChange = viewModel::updateFontSize,
            valueRange = 0.8f..2.0f,
            valueText = "${(settings.fontSize * 100).roundToInt()}%"
        )
    }
}

private fun snapToNearestWeight(value: Int, weights: List<Int>): Int {
    return weights.minByOrNull { kotlin.math.abs(it - value) } ?: 400
}
