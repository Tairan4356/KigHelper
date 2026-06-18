package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 主 ViewModel，协调 PhraseViewModel、GroupViewModel 和 DisplayViewModel
 * 使用 @HiltViewModel 注解，支持依赖注入
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val phraseRepository: PhraseRepository
) : ViewModel() {

    private val phraseViewModel = PhraseViewModel(phraseRepository)
    private val groupViewModel = GroupViewModel(phraseRepository)
    private val displayViewModel = DisplayViewModel()

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
