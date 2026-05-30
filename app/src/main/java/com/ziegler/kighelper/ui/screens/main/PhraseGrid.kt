// 主界面短语网格渲染；此文件刻意不包含拖拽行为。
package com.ziegler.kighelper.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics

/**
 * 为主 AAC 界面渲染分组短语按钮和分组标题。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhraseGrid(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    state: LazyGridState,
    columns: GridCells,
    onPhraseClick: (Phrase) -> Unit,
    onPhraseLongClick: (Phrase) -> Unit,
    onDisplayShouldExpand: () -> Unit,
    modifier: Modifier = Modifier,
    cardFontSize: TextUnit = 18.sp,
    cardHeight: Dp = 80.dp
) {
    val performButtonHaptic = rememberPhysicalButtonHaptics()

    LazyVerticalGrid(
        columns = columns,
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedSections.forEach { (group, groupPhrases) ->
            if (groupedSections.size > 1) {
                item(
                    key = "header_${group.id}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }
            }

            items(
                items = groupPhrases,
                key = { phrase -> phrase.id }
            ) { phrase ->
                val buttonShape = MaterialTheme.shapes.large

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .clip(buttonShape)
                        .combinedClickable(
                            role = Role.Button,
                            onClick = {
                                performButtonHaptic()
                                onDisplayShouldExpand()
                                onPhraseClick(phrase)
                            },
                            onLongClick = {
                                performButtonHaptic()
                                onPhraseLongClick(phrase)
                            }
                        ),
                    shape = buttonShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = phrase.label,
                            fontSize = cardFontSize,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}