package com.lhtstudio.kigtts.app.audio

import android.util.Log

object EspeakNative {
    @Volatile private var loaded = false
    @Volatile private var initialized = false

    private fun ensureLoaded() {
        if (!loaded) {
            try {
                System.loadLibrary("espeak_jni")
                loaded = true
            } catch (e: Throwable) {
                Log.e("EspeakNative", "Failed to load espeak_jni", e)
            }
        }
    }

    private external fun nativeInit(dataPath: String): Boolean
    private external fun nativePhonemize(text: String, voice: String): String

    fun ensureInit(dataPath: String): Boolean {
        ensureLoaded()
        if (!loaded) return false
        if (initialized) return true
        initialized = nativeInit(dataPath)
        return initialized
    }

    fun phonemize(text: String, voice: String): String {
        if (!loaded || !initialized) return ""
        return nativePhonemize(text, voice)
    }
}
