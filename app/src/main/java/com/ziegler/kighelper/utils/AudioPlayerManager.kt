package com.ziegler.kighelper.utils

import android.media.AudioDeviceInfo
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File

/**
 * 管理导入音频文件的播放。
 * 使用 MediaPlayer 实现，播放完成后自动释放资源。
 */
class AudioPlayerManager {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * 播放指定路径的音频文件。
     * 如果当前有正在播放的音频，会先停止并释放。
     *
     * @param audioPath 音频文件的内部存储路径
     * @param deviceInfo 可选的目标播放设备，为 null 时使用系统默认路由
     * @param onCompletion 播放完成时的回调
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun play(
        audioPath: String, deviceInfo: AudioDeviceInfo? = null, onCompletion: (() -> Unit)? = null
    ) {
        stop()

        val file = File(audioPath)
        if (!file.exists()) {
            Log.w(TAG, "音频文件不存在: $audioPath")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    onCompletion?.invoke()
                    release()
                    mediaPlayer = null
                }
                prepare()
                if (deviceInfo != null) setPreferredDevice(deviceInfo)
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放音频失败", e)
            release()
        }
    }

    /**
     * 停止当前播放并释放资源。
     */
    fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) {
                Log.w(TAG, "停止播放时出错", e)
            }
        }
        mediaPlayer = null
    }

    /**
     * 释放所有资源，应在不需要时调用。
     */
    fun release() {
        stop()
    }

    private companion object {
        private const val TAG = "AudioPlayerManager"
    }
}
