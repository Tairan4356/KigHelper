package com.ziegler.kighelper.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class PhraseRepository(context: Context) {
    private val prefs = context.getSharedPreferences("aac_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getPhrases(): List<Phrase> {
        val json = prefs.getString("phrases_key", null) ?: return getDefaultPhrases()
        val type = object : TypeToken<List<Phrase>>() {}.type
        return gson.fromJson(json, type)
    }

    fun savePhrases(phrases: List<Phrase>) {
        val json = gson.toJson(phrases)
        prefs.edit { putString("phrases_key", json) }
    }

    fun clearAll() {
        prefs.edit { remove("phrases_key") }
    }

    private fun getDefaultPhrases() = listOf(
        Phrase(label = "你好", speech = "你好"),
        Phrase(label = "谢谢", speech = "谢谢你"),
        Phrase(label = "我的角色", speech = "我今天出的角色是……"),
        Phrase(label = "不能说话", speech = "我现在不能说话，可以打字沟通"),
        Phrase(label = "喝水", speech = "我想喝点水")
    )
}