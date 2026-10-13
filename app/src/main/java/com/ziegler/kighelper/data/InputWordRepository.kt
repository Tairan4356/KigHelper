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
 * 常用字词仓库接口。
 * ViewModel 只依赖这个抽象，方便后续替换为 Room、DataStore 等实现。
 */
interface InputWordRepository {
    fun observeWords(): Flow<List<InputWord>>
    fun observeGroups(): Flow<List<InputWordGroup>>

    suspend fun getWords(): List<InputWord>
    suspend fun saveWords(words: List<InputWord>)
    suspend fun getGroups(): List<InputWordGroup>
    suspend fun saveGroups(groups: List<InputWordGroup>)
    suspend fun reset()
}

/**
 * 基于 SharedPreferences 的常用字词存储实现。
 */
class SharedPreferencesInputWordRepository(
    context: Context,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : InputWordRepository {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val wordListType: Type = object : TypeToken<List<InputWord>>() {}.type
    private val groupListType: Type = object : TypeToken<List<InputWordGroup>>() {}.type

    // 内部事件通道，当数据更新时通知订阅者
    private val _wordsFlow = MutableSharedFlow<List<InputWord>>(replay = 1)
    private val _groupsFlow = MutableSharedFlow<List<InputWordGroup>>(replay = 1)

    init {
        // 初始加载一次数据，使 Flow 拥有初值
        _wordsFlow.tryEmit(getWordsSync())
        _groupsFlow.tryEmit(getGroupsSync())
    }

    override fun observeWords(): Flow<List<InputWord>> = _wordsFlow.asSharedFlow()
    override fun observeGroups(): Flow<List<InputWordGroup>> = _groupsFlow.asSharedFlow()

    override suspend fun getWords(): List<InputWord> = withContext(ioDispatcher) {
        getWordsSync()
    }

    private fun getWordsSync(): List<InputWord> {
        val json = prefs.getString(WORDS_KEY, null) ?: return InputWordData.DEFAULT_WORDS
        return runCatching {
            gson.fromJson<List<InputWord>>(json, wordListType) ?: InputWordData.DEFAULT_WORDS
        }.getOrElse { error ->
            Log.w(TAG, "字词数据解析失败，已回退到默认字词", error)
            InputWordData.DEFAULT_WORDS
        }
    }

    /**
     * 将字词列表序列化并保存到本地
     */
    override suspend fun saveWords(words: List<InputWord>) = withContext(ioDispatcher) {
        val json = gson.toJson(words)
        prefs.edit(commit = true) { putString(WORDS_KEY, json) }
        _wordsFlow.emit(words)
    }

    override suspend fun getGroups(): List<InputWordGroup> = withContext(ioDispatcher) {
        getGroupsSync()
    }

    private fun getGroupsSync(): List<InputWordGroup> {
        val json = prefs.getString(GROUPS_KEY, null) ?: return InputWordGroup.DEFAULT_GROUPS
        return runCatching {
            gson.fromJson<List<InputWordGroup>>(json, groupListType)
                ?: InputWordGroup.DEFAULT_GROUPS
        }.getOrElse { error ->
            Log.w(TAG, "字词分组数据解析失败，已回退到默认分组", error)
            InputWordGroup.DEFAULT_GROUPS
        }
    }

    override suspend fun saveGroups(groups: List<InputWordGroup>) = withContext(ioDispatcher) {
        val json = gson.toJson(groups)
        prefs.edit(commit = true) { putString(GROUPS_KEY, json) }
        _groupsFlow.emit(groups)
    }

    override suspend fun reset() = withContext(ioDispatcher) {
        prefs.edit(commit = true) {
            remove(WORDS_KEY)
            remove(GROUPS_KEY)
        }
        _wordsFlow.emit(InputWordData.DEFAULT_WORDS)
        _groupsFlow.emit(InputWordGroup.DEFAULT_GROUPS)
    }

    private companion object {
        private const val TAG = "InputWordRepository"
        private const val PREFS_NAME = "input_word_prefs"
        private const val WORDS_KEY = "words_key"
        private const val GROUPS_KEY = "groups_key"
    }
}
