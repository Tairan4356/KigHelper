// 主界面短语网格及同步分组标签使用的纯分组辅助逻辑。
package com.ziegler.kighelper.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.phrase.effectiveGroupId
import com.ziegler.kighelper.ui.screens.phrase.sortedVisibleGroups

/**
 * 按可见短语分组整理短语，同时保留分组顺序和默认分组兜底。
 */
@Composable
internal fun rememberGroupedSections(
    phrases: List<Phrase>,
    groups: List<PhraseGroup>
): List<Pair<PhraseGroup, List<Phrase>>> {
    return remember(phrases, groups) {
        derivedStateOf {
            val sortedGroups = sortedVisibleGroups(groups)
            val knownGroupIds = sortedGroups.map { it.id }.toSet()

            // 主界面只显示含有短语的分组，避免出现空标签。
            val groupedMap = phrases.groupBy { phrase ->
                phrase.effectiveGroupId(knownGroupIds)
            }

            sortedGroups.mapNotNull { group ->
                val groupPhrases = groupedMap[group.id]
                if (!groupPhrases.isNullOrEmpty()) {
                    group to groupPhrases.distinctBy { it.id }
                } else {
                    null
                }
            }
        }
    }.value
}
