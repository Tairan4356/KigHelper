package com.ziegler.kighelper.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.CHINESE
            isReady = true
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AAC_ID")
        }
    }

    fun shutDown() {
        tts.stop()
        tts.shutdown()
    }
}