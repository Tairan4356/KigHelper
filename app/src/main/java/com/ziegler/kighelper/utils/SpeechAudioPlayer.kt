package com.ziegler.kighelper.utils

import android.media.AudioDeviceInfo
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

class SpeechAudioPlayer {
    private var player: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.P)
    fun play(file: File, deviceInfo: AudioDeviceInfo? = null): Boolean {
        if (!file.exists() || file.length() == 0L) return false

        stop()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                it.release()
                if (player === it) {
                    player = null
                }
            }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                if (player === mp) {
                    player = null
                }
                true
            }
            prepare()
            if (deviceInfo != null) setPreferredDevice(deviceInfo)
            start()
        }
        return true
    }

    fun stop() {
        player?.runCatching {
            if (isPlaying) {
                stop()
            }
            release()
        }
        player = null
    }
}
