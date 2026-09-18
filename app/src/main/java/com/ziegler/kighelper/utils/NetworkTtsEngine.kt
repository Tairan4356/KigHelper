package com.ziegler.kighelper.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.ziegler.kighelper.data.NetworkTtsRepository
import com.ziegler.kighelper.data.PlaybackDeviceProvider
import com.ziegler.kighelper.data.SettingsRepository
import com.ziegler.kighelper.data.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 网络合成引擎：调用 OpenAI 兼容的 POST {baseUrl}/audio/speech 接口（Authorization: Bearer）。
 * 生成结果写入与端侧引擎相同的 SpeechAudioCache（播放前先查缓存）。
 */
class NetworkTtsEngine(
    context: Context,
    private val networkTtsRepository: NetworkTtsRepository,
    private val playbackDeviceProvider: PlaybackDeviceProvider,
    private val settingsRepository: SettingsRepository
) {
    private val appContext = context.applicationContext
    private val audioCache = SpeechAudioCache(context)
    private val audioPlayer = SpeechAudioPlayer()
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var requestToken: Long = 0L

    /** 朗读文本；成功处理（命中缓存并播放，或已发起异步合成）返回 true，配置缺失返回 false */
    fun speak(text: String, profile: VoiceProfile): Boolean {
        val cachedAudio = audioCache.getIfExists(text, profile, configSalt())
        if (cachedAudio != null) {
            return audioPlayer.play(cachedAudio, resolvePreferredDevice())
        }
        if (!networkTtsRepository.getConfig().isUsable) return false

        val token = ++requestToken
        engineScope.launch {
            runCatching {
                val targetFile = audioCache.resolve(text, profile, configSalt())
                synthesizeToFile(text, profile, targetFile)
                if (token == requestToken) {
                    audioPlayer.play(targetFile, resolvePreferredDevice())
                }
            }.onFailure { error ->
                Log.w(TAG, "网络 TTS 合成失败，跳过朗读", error)
            }
        }
        return true
    }

    /** 只合成到缓存不播放，供一键生成/自动生成使用；失败返回 null */
    suspend fun generateToCache(text: String, profile: VoiceProfile): File? =
        withContext(Dispatchers.IO) {
            audioCache.getIfExists(text, profile, configSalt())?.let { return@withContext it }
            if (!networkTtsRepository.getConfig().isUsable) return@withContext null
            val targetFile = audioCache.resolve(text, profile, configSalt())
            runCatching {
                synthesizeToFile(text, profile, targetFile)
                targetFile.takeIf { it.exists() && it.length() > 0L }
            }.getOrNull()
        }

    fun stop() {
        requestToken++
        audioPlayer.stop()
    }

    fun shutdown() {
        stop()
        engineScope.cancel()
    }

    private fun configSalt(): String {
        val config = networkTtsRepository.getConfig()
        return buildString {
            append(config.baseUrl.trim()).append('|')
            append(config.modelId.trim()).append('|')
            append(config.voiceId.trim())
        }
    }

    private fun synthesizeToFile(text: String, profile: VoiceProfile, targetFile: File) {
        val config = networkTtsRepository.getConfig()
        check(config.isUsable) { "网络合成配置不完整" }
        val payload = Gson().toJson(
            mapOf(
                "model" to config.modelId,
                "input" to text,
                "voice" to config.voiceId,
                "response_format" to RESPONSE_FORMAT
            )
        )
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/audio/speech")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("网络 TTS 请求失败：HTTP ${response.code}")
            }
            val body = response.body
            body.byteStream().use { input ->
                targetFile.parentFile?.mkdirs()
                targetFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolvePreferredDevice(): android.media.AudioDeviceInfo? {
        val settings =
            (settingsRepository.settings as kotlinx.coroutines.flow.StateFlow<com.ziegler.kighelper.data.AppSettings>).value
        return playbackDeviceProvider.resolveAudioDeviceInfo(settings.playbackDeviceId)
    }

    private companion object {
        private const val TAG = "NetworkTts"
        private const val RESPONSE_FORMAT = "mp3"
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 120L
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}