// 短语管理界面的横竖屏可排序短语列表。
package com.ziegler.kighelper.ui.screens.edit

import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 短语管理界面的横竖屏可排序短语列表。
 *
 * @param isLandscape 当前屏幕是否为横屏，用于选择列表布局
 * @param localPhrases 当前分组内的短语列表，已根据用户操作进行本地排序
 * @param sortedGroups 所有分组列表，已根据用户操作进行本地排序
 * @param currentGroupId 当前分组 id，用于构建移动分组菜单
 * @param contentPadding 列表内容内边距，由上层 Scaffold 提供，适配系统窗口和 FAB
 * @param onDelete 删除事件回调，参数为被删除的短语对象
 * @param onNavigateToEdit 编辑事件回调，参数为被编辑的短语 id
 * @param onMovePhraseToGroup 移动分组事件回调，参数为被移动的短语 id 和目标分组 id
 * @param onDragStateChanged 拖拽状态变化回调，参数为当前是否正在拖拽，用于调整全局拖拽状态
 * @param onReorder 排序事件回调，参数为被移动项的原始索引和目标索引，由 ReorderableItem 提供
 * @param onDragStopped 拖拽停止回调，无参数，用于重置全局拖拽状态
 * @param modifier 可选的修饰符，用于调整组件布局
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhraseManagementList(
    isLandscape: Boolean,
    localPhrases: List<Phrase>,
    sortedGroups: List<PhraseGroup>,
    currentGroupId: String,
    contentPadding: PaddingValues,
    onDelete: (Phrase) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onMovePhraseToGroup: (phraseId: String, groupId: String) -> Unit,
    onDragStateChanged: (isDragging: Boolean) -> Unit,
    onReorder: (fromIdx: Int, toIdx: Int) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val itemSpacing = 8.dp

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        onReorder(from.index, to.index)
    }

    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState
    ) { from, to ->
        onReorder(from.index, to.index)
    }

    if (isLandscape) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = lazyGridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            itemsIndexed(
                items = localPhrases,
                key = { _, phrase -> phrase.id }
            ) { _, phrase ->
                ReorderableItem(
                    state = reorderableLazyGridState,
                    key = phrase.id
                ) { dragging ->
                    val interactionSource = remember {
                        MutableInteractionSource()
                    }

                    PhraseManagementItem(
                        phrase = phrase,
                        groups = sortedGroups,
                        currentGroupId = currentGroupId,
                        isDragging = dragging,
                        interactionSource = interactionSource,
                        onDelete = { onDelete(phrase) },
                        onEdit = { onNavigateToEdit(phrase.id) },
                        onMoveToGroup = { targetGroupId ->
                            onMovePhraseToGroup(phrase.id, targetGroupId)
                        },
                        dragHandleModifier = Modifier.draggableHandle(
                            interactionSource = interactionSource,
                            onDragStarted = {
                                onDragStateChanged(true)
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            },
                            onDragStopped = {
                                onDragStateChanged(false)
                                onDragStopped()
                            }
                        )
                    )
                }
            }
        }
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            itemsIndexed(
                items = localPhrases,
                key = { _, phrase -> phrase.id }
            ) { _, phrase ->
                ReorderableItem(
                    state = reorderableLazyListState,
                    key = phrase.id
                ) { dragging ->
                    val interactionSource = remember {
                        MutableInteractionSource()
                    }

                    PhraseManagementItem(
                        phrase = phrase,
                        groups = sortedGroups,
                        currentGroupId = currentGroupId,
                        isDragging = dragging,
                        interactionSource = interactionSource,
                        onDelete = { onDelete(phrase) },
                        onEdit = { onNavigateToEdit(phrase.id) },
                        onMoveToGroup = { targetGroupId ->
                            onMovePhraseToGroup(phrase.id, targetGroupId)
                        },
                        dragHandleModifier = Modifier.draggableHandle(
                            interactionSource = interactionSource,
                            onDragStarted = {
                                onDragStateChanged(true)
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            },
                            onDragStopped = {
                                onDragStateChanged(false)
                                onDragStopped()
                            }
                        )
                    )
                }
            }
        }
    }
}
