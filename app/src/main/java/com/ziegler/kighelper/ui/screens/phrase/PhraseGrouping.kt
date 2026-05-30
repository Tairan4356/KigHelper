// 短语与分组共用的纯逻辑，供主界面和短语管理界面复用。
package com.ziegler.kighelper.ui.screens.phrase

import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup

/**
 * 确保分组列表始终包含默认分组。
 */
internal fun ensureDefaultGroup(groups: List<PhraseGroup>): List<PhraseGroup> {
    return if (groups.any { it.id == PhraseGroup.DEFAULT_ID }) {
        groups
    } else {
        listOf(
            PhraseGroup(
                id = PhraseGroup.DEFAULT_ID,
                name = PhraseGroup.DEFAULT_NAME,
                order = 0
            )
        ) + groups
    }
}

/**
 * 将短语的分组 id 修正为当前已知分组；未知分组统一回落到默认分组。
 */
internal fun Phrase.effectiveGroupId(knownGroupIds: Set<String>): String {
    return groupId.takeIf { it in knownGroupIds } ?: PhraseGroup.DEFAULT_ID
}

/**
 * 生成用于界面展示的稳定分组列表：补齐默认分组、去重并按 order 排序。
 */
internal fun sortedVisibleGroups(groups: List<PhraseGroup>): List<PhraseGroup> {
    return ensureDefaultGroup(groups)
        .distinctBy { it.id }
        .sortedBy { it.order }
}

/**
 * 将当前分组内拖拽后的顺序合并回完整短语列表，保留其他分组的原有顺序。
 */
internal fun buildPhraseListWithGroupOrder(
    allPhrases: List<Phrase>,
    groups: List<PhraseGroup>,
    knownGroupIds: Set<String>,
    reorderedGroupId: String,
    reorderedPhrases: List<Phrase>
): List<Phrase> {
    val reorderedIds = reorderedPhrases.map { it.id }.toSet()
    val result = mutableListOf<Phrase>()

    for (group in groups) {
        if (group.id == reorderedGroupId) {
            result.addAll(reorderedPhrases.map { it.copy(groupId = reorderedGroupId) })
        } else {
            result.addAll(
                allPhrases.filter { phrase ->
                    phrase.id !in reorderedIds &&
                        phrase.effectiveGroupId(knownGroupIds) == group.id
                }
            )
        }
    }

    return result
}
