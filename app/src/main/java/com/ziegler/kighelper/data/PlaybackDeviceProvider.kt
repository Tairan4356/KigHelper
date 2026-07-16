package com.ziegler.kighelper.data

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提供当前可用的音频输出设备列表，并在设备连接 / 断开时自动刷新。
 *
 * 以 [Singleton] 形式存活于整个应用进程：设备回调随进程销毁自动释放，
 * 因此无需手动反注册。StateFlow 对多线程访问是线程安全的。
 */
@Singleton
class PlaybackDeviceProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _devices = MutableStateFlow<List<PlaybackDevice>>(emptyList())
    val devices: StateFlow<List<PlaybackDevice>> = _devices.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
    }

    init {
        refresh()
        // handler 传 null 时回调投递到主线程 Looper，避免阻塞 binder 线程。
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
    }

    /** 拉取一次最新设备列表并发布到 [devices]。 */
    fun refresh() {
        val excludedTypes = setOf(
            PlaybackDeviceType.EARPIECE,
            PlaybackDeviceType.BLUETOOTH_SCO,
            PlaybackDeviceType.UNKNOWN
        )
        _devices.value = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .map { it.toPlaybackDevice() }
            .filter { it.type !in excludedTypes }
    }

    /**
     * 根据持久化的设备 id 解析出真实 [AudioDeviceInfo]，供 MediaPlayer.setPreferredDevice 使用。
     *
     * 当传入 [PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID] 或设备已断开时返回 null，
     * 调用方应跳过 setPreferredDevice 调用，让系统自行路由。
     */
    fun resolveAudioDeviceInfo(id: Int): AudioDeviceInfo? {
        if (id == PLAYBACK_DEVICE_SYSTEM_DEFAULT_ID) return null
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.id == id }
    }
}
