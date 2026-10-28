package com.ziegler.kighelper.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ziegler.kighelper.data.camera.CameraProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用设置快照。
 *
 * 所有字段都有默认值，新增字段时旧版本 SharedPreferences 无需迁移即可读取。
 */
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
    val lockScreenEnabled: Boolean = false,
    val displayColorInverted: Boolean = false,
    val playbackDeviceId: Int = PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID,
    val selectedCustomFont: String? = null,
    val displayHintText: String = "点击下面按钮文字在此显示",
    val predictiveBackEnabled: Boolean = true,

    /** 拍照来源；扫码模式不受该字段影响，始终使用内置 CameraX。 */
    val cameraProvider: CameraProvider = CameraProvider.BUILT_IN,
    val thirdPartyCameraPackage: String? = null,
    val thirdPartyCameraActivity: String? = null,

    /** 是否尝试把定位写入照片 EXIF。 */
    val writePhotoLocation: Boolean = true,

    /** 拍照时是否同时录制约 2 秒实况视频。 */
    val livePhotoEnabled: Boolean = true,

    /** 开启后跳过 HDR SessionConfig，用于相机兼容性有问题的设备。 */
    val cameraCompatibilityMode: Boolean = false
)

/**
 * 应用设置的唯一持久化入口。
 *
 * SharedPreferences 负责落盘，[settings] 的 StateFlow 负责通知界面；
 * 所有更新方法都会重新读取完整快照，保证 UI 与其他消费者拿到一致数据。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    /** SharedPreferences 键名集中管理，避免字符串散落在更新方法中。 */
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
        const val DISPLAY_COLOR_INVERTED = "display_color_inverted"
        const val PLAYBACK_DEVICE_ID = "playback_device_id"
        const val SELECTED_CUSTOM_FONT = "selected_custom_font"
        const val DISPLAY_HINT_TEXT = "display_hint_text"
        const val PREDICTIVE_BACK_ENABLED = "predictive_back_enabled"
        const val CAMERA_PROVIDER = "camera_provider"
        const val THIRD_PARTY_CAMERA_PACKAGE = "third_party_camera_package"
        const val THIRD_PARTY_CAMERA_ACTIVITY = "third_party_camera_activity"
        const val LEGACY_THIRD_PARTY_CAMERA_COMPONENT = "third_party_camera_component"
        const val WRITE_PHOTO_LOCATION = "write_photo_location"
        const val LIVE_PHOTO_ENABLED = "live_photo_enabled"
        const val CAMERA_COMPATIBILITY_MODE = "camera_compatibility_mode"
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: Flow<AppSettings> = _settings.asStateFlow()

    /** 同步快照，供 ViewModel 初始化时避免等待首次收集。 */
    val currentSettings: AppSettings get() = _settings.value

    /**
     * 从 SharedPreferences 构造完整设置快照。
     *
     * 第三方相机保留旧版 `package/activity` 单字段的兼容读取。
     */
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
            lockScreenEnabled = prefs.getBoolean(Keys.LOCK_SCREEN_ENABLED, false),
            displayColorInverted = prefs.getBoolean(Keys.DISPLAY_COLOR_INVERTED, false),
            playbackDeviceId = prefs.getInt(
                Keys.PLAYBACK_DEVICE_ID, PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID
            ),
            selectedCustomFont = prefs.getString(Keys.SELECTED_CUSTOM_FONT, null),
            displayHintText = prefs.getString(
                Keys.DISPLAY_HINT_TEXT, "点击下面按钮文字在此显示"
            ) ?: "",
            predictiveBackEnabled = prefs.getBoolean(
                Keys.PREDICTIVE_BACK_ENABLED, true
            ),
            cameraProvider = CameraProvider.fromStored(
                prefs.getInt(Keys.CAMERA_PROVIDER, CameraProvider.BUILT_IN.ordinal)
            ),
            // 旧版本把 package/activity 合并存在同一个字符串中，这里按 '/' 拆分迁移。
            thirdPartyCameraPackage = prefs.getString(
                Keys.THIRD_PARTY_CAMERA_PACKAGE, null
            ) ?: prefs.getString(Keys.LEGACY_THIRD_PARTY_CAMERA_COMPONENT, null)
                ?.substringBefore('/'),
            thirdPartyCameraActivity = prefs.getString(
                Keys.THIRD_PARTY_CAMERA_ACTIVITY, null
            ) ?: prefs.getString(Keys.LEGACY_THIRD_PARTY_CAMERA_COMPONENT, null)
                ?.substringAfter('/', "")?.takeIf { it.isNotBlank() },
            writePhotoLocation = prefs.getBoolean(
                Keys.WRITE_PHOTO_LOCATION, false
            ),
            livePhotoEnabled = prefs.getBoolean(
                Keys.LIVE_PHOTO_ENABLED, false
            ),
            cameraCompatibilityMode = prefs.getBoolean(
                Keys.CAMERA_COMPATIBILITY_MODE, false
            )
        )
    }

    /** 写入后重新读取完整设置，统一触发所有设置的 StateFlow 更新。 */
    private fun saveAndEmit() {
        _settings.value = loadSettings()
    }

    // 字体与显示设置
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

    fun updateDisplayColorInverted(inverted: Boolean) {
        prefs.edit { putBoolean(Keys.DISPLAY_COLOR_INVERTED, inverted) }
        saveAndEmit()
    }

    fun updatePlaybackDeviceId(id: Int) {
        prefs.edit { putInt(Keys.PLAYBACK_DEVICE_ID, id) }
        saveAndEmit()
    }

    fun updateSelectedCustomFont(fontName: String?) {
        prefs.edit { putString(Keys.SELECTED_CUSTOM_FONT, fontName) }
        saveAndEmit()
    }

    fun updateDisplayHintText(text: String) {
        prefs.edit { putString(Keys.DISPLAY_HINT_TEXT, text) }
        saveAndEmit()
    }

    fun updatePredictiveBackEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.PREDICTIVE_BACK_ENABLED, enabled) }
        saveAndEmit()
    }

    // 相机设置
    fun updateCameraProvider(provider: CameraProvider) {
        prefs.edit { putInt(Keys.CAMERA_PROVIDER, provider.ordinal) }
        saveAndEmit()
    }

    /** packageName 与 activityName 必须成对更新，避免残留旧应用入口。 */
    fun updateThirdPartyCamera(packageName: String?, activityName: String?) {
        prefs.edit {
            putString(Keys.THIRD_PARTY_CAMERA_PACKAGE, packageName)
            putString(Keys.THIRD_PARTY_CAMERA_ACTIVITY, activityName)
        }
        saveAndEmit()
    }

    fun updateWritePhotoLocation(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.WRITE_PHOTO_LOCATION, enabled) }
        saveAndEmit()
    }

    fun updateLivePhotoEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.LIVE_PHOTO_ENABLED, enabled) }
        saveAndEmit()
    }

    /** 兼容模式会持久化，拍照超时自动降级后下次启动仍然生效。 */
    fun updateCameraCompatibilityMode(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.CAMERA_COMPATIBILITY_MODE, enabled) }
        saveAndEmit()
    }
}
