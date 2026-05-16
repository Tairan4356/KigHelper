package com.ziegler.kighelper.utils

import android.media.MediaPlayer
import java.io.File

class SpeechAudioPlayer {
    private var player: MediaPlayer? = null

    fun play(file: File): Boolean {
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
