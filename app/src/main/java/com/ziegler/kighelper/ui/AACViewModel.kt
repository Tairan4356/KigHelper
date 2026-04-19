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
        phraseList.add(Phrase(label = label, speech = speech))
        repository.savePhrases(phraseList)
    }

    fun deletePhrase(phrase: Phrase) {
        phraseList.remove(phrase)
        repository.savePhrases(phraseList)
    }

    fun resetToDefault() {
        phraseList.clear()
        repository.savePhrases(emptyList())
        loadPhrases()
    }
}