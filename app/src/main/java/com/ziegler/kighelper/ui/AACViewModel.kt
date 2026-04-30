package com.ziegler.kighelper.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository

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
        phraseList.clear()
        phraseList.addAll(repository.getPhrases())
    }

    fun addPhrase(label: String, speech: String) {
        val newPhrase = Phrase(label = label, speech = speech)
        phraseList.add(newPhrase)
        saveToDisk()
    }

    fun deletePhrase(phrase: Phrase) {
        phraseList.remove(phrase)
        saveToDisk()
    }

    /**
     * 调整短语顺序
     */
    fun movePhrase(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in phraseList.indices || toIndex !in phraseList.indices) return

        val item = phraseList.removeAt(fromIndex)
        phraseList.add(toIndex, item)

        saveToDisk()
    }

    fun resetToDefault() {
        repository.clearAll()
        loadPhrases()
    }

    /**
     * 统一保存入口
     */
    private fun saveToDisk() {
        repository.savePhrases(phraseList.toList())
    }
}