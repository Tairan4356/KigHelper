package com.ziegler.kighelper.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

data class AppSettings(
    val fontSize: Float = 1.0f,
    val darkMode: Int = 0,
    val dynamicColor: Boolean = true,
    val hapticFeedback: Boolean = true,
    val notificationEnabled: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private object Keys {
        const val FONT_SIZE = "font_size"
        const val DARK_MODE = "dark_mode"
        const val DYNAMIC_COLOR = "dynamic_color"
        const val HAPTIC_FEEDBACK = "haptic_feedback"
        const val NOTIFICATION_ENABLED = "notification_enabled"
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: Flow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            fontSize = prefs.getFloat(Keys.FONT_SIZE, 1.0f),
            darkMode = prefs.getInt(Keys.DARK_MODE, 0),
            dynamicColor = prefs.getBoolean(Keys.DYNAMIC_COLOR, true),
            hapticFeedback = prefs.getBoolean(Keys.HAPTIC_FEEDBACK, true),
            notificationEnabled = prefs.getBoolean(Keys.NOTIFICATION_ENABLED, true)
        )
    }

    private fun saveAndEmit() {
        _settings.value = loadSettings()
    }

    fun updateFontSize(size: Float) {
        prefs.edit { putFloat(Keys.FONT_SIZE, size) }
        saveAndEmit()
    }

    fun updateDarkMode(mode: Int) {
        prefs.edit { putInt(Keys.DARK_MODE, mode) }
        saveAndEmit()
    }

    fun updateDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.DYNAMIC_COLOR, enabled) }
        saveAndEmit()
    }

    fun updateHapticFeedback(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.HAPTIC_FEEDBACK, enabled) }
        saveAndEmit()
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.NOTIFICATION_ENABLED, enabled) }
        saveAndEmit()
    }
}
