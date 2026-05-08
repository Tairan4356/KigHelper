package com.ziegler.kighelper.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 短语数据仓库，负责数据的持久化存储（当前使用 SharedPreferences + Gson）
 */
class PhraseRepository(context: Context) {
    private val prefs = context.getSharedPreferences("aac_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val PHRASES_KEY = "phrases_key"

    /**
     * 从本地存储获取所有短语，若为空则返回默认列表
     */
    suspend fun getPhrases(): List<Phrase> = withContext(Dispatchers.IO) {
        val json = prefs.getString(PHRASES_KEY, null) ?: return@withContext getDefaultPhrases()
        return@withContext try {
            val type = object : TypeToken<List<Phrase>>() {}.type
            gson.fromJson<List<Phrase>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultPhrases() // 解析失败（如版本升级导致字段不兼容）时返回默认值
        }
    }

    /**
     * 将短语列表序列化并保存到本地
     */
    suspend fun savePhrases(phrases: List<Phrase>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(phrases)
        prefs.edit(commit = true) {
            putString(PHRASES_KEY, json)
        }
    }

    /**
     * 重置数据
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit { remove(PHRASES_KEY) }
    }

    private fun getDefaultPhrases() = listOf(
        Phrase(label = "你好", speech = "你好"),
        Phrase(label = "谢谢", speech = "谢谢你"),
        Phrase(label = "我的角色", speech = "我今天出的角色是……"),
        Phrase(label = "不能说话", speech = "我现在不能说话，可以打字沟通"),
        Phrase(label = "喝水", speech = "我想喝点水")
    )
}