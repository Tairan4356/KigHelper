package com.ziegler.kighelper.utils

import android.content.Context
import com.ziegler.kighelper.data.VoiceProfile
import java.io.File
import java.security.MessageDigest

class SpeechAudioCache(context: Context) {
    private val cacheDir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME).apply {
        mkdirs()
    }

    fun resolve(text: String, profile: VoiceProfile, extraKey: String = ""): File {
        return File(cacheDir, "${cacheKey(text, profile, extraKey)}.wav")
    }

    fun getIfExists(text: String, profile: VoiceProfile, extraKey: String = ""): File? {
        return resolve(text, profile, extraKey).takeIf { it.exists() && it.length() > 0L }
    }

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
        cacheDir.parentFile?.let { parent ->
            File(parent, SYSTEM_OUTPUT_FILE).delete()
        }
    }

    fun cacheSizeBytes(): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun cacheKey(text: String, profile: VoiceProfile, extraKey: String): String {
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
            append(extraKey)
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
        private const val SYSTEM_OUTPUT_FILE = "tts_system_output.wav"
    }
}
