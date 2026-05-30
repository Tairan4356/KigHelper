// 短语与分组共用的纯逻辑，供主界面和短语管理界面复用。
package com.ziegler.kighelper.ui.screens.phrase

import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup

/**
 * 确保分组列表中包含默认分组，如果没有则添加一个默认分组。
 *
 * @param groups 原始分组列表
 * @return 包含默认分组的分组列表
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
 * 获取短语的有效分组 id，如果当前分组 id 不在已知分组列表中，则返回默认分组 id。
 *
 * @param knownGroupIds 已知的分组 id 列表，用于验证当前分组 id 是否有效
 * @return 当前短语的有效分组 id，可能是原始分组 id 或默认分组 id
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
 * 构建短语列表，按照分组顺序排列，并将指定分组的短语替换为重新排序后的列表。
 *
 * @param allPhrases 所有短语列表，用于过滤和构建最终列表
 * @param groups 所有分组列表，用于确定分组顺序
 * @param knownGroupIds 已知的分组 id 列表，用于验证短语的有效分组
 * @param reorderedGroupId 被重新排序的分组 id，用于替换该分组的短语
 * @param reorderedPhrases 重新排序后的短语列表，用于替换指定分组的短语
 * @return 按照分组顺序排列的最终短语列表，包含重新排序后的指定分组短语
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
            result.addAll(
                reorderedPhrases.map {
                    it.copy(groupId = reorderedGroupId)
                }
            )
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
