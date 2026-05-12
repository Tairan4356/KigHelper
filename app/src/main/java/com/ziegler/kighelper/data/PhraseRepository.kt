package com.ziegler.kighelper.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

/**
 * 短语仓库接口。
 * ViewModel 只依赖这个抽象，方便后续替换为 Room、DataStore 等实现。
 */
interface PhraseRepository {
    suspend fun getPhrases(): List<Phrase>
    suspend fun savePhrases(phrases: List<Phrase>)
    suspend fun resetPhrases()
    suspend fun saveLastUsedPhrase(phrase: Phrase)
    suspend fun getLastUsedPhrase(): Phrase?
}

/**
 * 基于 SharedPreferences 的本地短语存储实现。
 */
class SharedPreferencesPhraseRepository(
    context: Context,
    private val gson: Gson = Gson()
) : PhraseRepository {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val phraseListType: Type = object : TypeToken<List<Phrase>>() {}.type

    /**
     * 从本地存储获取所有短语，若为空则返回默认列表
     */
    override suspend fun getPhrases(): List<Phrase> = withContext(Dispatchers.IO) {
        val json = prefs.getString(PHRASES_KEY, null) ?: return@withContext defaultPhrases()

        runCatching {
            gson.fromJson<List<Phrase>>(json, phraseListType) ?: defaultPhrases()
        }.getOrElse { error ->
            Log.w(TAG, "短语数据解析失败，已回退到默认短语", error)
            defaultPhrases()
        }
    }

    /**
     * 将短语列表序列化并保存到本地
     */
    override suspend fun savePhrases(phrases: List<Phrase>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(phrases)
        prefs.edit(commit = true) {
            putString(PHRASES_KEY, json)
        }
    }

    /**
     * 清除用户数据，下一次读取会自动返回默认短语
     */
    override suspend fun resetPhrases() = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) {
            remove(PHRASES_KEY)
        }
    }

    /**
     * 保存最后使用的短语
     */
    override suspend fun saveLastUsedPhrase(phrase: Phrase) = withContext(Dispatchers.IO) {
        val json = gson.toJson(phrase)
        prefs.edit(commit = true) {
            putString(LAST_PHRASE_KEY, json)
        }
    }

    /**
     * 获取最后使用的短语
     */
    override suspend fun getLastUsedPhrase(): Phrase? = withContext(Dispatchers.IO) {
        val json = prefs.getString(LAST_PHRASE_KEY, null) ?: return@withContext null
        runCatching {
            gson.fromJson(json, Phrase::class.java)
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "PhraseRepository"
        private const val PREFS_NAME = "aac_prefs"
        private const val PHRASES_KEY = "phrases_key"
        private const val LAST_PHRASE_KEY = "last_phrase_key"

        private fun defaultPhrases() = listOf(
            Phrase(label = "你好", speech = "你好"),
            Phrase(label = "谢谢", speech = "谢谢你"),
            Phrase(label = "我的角色", speech = "我今天出的角色是……"),
            Phrase(label = "不能说话", speech = "我现在不能说话，可以打字沟通"),
            Phrase(label = "喝水", speech = "我想喝点水")
        )
    }
}
