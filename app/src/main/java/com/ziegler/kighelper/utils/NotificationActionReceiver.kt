package com.ziegler.kighelper.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * BroadcastReceiver 用于处理通知中的 TTS 重播按钮
 */
class NotificationActionReceiver : BroadcastReceiver() {
    private var tts: TextToSpeech? = null

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REPLAY_PHRASE -> {
                val phraseText = intent.getStringExtra(EXTRA_PHRASE_TEXT)
                if (!phraseText.isNullOrEmpty()) {
                    replayPhrase(context, phraseText)
                }
            }
        }
    }

    private fun replayPhrase(context: Context, text: String) {
        // 创建临时 TTS 实例进行播放
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "中文 TTS 不支持，使用默认语言")
                }
                val utteranceId = System.currentTimeMillis().toString()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // do nothing
                    }
                    override fun onDone(utteranceId: String?) {
                        tts?.shutdown()
                        tts = null
                    }
                    override fun onError(utteranceId: String?) {
                        tts?.shutdown()
                        tts = null
                    }
                })
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }
    }

    companion object {
        const val ACTION_REPLAY_PHRASE = "com.ziegler.kighelper.ACTION_REPLAY_PHRASE"
        const val EXTRA_PHRASE_TEXT = "phrase_text"
        private const val TAG = "NotificationReceiver"
    }
}
