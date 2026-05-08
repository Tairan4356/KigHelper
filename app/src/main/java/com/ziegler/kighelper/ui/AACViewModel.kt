package com.ziegler.kighelper.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository
import kotlinx.coroutines.launch

/**
 * 核心业务逻辑层，管理 UI 状态
 */
class AACViewModel(private val repository: PhraseRepository) : ViewModel() {
    // 使用 mutableStateListOf 确保 UI 能自动感知列表变化（增删改）
    var phraseList = mutableStateListOf<Phrase>()
        private set

    init {
        loadPhrases()
    }

    private fun loadPhrases() {
        viewModelScope.launch {
            val phrases = repository.getPhrases()
            phraseList.clear()
            phraseList.addAll(phrases)
        }
    }

    fun addPhrase(label: String, speech: String) {
        val newPhrase = Phrase(label = label, speech = speech)
        phraseList.add(newPhrase)
        syncToRepository()
    }

    fun deletePhrase(phrase: Phrase) {
        phraseList.remove(phrase)
        syncToRepository()
    }

    fun updatePhrase(id: String, newLabel: String, newSpeech: String) {
        val index = phraseList.indexOfFirst { it.id == id }
        if (index != -1) {
            phraseList[index] = phraseList[index].copy(label = newLabel, speech = newSpeech)
            syncToRepository()
        }
    }

    /**
     * 调整短语顺序
     */
    fun movePhrase(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in phraseList.indices || toIndex !in phraseList.indices) return

        val item = phraseList.removeAt(fromIndex)
        phraseList.add(toIndex, item)

        syncToRepository()
    }

    /**
     * 恢复默认
     */
    fun resetToDefault() {
        viewModelScope.launch {
            repository.clearAll()
            val defaults = repository.getPhrases()
            phraseList.clear()
            phraseList.addAll(defaults)
        }
    }


    /**
     * 将当前内存中的列表同步到持久化存储
     */
    private fun syncToRepository() {
        // 将 SnapshotStateList 转换为普通 List 再进行序列化
        val listToSave = phraseList.toList()
        viewModelScope.launch {
            repository.savePhrases(listToSave)
        }
    }
}