// 主界面短语网格及同步分组标签使用的纯分组辅助逻辑。
package com.ziegler.kighelper.ui.screens.main

import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.phrase.ensureDefaultGroup
import com.ziegler.kighelper.ui.screens.phrase.effectiveGroupId

/**
 * 按可见短语分组整理短语，同时保留分组顺序和默认分组兜底。
 *
 * @param phrases 待分组的短语列表。
 * @param groups 可用的分组列表，可能不包含默认分组。
 * @return 按分组顺序排序的分组与其对应短语列表，仅包含有短语的分组。
 */
internal fun buildGroupedSections(
    phrases: List<Phrase>, groups: List<PhraseGroup>
): List<Pair<PhraseGroup, List<Phrase>>> {
    val defaultGroupId = PhraseGroup.DEFAULT_ID
    val groupsWithDefault = ensureDefaultGroup(groups)
    val defaultGroup = groupsWithDefault.first { it.id == defaultGroupId }
    val distinctGroups = groupsWithDefault.distinctBy { it.id }.filter { it.id != defaultGroupId }
    val groupById = distinctGroups.associateBy { it.id }
    val knownGroupIds = groupsWithDefault.map { it.id }.toSet()
    val grouped = phrases.groupBy { phrase ->
        groupById[phrase.effectiveGroupId(knownGroupIds)] ?: defaultGroup
    }

    // 主界面只显示含有短语的分组，避免出现空标签。
    val activeGroups = mutableListOf<PhraseGroup>()
    for (group in distinctGroups) {
        if (!grouped[group].isNullOrEmpty()) {
            activeGroups.add(group)
        }
    }

    val defaultPhrases = grouped[defaultGroup]
    if (!defaultPhrases.isNullOrEmpty()) {
        activeGroups.add(defaultGroup)
    }

    return activeGroups.sortedBy { it.order }.map { group ->
        group to grouped[group].orEmpty().distinctBy { phrase -> phrase.id }
    }
}

/**
 * 将分组段落转换为扁平 LazyGrid 项索引，供分组标签滚动定位使用。
 *
 * @param groupedSections 已按分组整理的短语列表。
 * @return 分组索引到 LazyGrid 项索引的映射，分组索引对应于 [groupedSections] 的位置。
 */
internal fun buildGroupStartIndexMap(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>
): Map<Int, Int> {
    val map = mutableMapOf<Int, Int>()
    var currentIndex = 0
    groupedSections.forEachIndexed { index, (_, phrases) ->
        map[index] = currentIndex
        val headerCount = if (groupedSections.size > 1) 1 else 0
        currentIndex += headerCount + phrases.size
    }
    return map
}
