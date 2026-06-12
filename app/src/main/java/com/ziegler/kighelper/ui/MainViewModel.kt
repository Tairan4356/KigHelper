package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository
import kotlinx.coroutines.launch

/**
 * 主 ViewModel，协调 PhraseViewModel、GroupViewModel 和 DisplayViewModel
 */
class MainViewModel(
    private val phraseViewModel: PhraseViewModel,
    private val groupViewModel: GroupViewModel,
    private val displayViewModel: DisplayViewModel
) : ViewModel() {

    val phraseList get() = phraseViewModel.phraseList
    val groupList get() = groupViewModel.groupList
    val isPhrasesLoading get() = phraseViewModel.isPhrasesLoading
    val displayState get() = displayViewModel.displayState
    val isFullScreen get() = displayViewModel.isFullScreen

    fun addPhrase(label: String, speech: String, groupId: String = "default") {
        phraseViewModel.addPhrase(label, speech, groupId)
    }

    fun deletePhrase(phrase: Phrase) {
        phraseViewModel.deletePhrase(phrase)
    }

    fun updatePhrase(id: String, newLabel: String, newSpeech: String, newGroupId: String? = null) {
        phraseViewModel.updatePhrase(id, newLabel, newSpeech, newGroupId)
    }

    fun showPhrase(phrase: Phrase) {
        displayViewModel.showPhrase(phrase)
    }

    fun clearDisplayText() {
        displayViewModel.clearDisplayText()
    }

    fun setFullScreen(enabled: Boolean) {
        displayViewModel.setFullScreen(enabled)
    }

    fun markPhraseAsUsed(phrase: Phrase) {
        phraseViewModel.markPhraseAsUsed(phrase)
    }

    suspend fun getLastUsedPhrase(): Phrase? {
        return phraseViewModel.getLastUsedPhrase()
    }

    fun addGroup(name: String): Boolean {
        return groupViewModel.addGroup(name)
    }

    fun deleteGroup(groupId: String) {
        groupViewModel.deleteGroup(groupId)
        // 将属于被删除分组的短语移回默认分组
        val currentPhrases = phraseViewModel.phraseList.value
        currentPhrases.filter { it.groupId == groupId }.forEach { phrase ->
            phraseViewModel.updatePhrase(phrase.id, phrase.label, phrase.speech, "default")
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        groupViewModel.renameGroup(groupId, newName)
    }

    fun moveGroup(fromIndex: Int, toIndex: Int) {
        groupViewModel.moveGroup(fromIndex, toIndex)
    }

    fun updateGroupsOrder(updatedGroups: List<com.ziegler.kighelper.data.PhraseGroup>) {
        groupViewModel.updateGroupsOrder(updatedGroups)
    }

    fun movePhraseToGroup(phraseId: String, targetGroupId: String) {
        phraseViewModel.movePhraseToGroup(phraseId, targetGroupId)
    }

    fun updatePhrasesOrder(updatedPhrases: List<Phrase>) {
        phraseViewModel.updatePhrasesOrder(updatedPhrases)
    }

    fun findPhraseById(id: String?): Phrase? {
        return phraseViewModel.findPhraseById(id)
    }

    fun exportAll(): String {
        return phraseViewModel.exportAll(groupList.value)
    }

    fun importData(content: String): Boolean {
        val groupsImported = groupViewModel.importGroups(content)
        val phrasesImported = phraseViewModel.importData(content, groupList.value)
        return groupsImported || phrasesImported
    }
}

/**
 * MainViewModel 工厂
 */
class MainViewModelFactory(
    private val phraseRepository: PhraseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val phraseViewModel = PhraseViewModel(phraseRepository)
            val groupViewModel = GroupViewModel(phraseRepository)
            val displayViewModel = DisplayViewModel()
            return MainViewModel(phraseViewModel, groupViewModel, displayViewModel) as T
        }
        throw IllegalArgumentException("未知的 ViewModel 类型: ${modelClass.name}")
    }
}
