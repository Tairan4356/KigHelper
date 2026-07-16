package com.ziegler.kighelper.utils

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.RequiresApi
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.data.PlaybackDeviceProvider
import com.ziegler.kighelper.data.SettingsRepository
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.data.VoiceProfile
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Locale

/**
 * 语音合成管理类
 * 注意：必须在 Activity 或 Application 生命周期中正确调用 shutDown() 防止内存泄漏
 */
class TTSManager(
    context: Context,
    private val playbackDeviceProvider: PlaybackDeviceProvider,
    private val settingsRepository: SettingsRepository
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech = TextToSpeech(appContext, this)
    private val offlineNeuralTtsEngine =
        OfflineNeuralTtsEngine(appContext, playbackDeviceProvider, settingsRepository)
    private val systemAudioPlayer = SpeechAudioPlayer()
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
        if (profile.engineOrDefault == VoiceEngineType.DISABLED) return
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
     * 朗读文本到指定设备
     * @param text 要转语音的文字
     * @param profile 声线配置
     * @param targetDevice 目标播放设备，null 表示跟随系统设置
     */
    fun speakTo(text: String, profile: VoiceProfile, targetDevice: android.media.AudioDeviceInfo?) {
        if (profile.engineOrDefault == VoiceEngineType.DISABLED) return
        val content = normalizeText(text, profile).trim()
        if (content.isEmpty()) return

        if (profile.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL) {
            stopSystemTts()
            val handledByOfflineEngine = offlineNeuralTtsEngine.speak(content, profile)
            if (handledByOfflineEngine) return
        }

        speakWithSystemTts(content, profile, targetDevice)
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
        systemAudioPlayer.stop()
        tts.shutdown()
        isReady = false
    }

    private fun speakWithSystemTts(
        content: String, profile: VoiceProfile, targetDevice: android.media.AudioDeviceInfo? = null
    ) {
        offlineNeuralTtsEngine.stop()
        systemAudioPlayer.stop()
        if (isReady) {
            val params = profile.toTtsParams()
            tts.setSpeechRate(params.speechRate)
            tts.setPitch(params.pitch)

            val tempFile = File(appContext.cacheDir, "tts_system_output.wav")
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == UTTERANCE_ID) {
                        val deviceInfo = targetDevice ?: resolvePreferredDevice()
                        systemAudioPlayer.play(tempFile, deviceInfo)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                }
            })
            tts.synthesizeToFile(content, null, tempFile, UTTERANCE_ID)
        } else {
            pendingSystemSpeech = content to profile
        }
    }

    private fun stopSystemTts() {
        if (isReady) {
            tts.stop()
        }
        systemAudioPlayer.stop()
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolvePreferredDevice(): android.media.AudioDeviceInfo? {
        val settings = (settingsRepository.settings as StateFlow<AppSettings>).value
        return playbackDeviceProvider.resolveAudioDeviceInfo(settings.playbackDeviceId)
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
