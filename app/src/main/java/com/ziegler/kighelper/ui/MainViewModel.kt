package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

/**
 * 主 ViewModel，协调 PhraseViewModel、GroupViewModel 和 DisplayViewModel
 * 使用 @HiltViewModel 注解，支持依赖注入
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    phraseRepository: PhraseRepository
) : ViewModel() {

    private val phraseViewModel = PhraseViewModel(phraseRepository)
    private val groupViewModel = GroupViewModel(phraseRepository)
    private val displayViewModel = DisplayViewModel()

    val phraseList get() = phraseViewModel.phraseList
    val groupList get() = groupViewModel.groupList
    val isPhrasesLoading get() = phraseViewModel.isPhrasesLoading
    val displayState get() = displayViewModel.displayState

    fun addPhrase(
        label: String,
        speech: String,
        groupId: String = "default",
        audioPath: String? = null,
        cardColor: Long? = null
    ) {
        phraseViewModel.addPhrase(label, speech, groupId, audioPath, cardColor)
    }

    fun deletePhrase(phrase: Phrase) {
        phraseViewModel.deletePhrase(phrase)
    }

    fun updatePhrase(
        id: String,
        newLabel: String,
        newSpeech: String,
        newGroupId: String? = null,
        newAudioPath: String? = null,
        newCardColor: Long? = null
    ) {
        phraseViewModel.updatePhrase(
            id, newLabel, newSpeech, newGroupId, newAudioPath, newCardColor
        )
    }

    fun showPhrase(phrase: Phrase) {
        displayViewModel.showPhrase(phrase)
    }

    fun clearDisplayText() {
        displayViewModel.clearDisplayText()
    }

    fun markPhraseAsUsed(phrase: Phrase) {
        phraseViewModel.markPhraseAsUsed(phrase)
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

    fun movePhraseToGroup(phraseId: String, targetGroupId: String) {
        phraseViewModel.movePhraseToGroup(phraseId, targetGroupId)
    }

    fun updatePhrasesOrder(updatedPhrases: List<Phrase>) {
        phraseViewModel.updatePhrasesOrder(updatedPhrases)
    }

    fun findPhraseById(id: String?): Phrase? {
        return phraseViewModel.findPhraseById(id)
    }

    suspend fun exportArchive(
        selectedGroupIds: Set<String>,
        includeAudio: Boolean,
        audioDir: File?,
        outputStream: java.io.OutputStream
    ) {
        phraseViewModel.exportArchive(
            groupList.value, selectedGroupIds, includeAudio, audioDir, outputStream
        )
    }

    suspend fun importArchive(archiveBytes: java.io.InputStream, audioDir: File?): Boolean {
        val bytes = archiveBytes.readBytes()
        val phrasesImported =
            phraseViewModel.importArchive(bytes.inputStream(), groupList.value, audioDir)
        val pendingGroups = phraseViewModel.consumePendingNewGroups()
        for (group in pendingGroups) {
            groupViewModel.addGroupDirectly(group)
        }
        return phrasesImported
    }

    suspend fun importArchiveOverwrite(
        archiveBytes: java.io.InputStream, audioDir: File?
    ): Boolean {
        val bytes = archiveBytes.readBytes()
        val phrasesImported = phraseViewModel.importArchiveOverwrite(bytes.inputStream(), audioDir)
        val pendingGroups = phraseViewModel.consumePendingNewGroups()
        for (group in pendingGroups) {
            groupViewModel.addGroupDirectly(group)
        }
        return phrasesImported
    }
}
