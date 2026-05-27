package com.ziegler.kighelper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseData
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.data.PhraseShare
import kotlinx.coroutines.launch

/**
 * 核心业务逻辑层，负责短语列表的加载、编辑和持久化。
 */
class AACViewModel(private val repository: PhraseRepository) : ViewModel() {
    private val initialHint = "点击下面按钮文字在此显示"
    private val _phraseList = mutableStateListOf<Phrase>()
    private val _groupList = mutableStateListOf<PhraseGroup>()

    // 对 UI 暴露只读列表，避免页面直接修改业务状态
    val phraseList: List<Phrase>
        get() = _phraseList

    val groupList: List<PhraseGroup>
        get() = _groupList

    var isPhrasesLoading by mutableStateOf(true)
        private set

    var displayText by mutableStateOf(initialHint)
        private set

    var isShowingInitialHint by mutableStateOf(true)
        private set

    init {
        loadPhrases()
    }

    private fun loadPhrases() {
        viewModelScope.launch {
            isPhrasesLoading = true

            try {
                val groups = repository.getGroups()
                val phrases = repository.getPhrases()
                replaceGroups(ensureDefaultGroup(groups))
                replacePhrases(phrases)
            } finally {
                isPhrasesLoading = false
            }
        }
    }

    fun addPhrase(label: String, speech: String, groupId: String = PhraseGroup.DEFAULT_ID) {
        val normalizedLabel = label.trim()
        val normalizedSpeech = speech.trim()
        if (normalizedLabel.isEmpty() || normalizedSpeech.isEmpty()) return

        _phraseList.add(Phrase(label = normalizedLabel, speech = normalizedSpeech, groupId = groupId))
        persistCurrentPhrases()
    }

    fun deletePhrase(phrase: Phrase) {
        if (_phraseList.remove(phrase)) {
            persistCurrentPhrases()
        }
    }

    fun updatePhrase(id: String, newLabel: String, newSpeech: String, newGroupId: String? = null) {
        val normalizedLabel = newLabel.trim()
        val normalizedSpeech = newSpeech.trim()
        if (normalizedLabel.isEmpty() || normalizedSpeech.isEmpty()) return

        val index = _phraseList.indexOfFirst { it.id == id }
        if (index != -1) {
            _phraseList[index] = _phraseList[index].copy(
                label = normalizedLabel,
                speech = normalizedSpeech,
                groupId = newGroupId ?: _phraseList[index].groupId
            )
            persistCurrentPhrases()
        }
    }

    /**
     * 调整短语顺序
     */
    fun movePhrase(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in _phraseList.indices || toIndex !in _phraseList.indices) return

        val item = _phraseList.removeAt(fromIndex)
        _phraseList.add(toIndex, item)

        persistCurrentPhrases()
    }

    fun movePhraseToGroup(phraseId: String, targetGroupId: String) {
        val index = _phraseList.indexOfFirst { it.id == phraseId }
        if (index == -1) return
        if (_phraseList[index].groupId == targetGroupId) return

        _phraseList[index] = _phraseList[index].copy(groupId = targetGroupId)
        persistCurrentPhrases()
    }

    fun addGroup(name: String): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return false
        if (_groupList.any { it.name.equals(normalizedName, ignoreCase = true) }) return false

        val maxOrder = _groupList.maxOfOrNull { it.order } ?: -1
        _groupList.add(PhraseGroup(name = normalizedName, order = maxOrder + 1))
        persistCurrentGroups()
        return true
    }

    fun deleteGroup(groupId: String) {
        if (groupId == PhraseGroup.DEFAULT_ID) return
        if (_groupList.removeIf { it.id == groupId }) {
            _phraseList.forEachIndexed { index, phrase ->
                if (phrase.groupId == groupId) {
                    _phraseList[index] = phrase.copy(groupId = PhraseGroup.DEFAULT_ID)
                }
            }
            persistCurrentGroups()
            persistCurrentPhrases()
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        val normalizedName = newName.trim()
        if (normalizedName.isEmpty()) return
        if (_groupList.any { it.id != groupId && it.name.equals(normalizedName, ignoreCase = true) }) return

        val index = _groupList.indexOfFirst { it.id == groupId }
        if (index != -1) {
            _groupList[index] = _groupList[index].copy(name = normalizedName)
            persistCurrentGroups()
        }
    }

    fun moveGroup(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in _groupList.indices || toIndex !in _groupList.indices) return

        val item = _groupList.removeAt(fromIndex)
        _groupList.add(toIndex, item)

        _groupList.forEachIndexed { index, group ->
            _groupList[index] = group.copy(order = index)
        }
        persistCurrentGroups()
    }

    fun updateGroupsOrder(updatedGroups: List<PhraseGroup>) {
        val currentGroupsById = _groupList.associateBy { it.id }
        val orderedGroups = updatedGroups
            .mapNotNull { updated -> currentGroupsById[updated.id]?.copy(order = 0) }
            .distinctBy { it.id }
            .toMutableList()

        for (group in _groupList) {
            if (orderedGroups.none { it.id == group.id }) {
                orderedGroups.add(group)
            }
        }

        orderedGroups.forEachIndexed { index, group ->
            orderedGroups[index] = group.copy(order = index)
        }

        replaceGroups(ensureDefaultGroup(orderedGroups))
        persistCurrentGroups()
    }

    /**
     * 按 id 查找短语，供编辑页初始化表单使用
     */
    fun findPhraseById(id: String?): Phrase? {
        return _phraseList.firstOrNull { it.id == id }
    }

    fun showPhrase(phrase: Phrase) {
        displayText = phrase.speech
        isShowingInitialHint = false
    }

    fun clearDisplayText() {
        displayText = ""
        isShowingInitialHint = false
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

    /**
     * 恢复默认短语
     */
    fun resetToDefault() {
        viewModelScope.launch {
            isPhrasesLoading = true

            try {
                repository.resetPhrases()
                replaceGroups(ensureDefaultGroup(repository.getGroups()))
                replacePhrases(repository.getPhrases())
            } finally {
                isPhrasesLoading = false
            }
        }
    }

    fun exportAll(): String {
        return PhraseShare.export(_groupList.toList(), _phraseList.toList())
    }

    fun importData(content: String): Boolean {
        val data = PhraseShare.import(content) ?: return false

        val existingGroupIds = _groupList.map { it.id }.toMutableSet()
        val existingPhraseIds = _phraseList.map { it.id }.toMutableSet()
        val existingGroupNames = _groupList.map { it.name }.toMutableSet()

        for (group in data.groups) {
            if (group.id in existingGroupIds) continue
            var name = group.name
            var suffix = 1
            while (name in existingGroupNames) {
                name = "${group.name} ($suffix)"
                suffix++
            }
            _groupList.add(group.copy(name = name))
            existingGroupIds.add(group.id)
            existingGroupNames.add(name)
        }

        for (phrase in data.phrases) {
            if (phrase.id in existingPhraseIds) continue
            val targetGroupId = if (phrase.groupId in existingGroupIds) phrase.groupId else PhraseGroup.DEFAULT_ID
            _phraseList.add(phrase.copy(groupId = targetGroupId))
            existingPhraseIds.add(phrase.id)
        }

        persistCurrentGroups()
        persistCurrentPhrases()
        return true
    }

    /**
     * 将当前内存中的列表同步到持久化存储
     */
    private fun persistCurrentPhrases() {
        val snapshot = _phraseList.toList()
        viewModelScope.launch {
            repository.savePhrases(snapshot)
        }
    }

    private fun persistCurrentGroups() {
        val snapshot = _groupList.toList()
        viewModelScope.launch {
            repository.saveGroups(snapshot)
        }
    }

    private fun replacePhrases(phrases: List<Phrase>) {
        _phraseList.clear()
        _phraseList.addAll(phrases)
    }

    private fun replaceGroups(groups: List<PhraseGroup>) {
        _groupList.clear()
        _groupList.addAll(groups)
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
        _phraseList.clear()
        _phraseList.addAll(updatedPhrases)
        persistCurrentPhrases()
    }
}
/**
 * ViewModel 工厂，负责把仓库实现注入到 ViewModel。
 */
class AACViewModelFactory(
    private val repository: PhraseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AACViewModel::class.java)) {
            return AACViewModel(repository) as T
        }

        throw IllegalArgumentException("未知的 ViewModel 类型: ${modelClass.name}")
    }
}
