package com.ziegler.kighelper.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

/**
 * 短语仓库接口。
 * ViewModel 只依赖这个抽象，方便后续替换为 Room、DataStore 等实现。
 */
interface PhraseRepository {
    fun observePhrases(): Flow<List<Phrase>>
    fun observeGroups(): Flow<List<PhraseGroup>>

    suspend fun getPhrases(): List<Phrase>
    suspend fun savePhrases(phrases: List<Phrase>)
    suspend fun getGroups(): List<PhraseGroup>
    suspend fun saveGroups(groups: List<PhraseGroup>)
    suspend fun resetPhrases()
    suspend fun saveLastUsedPhrase(phrase: Phrase)
    suspend fun getLastUsedPhrase(): Phrase?
}

/**
 * 基于 SharedPreferences 的本地短语存储实现。
 */
class SharedPreferencesPhraseRepository(
    context: Context,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PhraseRepository {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val phraseListType: Type = object : TypeToken<List<Phrase>>() {}.type
    private val groupListType: Type = object : TypeToken<List<PhraseGroup>>() {}.type

    // 内部事件通道，当数据更新时通知订阅者
    private val _phrasesFlow = MutableSharedFlow<List<Phrase>>(replay = 1)
    private val _groupsFlow = MutableSharedFlow<List<PhraseGroup>>(replay = 1)

    init {
        // 初始加载一次数据，使 Flow 拥有初值
        _phrasesFlow.tryEmit(getPhrasesSync())
        _groupsFlow.tryEmit(getGroupsSync())
    }

    override fun observePhrases(): Flow<List<Phrase>> = _phrasesFlow.asSharedFlow()
    override fun observeGroups(): Flow<List<PhraseGroup>> = _groupsFlow.asSharedFlow()

    override suspend fun getPhrases(): List<Phrase> = withContext(ioDispatcher) {
        getPhrasesSync()
    }

    private fun getPhrasesSync(): List<Phrase> {
        val json = prefs.getString(PHRASES_KEY, null) ?: return Phrase.DEFAULT_PHRASES
        return runCatching {
            gson.fromJson<List<Phrase>>(json, phraseListType) ?: Phrase.DEFAULT_PHRASES
        }.getOrElse { error ->
            Log.w(TAG, "短语数据解析失败，已回退到默认短语", error)
            Phrase.DEFAULT_PHRASES
        }
    }

    /**
     * 将短语列表序列化并保存到本地
     */
    override suspend fun savePhrases(phrases: List<Phrase>) = withContext(ioDispatcher) {
        val json = gson.toJson(phrases)
        prefs.edit(commit = true) { putString(PHRASES_KEY, json) }
        _phrasesFlow.emit(phrases)
    }

    override suspend fun getGroups(): List<PhraseGroup> = withContext(ioDispatcher) {
        getGroupsSync()
    }

    private fun getGroupsSync(): List<PhraseGroup> {
        val json = prefs.getString(GROUPS_KEY, null) ?: return PhraseGroup.DEFAULT_GROUPS
        return runCatching {
            gson.fromJson<List<PhraseGroup>>(json, groupListType) ?: PhraseGroup.DEFAULT_GROUPS
        }.getOrElse { error ->
            Log.w(TAG, "分组数据解析失败，已回退到默认分组", error)
            PhraseGroup.DEFAULT_GROUPS
        }
    }

    override suspend fun saveGroups(groups: List<PhraseGroup>) = withContext(ioDispatcher) {
        val json = gson.toJson(groups)
        prefs.edit(commit = true) { putString(GROUPS_KEY, json) }
        _groupsFlow.emit(groups)
    }

    override suspend fun resetPhrases() = withContext(ioDispatcher) {
        prefs.edit(commit = true) {
            remove(PHRASES_KEY)
            remove(GROUPS_KEY)
        }
        _phrasesFlow.emit(Phrase.DEFAULT_PHRASES)
        _groupsFlow.emit(PhraseGroup.DEFAULT_GROUPS)
    }

    /**
     * 保存最后使用的短语
     */
    override suspend fun saveLastUsedPhrase(phrase: Phrase) = withContext(ioDispatcher) {
        val json = gson.toJson(phrase)
        prefs.edit(commit = true) { putString(LAST_PHRASE_KEY, json) }
    }

    /**
     * 获取最后使用的短语
     */
    override suspend fun getLastUsedPhrase(): Phrase? = withContext(ioDispatcher) {
        val json = prefs.getString(LAST_PHRASE_KEY, null) ?: return@withContext null
        runCatching {
            gson.fromJson(json, Phrase::class.java)
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "PhraseRepository"
        private const val PREFS_NAME = "aac_prefs"
        private const val PHRASES_KEY = "phrases_key"
        private const val GROUPS_KEY = "groups_key"
        private const val LAST_PHRASE_KEY = "last_phrase_key"
    }
}