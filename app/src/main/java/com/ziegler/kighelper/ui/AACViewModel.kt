package com.ziegler.kighelper.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository

class AACViewModel(private val repository: PhraseRepository) : ViewModel() {
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

    fun movePhrase(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in phraseList.indices || toIndex !in phraseList.indices) return

        val item = phraseList.removeAt(fromIndex)
        phraseList.add(toIndex, item)

        repository.savePhrases(phraseList)
    }

    fun resetToDefault() {
        repository.clearAll()
        loadPhrases()
    }
}