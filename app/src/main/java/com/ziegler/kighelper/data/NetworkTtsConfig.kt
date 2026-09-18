package com.ziegler.kighelper.data

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络合成引擎的全局配置。
 * 使用 OpenAI 兼容的 /audio/speech 接口，key 为全局配置，不随声线预设保存。
 */
data class NetworkTtsConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val modelId: String = "",
    val voiceId: String = ""
) {
    val isUsable: Boolean
        get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && modelId.isNotBlank() && voiceId.isNotBlank()
}

interface NetworkTtsRepository {
    fun getConfig(): NetworkTtsConfig
    fun saveConfig(config: NetworkTtsConfig)
}

@Singleton
class SharedPreferencesNetworkTtsRepository @Inject constructor(
    context: Context
) : NetworkTtsRepository {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    override fun getConfig(): NetworkTtsConfig {
        return NetworkTtsConfig(
            baseUrl = prefs.getString(BASE_URL_KEY, DEFAULT_CONFIG.baseUrl).orEmpty(),
            apiKey = prefs.getString(API_KEY_KEY, DEFAULT_CONFIG.apiKey).orEmpty(),
            modelId = prefs.getString(MODEL_ID_KEY, DEFAULT_CONFIG.modelId).orEmpty(),
            voiceId = prefs.getString(VOICE_ID_KEY, DEFAULT_CONFIG.voiceId).orEmpty()
        )
    }

    override fun saveConfig(config: NetworkTtsConfig) {
        prefs.edit(commit = true) {
            putString(BASE_URL_KEY, config.baseUrl)
            putString(API_KEY_KEY, config.apiKey)
            putString(MODEL_ID_KEY, config.modelId)
            putString(VOICE_ID_KEY, config.voiceId)
        }
    }

    private companion object {
        const val PREFS_NAME = "network_tts"
        const val BASE_URL_KEY = "base_url"
        const val API_KEY_KEY = "api_key"
        const val MODEL_ID_KEY = "model_id"
        const val VOICE_ID_KEY = "voice_id"
        val DEFAULT_CONFIG = NetworkTtsConfig()
    }
}