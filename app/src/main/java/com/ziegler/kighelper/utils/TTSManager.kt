package com.ziegler.kighelper.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 语音合成管理类
 * 注意：必须在 Activity 或 Application 生命周期中正确调用 shutDown() 防止内存泄漏
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = tts.setLanguage(Locale.CHINESE)
            isReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                languageResult != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    /**
     * 朗读文本
     * @param text 要转语音的文字
     */
    fun speak(text: String) {
        val content = text.trim()
        if (isReady && content.isNotEmpty()) {
            tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    /**
     * 停止当前所有朗读
     */
    fun stop() {
        if (isReady) {
            tts.stop()
        }
    }

    /**
     * 释放资源
     */
    fun shutDown() {
        if (isReady) {
            tts.stop()
        }
        tts.shutdown()
        isReady = false
    }

    private companion object {
        private const val UTTERANCE_ID = "KIG_HELPER_TTS"
    }
}
