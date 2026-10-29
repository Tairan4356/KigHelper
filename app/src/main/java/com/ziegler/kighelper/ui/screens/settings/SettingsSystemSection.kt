package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.data.PlaybackDevice
import com.ziegler.kighelper.ui.SettingsViewModel

/** 播放设备、触感和返回动画设置。 */
@Composable
internal fun SettingsSystemSection(
    settings: AppSettings,
    playbackDevices: List<PlaybackDevice>,
    viewModel: SettingsViewModel,
    onTestDevice: (Int) -> Unit
) {
    SettingSection(title = "系统", icon = Icons.Filled.Tune) {
        Text(
            "播放设备",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        PlaybackDeviceSelector(
            selectedDeviceId = settings.playbackDeviceId,
            devices = playbackDevices,
            onDeviceSelected = viewModel::updatePlaybackDevice,
            onTestDevice = onTestDevice
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "选择应用播放语音或音频时使用的输出设备",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "功能",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingSwitch(
            title = "触感反馈",
            subtitle = "点击短语时震动",
            checked = settings.hapticFeedback,
            onCheckedChange = viewModel::updateHapticFeedback
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitch(
            title = "预见式返回动画",
            subtitle = "Android 14+ 手势返回时页面跟随手势动画",
            checked = settings.predictiveBackEnabled,
            onCheckedChange = viewModel::updatePredictiveBackEnabled
        )
    }
}
