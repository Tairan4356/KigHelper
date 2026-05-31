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
 */
@Composable
internal fun GroupTabs(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    selectedGroupIndex: Int,
    onGroupSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
