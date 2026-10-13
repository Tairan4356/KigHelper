package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.InputWord
import com.ziegler.kighelper.data.InputWordGroup
import com.ziegler.kighelper.data.InputWordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 常用字词管理 ViewModel，负责字词与分组的加载、增删改查和排序。
 * 所有变更即时写入仓库，仓库 Flow 作为唯一数据源回灌界面。
 */
@HiltViewModel
class InputWordViewModel @Inject constructor(
    private val repository: InputWordRepository
) : ViewModel() {

    private val _words = MutableStateFlow<List<InputWord>>(emptyList())
    val words: StateFlow<List<InputWord>> = _words.asStateFlow()

    private val _groups = MutableStateFlow<List<InputWordGroup>>(emptyList())
    val groups: StateFlow<List<InputWordGroup>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeGroups().collect { groups ->
                _groups.value = groups.sortedBy { it.order }
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            repository.observeWords().collect { _words.value = it }
        }
    }

    /**
     * 组内字词列表（按 order 升序）
     */
    fun wordsOfGroup(groupId: String): List<InputWord> =
        _words.value.filter { it.groupId == groupId }.sortedBy { it.order }

    /**
     * 标点组已达到上限时不允许继续添加
     */
    fun canAddWordToGroup(groupId: String): Boolean =
        groupId != InputWordGroup.PUNCTUATION_ID ||
                wordsOfGroup(groupId).size < InputWordGroup.PUNCTUATION_MAX

    /**
     * 添加字词，返回是否添加成功（空内容或标点组超限时失败）
     */
    fun addWord(text: String, groupId: String): Boolean {
        val normalized = text.trim()
        if (normalized.isEmpty() || !canAddWordToGroup(groupId)) return false

        val groupWords = wordsOfGroup(groupId)
        val newWord = InputWord(
            text = normalized, groupId = groupId, order = (groupWords.maxOfOrNull { it.order } ?: -1) + 1
        )
        persistWords(_words.value + newWord)
        return true
    }

    /**
     * 编辑字词内容与所属分组
     */
    fun updateWord(id: String, newText: String, newGroupId: String? = null) {
        val normalized = newText.trim()
        if (normalized.isEmpty()) return
        val targetGroup = newGroupId ?: _words.value.firstOrNull { it.id == id }?.groupId ?: return

        // 移动到其他分组时补充目标分组末尾的排序值
        val targetMaxOrder = wordsOfGroup(targetGroup).maxOfOrNull { it.order } ?: -1
        _words.value = _words.value.map { word ->
            if (word.id == id) {
                val moved = newGroupId != null && word.groupId != newGroupId
                word.copy(
                    text = normalized,
                    groupId = targetGroup,
                    order = if (moved) targetMaxOrder + 1 else word.order
                )
            } else {
                word
            }
        }
        persistWords(_words.value)
    }

    fun deleteWord(word: InputWord) {
        persistWords(_words.value.filterNot { it.id == word.id })
    }

    /**
     * 持久化组内字词的新顺序（传入该组展示顺序的完整列表）
     */
    fun updateWordsOrder(groupId: String, orderedWords: List<InputWord>) {
        val reordered = orderedWords.mapIndexed { index, word -> word.copy(order = index) }
        val rest = _words.value.filter { it.groupId != groupId }
        persistWords(rest + reordered)
    }

    /**
     * 添加分组，重名时返回 false
     */
    fun addGroup(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        val exists = _groups.value.any { it.name.equals(normalized, ignoreCase = true) }
        if (exists) return false

        val nextOrder = (_groups.value.maxOfOrNull { it.order } ?: -1) + 1
        persistGroups(_groups.value + InputWordGroup(name = normalized, order = nextOrder))
        return true
    }

    fun renameGroup(groupId: String, newName: String) {
        val normalized = newName.trim()
        if (normalized.isEmpty()) return
        persistGroups(
            _groups.value.map { group ->
                if (group.id == groupId) group.copy(name = normalized) else group
            }
        )
    }

    /**
     * 删除分组并级联删除组内字词。标点组固定在输入页底栏，禁止删除。
     */
    fun deleteGroup(groupId: String) {
        if (groupId == InputWordGroup.PUNCTUATION_ID) return
        persistGroups(_groups.value.filterNot { it.id == groupId })
        persistWords(_words.value.filterNot { it.groupId == groupId })
    }

    /**
     * 持久化分组新顺序
     */
    fun updateGroupsOrder(updatedGroups: List<InputWordGroup>) {
        persistGroups(updatedGroups.mapIndexed { index, group -> group.copy(order = index) })
    }

    /**
     * 恢复预置字词与分组
     */
    fun resetToDefaults() {
        viewModelScope.launch { repository.reset() }
    }

    private fun persistWords(words: List<InputWord>) {
        _words.value = words
        viewModelScope.launch { repository.saveWords(words) }
    }

    private fun persistGroups(groups: List<InputWordGroup>) {
        _groups.value = groups
        viewModelScope.launch { repository.saveGroups(groups) }
    }
}
