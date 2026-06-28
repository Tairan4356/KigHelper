package com.ziegler.kighelper.utils

import android.content.Context
import com.ziegler.kighelper.data.VoiceProfile
import java.io.File
import java.security.MessageDigest

class SpeechAudioCache(context: Context) {
    private val cacheDir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME).apply {
        mkdirs()
    }

    fun resolve(text: String, profile: VoiceProfile): File {
        return File(cacheDir, "${cacheKey(text, profile)}.wav")
    }

    fun getIfExists(text: String, profile: VoiceProfile): File? {
        return resolve(text, profile).takeIf { it.exists() && it.length() > 0L }
    }

    private fun cacheKey(text: String, profile: VoiceProfile): String {
        val rawKey = buildString {
            append(CACHE_VERSION)
            append('|')
            append(profile.engineOrDefault.name)
            append('|')
            append(profile.modelId.orEmpty())
            append('|')
            append(profile.speakerId)
            append('|')
            append(profile.toTtsParams())
            append('|')
            append(text.trim())
        }
        return rawKey.sha256()
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private const val CACHE_DIR_NAME = "tts_audio"
        private const val CACHE_VERSION = "v2"
    }
}
