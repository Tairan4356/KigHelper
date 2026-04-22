package com.ziegler.kighelper.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository

class AACViewModel(private val repository: PhraseRepository) : ViewModel() {
    // 使用 SnapshotStateList，Compose 会自动监听列表内容的增删
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
        repository.savePhrases(phraseList)
    }

    fun deletePhrase(phrase: Phrase) {
        phraseList.remove(phrase)
        repository.savePhrases(phraseList)
    }

    fun resetToDefault() {
        // 清除持久化数据
        repository.clearAll()
        // 重新加载（Repository 内部逻辑会因为找不到数据而加载默认值）
        loadPhrases()
    }
}