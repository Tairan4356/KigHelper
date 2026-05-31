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
 * 短语管理界面的横竖屏可排序短语列表。
 *
 * @param isLandscape 当前屏幕是否为横屏，用于选择列表布局
 * @param contentPadding 列表内容内边距，由上层 Scaffold 提供，适配系统窗口和 FAB
 * @param onDelete 删除事件回调，参数为被删除的短语对象
 * @param onReorder 排序事件回调，参数为被移动项的原始索引和目标索引，由 ReorderableItem 提供
 * @param onDragStopped 拖拽停止回调，无参数，用于重置全局拖拽状态
 * @param modifier 可选的修饰符，用于调整组件布局
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
                items = phrases, key = { _, phrase -> phrase.id }) { _, phrase ->
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
                        })
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
                items = phrases, key = { _, phrase -> phrase.id }) { _, phrase ->
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
                        })
                }
            }
        }
    }
}
