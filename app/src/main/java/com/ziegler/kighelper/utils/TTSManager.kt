package com.ziegler.kighelper.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 语音合成管理类
 * 注意：必须在 Activity 或 Application 生命周期中正确调用 shutDown() 防止内存泄漏
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.CHINESE
            isReady = true
        }
    }

    /**
     * 朗读文本
     * @param text 要转语音的文字
     */
    fun speak(text: String) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AAC_ID")
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
        tts.stop()
        tts.shutdown()
    }
}