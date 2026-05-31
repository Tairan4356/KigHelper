package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.DisplayState
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.data.PhraseShare
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 核心业务逻辑层，负责短语列表的加载、编辑和持久化。
 */
class AACViewModel(private val repository: PhraseRepository) : ViewModel() {
    private val _displayState = MutableStateFlow(DisplayState())
    val displayState: StateFlow<DisplayState> = _displayState.asStateFlow()

    // 对 UI 暴露只读列表，避免页面直接修改业务状态
    private val _phraseList = MutableStateFlow<List<Phrase>>(emptyList())
    val phraseList: StateFlow<List<Phrase>> = _phraseList.asStateFlow()

    private val _groupList = MutableStateFlow<List<PhraseGroup>>(emptyList())
    val groupList: StateFlow<List<PhraseGroup>> = _groupList.asStateFlow()

    private val _isPhrasesLoading = MutableStateFlow(true)
    val isPhrasesLoading: StateFlow<Boolean> = _isPhrasesLoading.asStateFlow()

    private val _isShowingInitialHint = MutableStateFlow(true)
    val isShowingInitialHint: StateFlow<Boolean> = _isShowingInitialHint.asStateFlow()

    init {
        loadPhrases()
    }

    private fun loadPhrases() {
        viewModelScope.launch {
            _isPhrasesLoading.value = true
            try {
                val groups = repository.getGroups()
                val phrases = repository.getPhrases()
                _groupList.value = ensureDefaultGroup(groups)
                _phraseList.value = phrases
            } finally {
                _isPhrasesLoading.value = false
            }
        }
    }

    fun addPhrase(label: String, speech: String, groupId: String = PhraseGroup.DEFAULT_ID) {
        val normalizedLabel = label.trim()
        val normalizedSpeech = speech.trim()
        if (normalizedLabel.isEmpty() || normalizedSpeech.isEmpty()) return

        val newPhrase = Phrase(label = normalizedLabel, speech = normalizedSpeech, groupId = groupId)
        _phraseList.value = _phraseList.value + newPhrase
        persistCurrentPhrases()
    }

    fun deletePhrase(phrase: Phrase) {
        _phraseList.value = _phraseList.value - phrase
        persistCurrentPhrases()
    }

    fun updatePhrase(id: String, newLabel: String, newSpeech: String, newGroupId: String? = null) {
        val normalizedLabel = newLabel.trim()
        val normalizedSpeech = newSpeech.trim()
        if (normalizedLabel.isEmpty() || normalizedSpeech.isEmpty()) return

        _phraseList.value = _phraseList.value.map { phrase ->
            if (phrase.id == id) {
                phrase.copy(
                    label = normalizedLabel,
                    speech = normalizedSpeech,
                    groupId = newGroupId ?: phrase.groupId
                )
            } else {
                phrase
            }
        }
        persistCurrentPhrases()
    }

    /**
     * 调整短语顺序
     */
    fun movePhrase(fromIndex: Int, toIndex: Int) {
        val currentList = _phraseList.value.toMutableList()
        if (fromIndex == toIndex) return
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _phraseList.value = currentList

        persistCurrentPhrases()
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

    fun addGroup(name: String): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return false
        if (_groupList.value.any { it.name.equals(normalizedName, ignoreCase = true) }) return false

        val maxOrder = _groupList.value.maxOfOrNull { it.order } ?: -1
        _groupList.value = _groupList.value + PhraseGroup(name = normalizedName, order = maxOrder + 1)
        persistCurrentGroups()
        return true
    }

    fun deleteGroup(groupId: String) {
        if (groupId == PhraseGroup.DEFAULT_ID) return

        _groupList.value = _groupList.value.filter { it.id != groupId }
        _phraseList.value = _phraseList.value.map { phrase ->
            if (phrase.groupId == groupId) {
                phrase.copy(groupId = PhraseGroup.DEFAULT_ID)
            } else {
                phrase
            }
        }
        persistCurrentGroups()
        persistCurrentPhrases()
    }

    fun renameGroup(groupId: String, newName: String) {
        val normalizedName = newName.trim()
        if (normalizedName.isEmpty()) return
        if (_groupList.value.any { it.id != groupId && it.name.equals(normalizedName, ignoreCase = true) }) return

        _groupList.value = _groupList.value.map { group ->
            if (group.id == groupId) {
                group.copy(name = normalizedName)
            } else {
                group
            }
        }
        persistCurrentGroups()
    }

    fun moveGroup(fromIndex: Int, toIndex: Int) {
        val currentList = _groupList.value.toMutableList()
        if (fromIndex == toIndex) return
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)

        _groupList.value = currentList.mapIndexed { index, group ->
            group.copy(order = index)
        }
        persistCurrentGroups()
    }

    fun updateGroupsOrder(updatedGroups: List<PhraseGroup>) {
        val currentGroupsById = _groupList.value.associateBy { it.id }
        val orderedGroups = updatedGroups
            .mapNotNull { updated -> currentGroupsById[updated.id] }
            .distinctBy { it.id }
            .toMutableList()

        for (group in _groupList.value) {
            if (orderedGroups.none { it.id == group.id }) {
                orderedGroups.add(group)
            }
        }

        val finalizedList = orderedGroups.mapIndexed { index, group ->
            group.copy(order = index)
        }

        _groupList.value = ensureDefaultGroup(finalizedList)
        persistCurrentGroups()
    }

    /**
     * 按 id 查找短语，供编辑页初始化表单使用
     */
    fun findPhraseById(id: String?): Phrase? {
        return _phraseList.value.firstOrNull { it.id == id }
    }

    fun showPhrase(phrase: Phrase) {
        _displayState.value = DisplayState(
            text = phrase.speech,
            isInitialHint = false
        )
    }

    fun clearDisplayText() {
        _displayState.value = DisplayState(
            text = "",
            isInitialHint = false
        )
    }

    /**
     * 记录最后使用的短语
     */
    fun markPhraseAsUsed(phrase: Phrase) {
        viewModelScope.launch {
            repository.saveLastUsedPhrase(phrase)
        }
    }

    /**
     * 获取最后使用的短语
     */
    suspend fun getLastUsedPhrase(): Phrase? {
        return repository.getLastUsedPhrase()
    }

    fun exportAll(): String {
        return PhraseShare.export(_groupList.value, _phraseList.value)
    }

    fun importData(content: String): Boolean {
        val data = PhraseShare.import(content) ?: return false

        val currentGroups = _groupList.value.toMutableList()
        val currentPhrases = _phraseList.value.toMutableList()

        val existingGroupIds = currentGroups.map { it.id }.toMutableSet()
        val existingPhraseIds = currentPhrases.map { it.id }.toMutableSet()
        val existingGroupNames = currentGroups.map { it.name }.toMutableSet()

        for (group in data.groups) {
            if (group.id in existingGroupIds) continue
            var name = group.name
            var suffix = 1
            while (name in existingGroupNames) {
                name = "${group.name} ($suffix)"
                suffix++
            }
            currentGroups.add(group.copy(name = name))
            existingGroupIds.add(group.id)
            existingGroupNames.add(name)
        }

        for (phrase in data.phrases) {
            if (phrase.id in existingPhraseIds) continue
            val targetGroupId = if (phrase.groupId in existingGroupIds) phrase.groupId else PhraseGroup.DEFAULT_ID
            currentPhrases.add(phrase.copy(groupId = targetGroupId))
            existingPhraseIds.add(phrase.id)
        }

        _groupList.value = currentGroups
        _phraseList.value = currentPhrases

        persistCurrentGroups()
        persistCurrentPhrases()
        return true
    }

    /**
     * 将当前内存中的列表同步到持久化存储
     */
    private fun persistCurrentPhrases() {
        val snapshot = _phraseList.value
        viewModelScope.launch {
            repository.savePhrases(snapshot)
        }
    }

    private fun persistCurrentGroups() {
        val snapshot = _groupList.value
        viewModelScope.launch {
            repository.saveGroups(snapshot)
        }
    }

    private fun ensureDefaultGroup(groups: List<PhraseGroup>): List<PhraseGroup> {
        return if (groups.any { it.id == PhraseGroup.DEFAULT_ID }) {
            groups
        } else {
            listOf(PhraseGroup(id = PhraseGroup.DEFAULT_ID, name = PhraseGroup.DEFAULT_NAME, order = 0)) + groups
        }
    }

    /**
     * 更新全部短语的排列顺序与分组合规性（用于拖拽排序后的统一同步）
     */
    fun updatePhrasesOrder(updatedPhrases: List<Phrase>) {
        _phraseList.value = updatedPhrases
        persistCurrentPhrases()
    }
}