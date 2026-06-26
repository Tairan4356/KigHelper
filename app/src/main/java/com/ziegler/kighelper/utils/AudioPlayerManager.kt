package com.ziegler.kighelper.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File

/**
 * 管理导入音频文件的播放。
 * 使用 MediaPlayer 实现，播放完成后自动释放资源。
 */
class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * 播放指定路径的音频文件。
     * 如果当前有正在播放的音频，会先停止并释放。
     *
     * @param audioPath 音频文件的内部存储路径
     * @param onCompletion 播放完成时的回调
     */
    fun play(audioPath: String, onCompletion: (() -> Unit)? = null) {
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
