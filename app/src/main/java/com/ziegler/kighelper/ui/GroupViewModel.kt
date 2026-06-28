package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.data.PhraseShare
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 分组管理 ViewModel，负责分组的加载、增删改查和排序
 */
class GroupViewModel(private val repository: PhraseRepository) : ViewModel() {
    private val _groupList = MutableStateFlow<List<PhraseGroup>>(emptyList())
    val groupList: StateFlow<List<PhraseGroup>> = _groupList.asStateFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            val groups = repository.getGroups()
            _groupList.value = ensureDefaultGroup(groups)
        }
    }

    fun addGroup(name: String): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return false
        if (_groupList.value.any { it.name.equals(normalizedName, ignoreCase = true) }) return false

        val maxOrder = _groupList.value.maxOfOrNull { it.order } ?: -1
        _groupList.value += PhraseGroup(name = normalizedName, order = maxOrder + 1)
        persistCurrentGroups()
        return true
    }

    fun addGroupDirectly(group: PhraseGroup) {
        if (_groupList.value.any { it.id == group.id || it.name.equals(group.name, ignoreCase = true) }) return
        _groupList.value = _groupList.value + group
        persistCurrentGroups()
    }

    fun deleteGroup(groupId: String) {
        if (groupId == PhraseGroup.DEFAULT_ID) return

        _groupList.value = _groupList.value.filter { it.id != groupId }
        persistCurrentGroups()
    }

    fun renameGroup(groupId: String, newName: String) {
        val normalizedName = newName.trim()
        if (normalizedName.isEmpty()) return
        if (_groupList.value.any {
                it.id != groupId && it.name.equals(
                    normalizedName, ignoreCase = true
                )
            }) return

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
        val orderedGroups = updatedGroups.mapNotNull { updated -> currentGroupsById[updated.id] }
            .distinctBy { it.id }.toMutableList()

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

    fun importGroups(content: String): Boolean {
        val data = PhraseShare.import(content) ?: return false

        val currentGroups = _groupList.value.toMutableList()
        val existingGroupIds = currentGroups.map { it.id }.toMutableSet()
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

        _groupList.value = currentGroups
        persistCurrentGroups()
        return true
    }

    private fun ensureDefaultGroup(groups: List<PhraseGroup>): List<PhraseGroup> {
        return if (groups.any { it.id == PhraseGroup.DEFAULT_ID }) {
            groups
        } else {
            listOf(
                PhraseGroup(
                    id = PhraseGroup.DEFAULT_ID, name = PhraseGroup.DEFAULT_NAME, order = 0
                )
            ) + groups
        }
    }

    private fun persistCurrentGroups() {
        val snapshot = _groupList.value
        viewModelScope.launch {
            repository.saveGroups(snapshot)
        }
    }
}
