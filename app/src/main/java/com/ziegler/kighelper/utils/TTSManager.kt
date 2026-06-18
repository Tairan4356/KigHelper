package com.ziegler.kighelper.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.data.VoiceProfile
import java.util.Locale

/**
 * 语音合成管理类
 * 注意：必须在 Activity 或 Application 生命周期中正确调用 shutDown() 防止内存泄漏
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private val offlineNeuralTtsEngine = OfflineNeuralTtsEngine(context.applicationContext)
    private var isReady = false
    private var pendingSystemSpeech: Pair<String, VoiceProfile>? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = tts.setLanguage(Locale.CHINESE)
            isReady =
                languageResult != TextToSpeech.LANG_MISSING_DATA && languageResult != TextToSpeech.LANG_NOT_SUPPORTED
            if (isReady) {
                pendingSystemSpeech?.let { (content, profile) ->
                    pendingSystemSpeech = null
                    speakWithSystemTts(content, profile)
                }
            }
        }
    }

    /**
     * 朗读文本
     * @param text 要转语音的文字
     */
    fun speak(text: String, profile: VoiceProfile = VoiceProfile.defaultProfile()) {
        val content = normalizeText(text, profile).trim()
        if (content.isEmpty()) return

        if (profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL) {
            stopSystemTts()
            val handledByOfflineEngine = offlineNeuralTtsEngine.speak(content, profile)
            if (handledByOfflineEngine) return
        }

        speakWithSystemTts(content, profile)
    }

    /**
     * 停止当前所有朗读
     */
    fun stop() {
        pendingSystemSpeech = null
        stopSystemTts()
        offlineNeuralTtsEngine.stop()
    }

    /**
     * 释放资源
     */
    fun shutDown() {
        stop()
        offlineNeuralTtsEngine.shutdown()
        tts.shutdown()
        isReady = false
    }

    private fun speakWithSystemTts(content: String, profile: VoiceProfile) {
        offlineNeuralTtsEngine.stop()
        if (isReady) {
            val params = profile.toTtsParams()
            tts.setSpeechRate(params.speechRate)
            tts.setPitch(params.pitch)
            tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        } else {
            pendingSystemSpeech = content to profile
        }
    }

    private fun stopSystemTts() {
        if (isReady) {
            tts.stop()
        }
    }

    private fun normalizeText(text: String, profile: VoiceProfile): String {
        val normalized = text.replace(Regex("\\s+"), " ").replace("...", "……").trim()

        if (normalized.isEmpty()) return normalized

        return when {
            profile.expressiveness < 0.35f -> normalized.replace(Regex("[！!]+"), "。")
            profile.expressiveness > 0.7f && !normalized.endsWithAny("。", "！", "？", "……") -> {
                "$normalized！"
            }

            else -> normalized
        }
    }

    private fun String.endsWithAny(vararg suffixes: String): Boolean {
        return suffixes.any { endsWith(it) }
    }

    private companion object {
        private const val UTTERANCE_ID = "KIG_HELPER_TTS"
    }
}
