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
    val fontType: Int = 0,
    val fontWeight: Int = 400,
    val darkMode: Int = 0,
    val colorMode: Int = 0,
    val presetColorIndex: Int = 0,
    val customColor: Long = 0xFF6650A4,
    val hapticFeedback: Boolean = true,
    val notificationEnabled: Boolean = true,
    val lockScreenEnabled: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private object Keys {
        const val FONT_SIZE = "font_size"
        const val FONT_TYPE = "font_type"
        const val FONT_WEIGHT = "font_weight"
        const val DARK_MODE = "dark_mode"
        const val COLOR_MODE = "color_mode"
        const val PRESET_COLOR_INDEX = "preset_color_index"
        const val CUSTOM_COLOR = "custom_color"
        const val HAPTIC_FEEDBACK = "haptic_feedback"
        const val NOTIFICATION_ENABLED = "notification_enabled"
        const val LOCK_SCREEN_ENABLED = "lock_screen_enabled"
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: Flow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            fontSize = prefs.getFloat(Keys.FONT_SIZE, 1.0f),
            fontType = prefs.getInt(Keys.FONT_TYPE, 0),
            fontWeight = prefs.getInt(Keys.FONT_WEIGHT, 400),
            darkMode = prefs.getInt(Keys.DARK_MODE, 0),
            colorMode = prefs.getInt(Keys.COLOR_MODE, 0),
            presetColorIndex = prefs.getInt(Keys.PRESET_COLOR_INDEX, 0),
            customColor = prefs.getLong(Keys.CUSTOM_COLOR, 0xFF6650A4),
            hapticFeedback = prefs.getBoolean(Keys.HAPTIC_FEEDBACK, true),
            notificationEnabled = prefs.getBoolean(Keys.NOTIFICATION_ENABLED, true),
            lockScreenEnabled = prefs.getBoolean(Keys.LOCK_SCREEN_ENABLED, false)
        )
    }

    private fun saveAndEmit() {
        _settings.value = loadSettings()
    }

    fun updateFontSize(size: Float) {
        prefs.edit { putFloat(Keys.FONT_SIZE, size) }
        saveAndEmit()
    }

    fun updateFontType(type: Int) {
        prefs.edit { putInt(Keys.FONT_TYPE, type) }
        saveAndEmit()
    }

    fun updateFontWeight(weight: Int) {
        prefs.edit { putInt(Keys.FONT_WEIGHT, weight) }
        saveAndEmit()
    }

    fun updateDarkMode(mode: Int) {
        prefs.edit { putInt(Keys.DARK_MODE, mode) }
        saveAndEmit()
    }

    fun updateColorMode(mode: Int) {
        prefs.edit { putInt(Keys.COLOR_MODE, mode) }
        saveAndEmit()
    }

    fun updatePresetColorIndex(index: Int) {
        prefs.edit { putInt(Keys.PRESET_COLOR_INDEX, index) }
        saveAndEmit()
    }

    fun updateCustomColor(color: Long) {
        prefs.edit { putLong(Keys.CUSTOM_COLOR, color) }
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

    fun updateLockScreenEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.LOCK_SCREEN_ENABLED, enabled) }
        saveAndEmit()
    }
}
