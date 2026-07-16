package com.ziegler.kighelper.data

import android.media.AudioDeviceInfo

/**
 * 系统默认播放设备的占位 id。
 *
 * 表示不主动指定输出设备，由系统按当前路由策略决定（如自动选择已连接的蓝牙耳机）。
 */
const val PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID: Int = -1

/**
 * 播放设备类型，对 Android [AudioDeviceInfo] 原始 type 的语义化封装，
 * 仅用于 UI 展示与归类，不参与系统路由决策。
 */
enum class PlaybackDeviceType(val displayName: String) {
    SPEAKER("扬声器"),
    EARPIECE("听筒"),
    WIRED_HEADSET("有线耳机"),
    BLUETOOTH_A2DP("蓝牙耳机"),
    BLUETOOTH_SCO("蓝牙通话"),
    HDMI("HDMI"),
    DOCK("底座"),
    USB("USB"),
    UNKNOWN("其他");

    companion object {
        fun fromAudioDeviceType(type: Int): PlaybackDeviceType = when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> SPEAKER
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> EARPIECE
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> WIRED_HEADSET

            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> BLUETOOTH_A2DP
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> BLUETOOTH_SCO
            AudioDeviceInfo.TYPE_HDMI -> HDMI
            AudioDeviceInfo.TYPE_DOCK -> DOCK
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET -> USB

            else -> UNKNOWN
        }
    }
}

/**
 * UI 层使用的播放设备模型，与 Android [AudioDeviceInfo] 解耦，
 * 避免在 Compose / ViewModel 层直接持有系统对象。
 *
 * @property id 系统分配的设备 id，对应 [AudioDeviceInfo.getId]，可持久化。
 * @property name 设备名称，缺省时退化为类型名称。
 * @property type 设备类型，用于归类展示。
 */
data class PlaybackDevice(
    val id: Int,
    val name: String,
    val type: PlaybackDeviceType
) {
    /** 下拉列表使用的组合名称，例如 "蓝牙耳机 · AirPods Pro"。 */
    val displayName: String
        get() = if (name.isNotBlank() && name != type.displayName) {
            "${type.displayName} · $name"
        } else {
            type.displayName
        }
}

/** 将系统 [AudioDeviceInfo] 转换为 UI 模型。 */
internal fun AudioDeviceInfo.toPlaybackDevice(): PlaybackDevice {
    val resolvedType = PlaybackDeviceType.fromAudioDeviceType(type)
    val productName = productName?.toString()?.takeIf { it.isNotBlank() }
    return PlaybackDevice(
        id = id,
        name = productName ?: resolvedType.displayName,
        type = resolvedType
    )
}
