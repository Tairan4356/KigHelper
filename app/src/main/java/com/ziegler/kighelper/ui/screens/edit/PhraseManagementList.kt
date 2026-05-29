// 短语管理界面的横竖屏可排序短语列表。
package com.ziegler.kighelper.ui.screens.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 根据横竖屏选择网格或列表布局，并把拖拽排序事件转发给上层状态。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhraseManagementList(
    isLandscape: Boolean,
    phrases: List<Phrase>,
    groups: List<PhraseGroup>,
    contentPadding: PaddingValues,
    itemSpacing: Dp,
    onReorder: (fromIdx: Int, toIdx: Int) -> Unit,
    onDelete: (Phrase) -> Unit,
    onEdit: (Phrase) -> Unit,
    onMoveToGroup: (phrase: Phrase, targetGroupId: String) -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLandscape) {
        val lazyGridState = rememberLazyGridState()
        val reorderableLazyGridState = rememberReorderableLazyGridState(
            lazyGridState = lazyGridState
        ) { from, to ->
            onReorder(from.index, to.index)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = lazyGridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            itemsIndexed(
                items = phrases,
                key = { _, phrase -> phrase.id }
            ) { _, phrase ->
                ReorderableItem(
                    state = reorderableLazyGridState,
                    key = phrase.id,
                    animateItemModifier = Modifier
                ) { dragging ->
                    val interactionSource = remember { MutableInteractionSource() }

                    PhraseManagementItem(
                        phrase = phrase,
                        groups = groups,
                        isDragging = dragging,
                        modifier = Modifier.draggableHandle(
                            interactionSource = interactionSource,
                            onDragStarted = { onDragStarted() },
                            onDragStopped = onDragStopped
                        ),
                        interactionSource = interactionSource,
                        onDelete = { onDelete(phrase) },
                        onEdit = { onEdit(phrase) },
                        onMoveToGroup = { targetGroupId ->
                            onMoveToGroup(phrase, targetGroupId)
                        }
                    )
                }
            }
        }
    } else {
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(
            lazyListState = lazyListState
        ) { from, to ->
            onReorder(from.index, to.index)
        }

        LazyColumn(
            state = lazyListState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            itemsIndexed(
                items = phrases,
                key = { _, phrase -> phrase.id }
            ) { _, phrase ->
                ReorderableItem(
                    state = reorderableLazyListState,
                    key = phrase.id,
                    animateItemModifier = Modifier
                ) { dragging ->
                    val interactionSource = remember { MutableInteractionSource() }

                    PhraseManagementItem(
                        phrase = phrase,
                        groups = groups,
                        isDragging = dragging,
                        modifier = Modifier.draggableHandle(
                            interactionSource = interactionSource,
                            onDragStarted = { onDragStarted() },
                            onDragStopped = onDragStopped
                        ),
                        interactionSource = interactionSource,
                        onDelete = { onDelete(phrase) },
                        onEdit = { onEdit(phrase) },
                        onMoveToGroup = { targetGroupId ->
                            onMoveToGroup(phrase, targetGroupId)
                        }
                    )
                }
            }
        }
    }
}
