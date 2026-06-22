// 短语管理界面中单个短语的可编辑、可删除、可移动分组卡片。
package com.ziegler.kighelper.ui.screens.edit

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.DriveFileMoveRtl
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup

/**
 * 短语管理界面中单个短语的可编辑、可删除、可移动分组卡片。
 *
 * @param phrase 当前短语数据
 * @param groups 所有分组数据，用于构建移动分组菜单
 * @param isDragging 是否正在被拖拽排序，用于调整卡片样式
 * @param interactionSource 交互源，用于配合拖拽排序调整按压状态样式
 * @param onDelete 删除事件回调
 * @param onEdit 编辑事件回调
 * @param onMoveToGroup 移动分组事件回调，参数为目标分组 id
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhraseManagementItem(
    phrase: Phrase,
    groups: List<PhraseGroup>,
    isDragging: Boolean,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveToGroup: (String) -> Unit
) {
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 1.dp, label = "phraseCardElevation"
    )
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = cardElevation,
            pressedElevation = cardElevation,
            focusedElevation = cardElevation,
            hoveredElevation = cardElevation,
            draggedElevation = cardElevation
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = modifier.size(40.dp), onClick = {}) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "拖拽排序",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = phrase.label, style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = phrase.speech,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMoveRtl,
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            enabled = group.id != phrase.groupId,
                            onClick = {
                                onMoveToGroup(group.id)
                                menuExpanded = false
                            })
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
