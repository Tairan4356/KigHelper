package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings as StateFlow<AppSettings>

    fun updateFontSize(size: Float) {
        settingsRepository.updateFontSize(size)
    }

    fun updateDarkMode(mode: Int) {
        settingsRepository.updateDarkMode(mode)
    }

    fun updateDynamicColor(enabled: Boolean) {
        settingsRepository.updateDynamicColor(enabled)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        settingsRepository.updateHapticFeedback(enabled)
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        settingsRepository.updateNotificationEnabled(enabled)
    }
}
