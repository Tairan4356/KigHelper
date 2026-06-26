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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.ziegler.kighelper.utils.WindowConfig
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.components.ColorPickerDialog
import com.ziegler.kighelper.ui.components.CustomColorSelector
import com.ziegler.kighelper.ui.components.PresetColorGrid
import com.ziegler.kighelper.ui.screens.settings.ColorModeSelector
import com.ziegler.kighelper.ui.screens.settings.SettingDropdownMenuItem
import com.ziegler.kighelper.ui.screens.settings.SettingRadioButton
import com.ziegler.kighelper.ui.theme.FontType
import kotlin.math.roundToInt
import com.ziegler.kighelper.ui.screens.settings.SettingSection
import com.ziegler.kighelper.ui.screens.settings.SettingSlider
import com.ziegler.kighelper.ui.screens.settings.SettingSwitch

private fun snapToNearestWeight(value: Int, weights: List<Int>): Int {
    return weights.minByOrNull { kotlin.math.abs(it - value) } ?: 400
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, onBack: () -> Unit
) {
    val navigationBarPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(title = { Text("偏好设置") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }, scrollBehavior = scrollBehavior)
        }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding), contentPadding = PaddingValues(
                start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navigationBarPadding
            ), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingSection(title = "字体") {
                    FontTypeSelector(
                        selectedType = settings.fontType, onTypeSelected = { type ->
                            viewModel.updateFontType(type)
                            val weights = FontType.entries[type].availableWeights
                            viewModel.updateFontWeight(
                                snapToNearestWeight(
                                    settings.fontWeight, weights
                                )
                            )
                        })
                    Spacer(modifier = Modifier.height(12.dp))
                    val currentFontType = FontType.entries[settings.fontType]
                    val weights = currentFontType.availableWeights
                    if (weights.size > 1) {
                        SettingSlider(
                            title = "字重",
                            value = weights.indexOf(
                                snapToNearestWeight(
                                    settings.fontWeight, weights
                                )
                            ).toFloat(),
                            onValueChange = { index ->
                                viewModel.updateFontWeight(weights[index.roundToInt()])
                            },
                            valueRange = 0f..(weights.size - 1).toFloat(),
                            steps = weights.size - 2
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSlider(
                        title = "文字显示区字体大小",
                        value = settings.fontSize,
                        onValueChange = viewModel::updateFontSize,
                        valueRange = 0.8f..2.0f,
                        valueText = "${(settings.fontSize * 100).roundToInt()}%"
                    )
                }
            }

            item {
                SettingSection(title = "颜色模式") {
                    DarkModeOptions(
                        selectedMode = settings.darkMode, onModeSelected = viewModel::updateDarkMode
                    )
                }
            }

            item {
                var showColorPicker by remember { mutableStateOf(false) }

                SettingSection(title = "主题颜色") {
                    ColorModeSelector(
                        colorMode = settings.colorMode,
                        onColorModeChange = viewModel::updateColorMode
                    )

                    if (settings.colorMode == 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PresetColorGrid(
                            selectedIndex = settings.presetColorIndex,
                            onColorSelected = viewModel::updatePresetColorIndex
                        )
                    }

                    if (settings.colorMode == 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomColorSelector(
                            customColor = settings.customColor,
                            onClick = { showColorPicker = true })
                    }
                }

                if (showColorPicker) {
                    ColorPickerDialog(
                        initialColor = settings.customColor,
                        onColorSelected = { color ->
                            viewModel.updateCustomColor(color)
                            showColorPicker = false
                        },
                        onDismiss = { showColorPicker = false })
                }
            }

            item {
                val context = LocalContext.current

                SettingSection(title = "功能开关") {
                    SettingSwitch(
                        title = "触感反馈",
                        subtitle = "点击短语时震动",
                        checked = settings.hapticFeedback,
                        onCheckedChange = viewModel::updateHapticFeedback
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSwitch(
                        title = "通知显示",
                        subtitle = "应用置于后台时显示通知",
                        checked = settings.notificationEnabled,
                        onCheckedChange = viewModel::updateNotificationEnabled
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSwitch(
                        title = "锁屏显示",
                        subtitle = "在锁屏界面显示应用",
                        checked = settings.lockScreenEnabled,
                        onCheckedChange = viewModel::updateLockScreenEnabled
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("锁屏显示权限", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (WindowConfig.canDrawOverlays(context)) "已授予" else "未授予",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (WindowConfig.canDrawOverlays(context)) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(onClick = {
                            try {
                                context.startActivity(
                                    WindowConfig.getOverlayPermissionIntent(
                                        context
                                    )
                                )
                                Toast.makeText(
                                    context, "请找到并开启 KigHelper 的权限", Toast.LENGTH_LONG
                                ).show()
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context, "无法跳转设置，请手动开启权限", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Text(if (WindowConfig.canDrawOverlays(context)) "已授予" else "去设置")
                        }
                    }
                    Text(
                        "不同机型的系统权限设置存在差异，请根据实际情况手动开启锁屏显示、应用上层或悬浮窗等相关权限。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkModeOptions(
    selectedMode: Int, onModeSelected: (Int) -> Unit
) {
    Row {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontTypeSelector(
    selectedType: Int, onTypeSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedFont = FontType.entries[selectedType]

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selectedFont.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
            FontType.entries.forEachIndexed { index, fontType ->
                SettingDropdownMenuItem(
                    text = fontType.displayName, onClick = {
                        onTypeSelected(index)
                        expanded = false
                    })
            }
        }
    }
}

