// 主界面中用于导航短语分组段落的可滚动分组标签。
package com.ziegler.kighelper.ui.screens.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup

/**
 * 为每个可见短语分组渲染一个标签，并高亮当前可见段落。
 *
 * @param groupedSections 已分组的短语列表，每个分组包含一个 [PhraseGroup] 和对应的短语列表。
 * @param selectedGroupIndex 当前选中的分组索引。
 * @param onGroupSelected 当用户点击标签时调用的回调函数，传递被选中的分组索引。
 * @param modifier 可选的 [Modifier]，用于调整组件的布局和样式。
 */
@Composable
internal fun GroupTabs(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    selectedGroupIndex: Int,
    onGroupSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (groupedSections.size <= 1) return

    SecondaryScrollableTabRow(
        selectedTabIndex = selectedGroupIndex,
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp,
        divider = {}) {
        groupedSections.forEachIndexed { index, (group, _) ->
            val selected = selectedGroupIndex == index
            Tab(selected = selected, onClick = { onGroupSelected(index) }, text = {
                Text(
                    text = group.name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.titleSmall
                )
            })
        }
    }
}
