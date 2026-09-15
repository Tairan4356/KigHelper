package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.ArchiveMediaFile
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.data.PhraseMedia
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.data.PhraseShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 短语管理 ViewModel，负责短语的加载、增删改查和排序
 */
class PhraseViewModel(private val repository: PhraseRepository) : ViewModel() {
    private val _phraseList = MutableStateFlow<List<Phrase>>(emptyList())
    val phraseList: StateFlow<List<Phrase>> = _phraseList.asStateFlow()

    private val _isPhrasesLoading = MutableStateFlow(true)
    val isPhrasesLoading: StateFlow<Boolean> = _isPhrasesLoading.asStateFlow()

    private val _pendingNewGroups = mutableListOf<PhraseGroup>()

    init {
        loadPhrases()
    }

    private fun loadPhrases() {
        viewModelScope.launch {
            _isPhrasesLoading.value = true
            try {
                val phrases = repository.getPhrases()
                _phraseList.value = phrases
            } finally {
                _isPhrasesLoading.value = false
            }
        }
    }

    fun addPhrase(
        label: String,
        speech: String,
        groupId: String = "default",
        audioPath: String? = null,
        cardColor: Long? = null,
        imagePath: String? = null,
        videoPath: String? = null
    ) {
        val normalizedLabel = label.trim()
        val normalizedSpeech = speech.trim()
        if (normalizedLabel.isEmpty() || !hasContent(
                normalizedSpeech, audioPath, imagePath, videoPath
            )
        ) return

        val newPhrase = Phrase(
            label = normalizedLabel,
            speech = normalizedSpeech,
            groupId = groupId,
            audioPath = audioPath,
            cardColor = cardColor,
            imagePath = imagePath,
            videoPath = videoPath
        )
        _phraseList.value += newPhrase
        persistCurrentPhrases()
    }

    fun deletePhrase(phrase: Phrase) {
        _phraseList.value -= phrase
        persistCurrentPhrases()
    }

    fun updatePhrase(
        id: String,
        newLabel: String,
        newSpeech: String,
        newGroupId: String? = null,
        newAudioPath: String? = null,
        newCardColor: Long? = null,
        newImagePath: String? = null,
        newVideoPath: String? = null
    ) {
        val normalizedLabel = newLabel.trim()
        val normalizedSpeech = newSpeech.trim()
        if (normalizedLabel.isEmpty() || !hasContent(
                normalizedSpeech, newAudioPath, newImagePath, newVideoPath
            )
        ) return

        _phraseList.value = _phraseList.value.map { phrase ->
            if (phrase.id == id) {
                phrase.copy(
                    label = normalizedLabel,
                    speech = normalizedSpeech,
                    groupId = newGroupId ?: phrase.groupId,
                    audioPath = newAudioPath,
                    cardColor = newCardColor,
                    imagePath = newImagePath,
                    videoPath = newVideoPath
                )
            } else {
                phrase
            }
        }
        persistCurrentPhrases()
    }

    private fun hasContent(
        speech: String, audioPath: String?, imagePath: String?, videoPath: String?
    ): Boolean {
        return speech.isNotEmpty() || !audioPath.isNullOrBlank() || !imagePath.isNullOrBlank() || !videoPath.isNullOrBlank()
    }

    fun movePhraseToGroup(phraseId: String, targetGroupId: String) {
        _phraseList.value = _phraseList.value.map { phrase ->
            if (phrase.id == phraseId) {
                phrase.copy(groupId = targetGroupId)
            } else {
                phrase
            }
        }
        persistCurrentPhrases()
    }

    fun findPhraseById(id: String?): Phrase? {
        return _phraseList.value.firstOrNull { it.id == id }
    }

    fun markPhraseAsUsed(phrase: Phrase) {
        viewModelScope.launch {
            repository.saveLastUsedPhrase(phrase)
        }
    }

    suspend fun getLastUsedPhrase(): Phrase? {
        return repository.getLastUsedPhrase()
    }

    fun updatePhrasesOrder(updatedPhrases: List<Phrase>) {
        _phraseList.value = updatedPhrases
        persistCurrentPhrases()
    }

    fun exportAll(groups: List<PhraseGroup>): String {
        return PhraseShare.export(groups, _phraseList.value)
    }

    suspend fun exportArchive(
        groups: List<PhraseGroup>,
        selectedGroupIds: Set<String>,
        includeMedia: Boolean,
        mediaDirs: Map<String, File>,
        outputStream: java.io.OutputStream
    ) = withContext(Dispatchers.IO) {
        val filteredPhrases = if (selectedGroupIds.isEmpty()) {
            _phraseList.value
        } else {
            _phraseList.value.filter { it.groupId in selectedGroupIds }
        }
        val exportPhrases = if (includeMedia) {
            filteredPhrases
        } else {
            filteredPhrases.map {
                it.copy(audioPath = null, imagePath = null, videoPath = null)
            }
        }
        val mediaFiles = if (includeMedia) {
            buildList {
                for (category in PhraseMedia.CATEGORIES) {
                    if (mediaDirs[category] == null) continue
                    for (phrase in filteredPhrases) {
                        val path = phraseMediaPath(phrase, category) ?: continue
                        val file = File(path)
                        if (file.exists()) {
                            add(ArchiveMediaFile(category, file.name, file))
                        }
                    }
                }
            }
        } else {
            emptyList()
        }
        PhraseShare.exportArchive(
            groups, exportPhrases, mediaFiles, outputStream
        )
    }

    private fun phraseMediaPath(phrase: Phrase, category: String): String? = when (category) {
        PhraseMedia.AUDIO -> phrase.audioPath
        PhraseMedia.IMAGE -> phrase.imagePath
        PhraseMedia.VIDEO -> phrase.videoPath
        else -> null
    }

    fun importData(
        content: String, existingGroups: List<PhraseGroup>
    ): Boolean {
        val data = PhraseShare.import(content) ?: return false

        val currentPhrases = _phraseList.value.toMutableList()
        val existingPhraseIds = currentPhrases.map { it.id }.toMutableSet()
        val existingGroupIds = existingGroups.map { it.id }.toMutableSet()

        for (phrase in data.phrases) {
            if (phrase.id in existingPhraseIds) continue
            val targetGroupId =
                if (phrase.groupId in existingGroupIds) phrase.groupId else "default"
            currentPhrases.add(phrase.copy(groupId = targetGroupId))
            existingPhraseIds.add(phrase.id)
        }

        _phraseList.value = currentPhrases
        persistCurrentPhrases()
        return true
    }

    suspend fun importArchive(
        archiveBytes: java.io.InputStream,
        existingGroups: List<PhraseGroup>,
        mediaDirs: Map<String, File>
    ): Boolean = withContext(Dispatchers.IO) {
        val (parsed, originalGroups) = PhraseShare.importArchive(archiveBytes, mediaDirs)
            ?: return@withContext false

        val existingGroupByName = existingGroups.associateBy { it.name }
        val idMapping = mutableMapOf<String, String>()

        for (origGroup in originalGroups) {
            val matched = existingGroupByName[origGroup.name]
            if (matched != null) {
                idMapping[origGroup.id] = matched.id
            } else {
                val newGroupId = UUID.randomUUID().toString()
                idMapping[origGroup.id] = newGroupId
                _pendingNewGroups.add(
                    PhraseGroup(
                        id = newGroupId, name = origGroup.name, order = origGroup.order
                    )
                )
            }
        }

        val currentPhrases = _phraseList.value.toMutableList()
        val currentPhraseLabels = currentPhrases.map { "${it.label}:${it.groupId}" }.toMutableSet()

        for (phrase in parsed.phrases) {
            val labelKey = "${phrase.label}:${idMapping[phrase.groupId] ?: PhraseGroup.DEFAULT_ID}"
            if (labelKey in currentPhraseLabels) continue
            val targetGroupId = idMapping[phrase.groupId] ?: PhraseGroup.DEFAULT_ID
            val localPhrase = PhraseShare.remapMediaPaths(phrase, mediaDirs)
            currentPhrases.add(localPhrase.copy(groupId = targetGroupId))
            currentPhraseLabels.add(labelKey)
        }

        _phraseList.value = currentPhrases
        persistCurrentPhrases()
        true
    }

    suspend fun importArchiveOverwrite(
        archiveBytes: java.io.InputStream, mediaDirs: Map<String, File>
    ): Boolean = withContext(Dispatchers.IO) {
        mediaDirs.values.forEach { dir -> dir.listFiles()?.forEach { it.delete() } }

        val (parsed, originalGroups) = PhraseShare.importArchive(archiveBytes, mediaDirs)
            ?: return@withContext false

        val idMapping = mutableMapOf<String, String>()
        for (origGroup in originalGroups) {
            val newGroupId = UUID.randomUUID().toString()
            idMapping[origGroup.id] = newGroupId
            _pendingNewGroups.add(
                PhraseGroup(
                    id = newGroupId, name = origGroup.name, order = origGroup.order
                )
            )
        }

        val phrasesWithLocalPaths = parsed.phrases.map { phrase ->
            PhraseShare.remapMediaPaths(
                phrase.copy(groupId = idMapping[phrase.groupId] ?: PhraseGroup.DEFAULT_ID),
                mediaDirs
            )
        }

        _phraseList.value = phrasesWithLocalPaths
        persistCurrentPhrases()
        true
    }

    private fun persistCurrentPhrases() {
        val snapshot = _phraseList.value
        viewModelScope.launch {
            repository.savePhrases(snapshot)
        }
    }

    fun consumePendingNewGroups(): List<PhraseGroup> {
        val groups = _pendingNewGroups.toList()
        _pendingNewGroups.clear()
        return groups
    }
}
