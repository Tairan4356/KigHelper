// 短语管理界面中单个短语的可编辑、可删除、可移动分组卡片。
package com.ziegler.kighelper.ui.screens.edit

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup

/**
 * 短语管理界面中单个短语的可编辑、可删除、可移动分组卡片。
 *
 * @param phrase 当前短语数据
 * @param groups 所有分组数据，用于构建移动分组菜单
 * @param currentGroupId 当前短语所在分组 id，用于过滤移动分组菜单
 * @param isDragging 是否正在被拖拽排序，用于调整卡片样式
 * @param interactionSource 交互源，用于配合拖拽排序调整按压状态样式
 * @param onDelete 删除事件回调
 * @param onEdit 编辑事件回调
 * @param onMoveToGroup 移动分组事件回调，参数为目标分组 id
 * @param dragHandleModifier 拖拽手柄修饰符，由上层重排序组件提供，负责处理拖拽手势和调整样式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhraseManagementItem(
    phrase: Phrase,
    groups: List<PhraseGroup>,
    currentGroupId: String,
    isDragging: Boolean,
    interactionSource: MutableInteractionSource,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveToGroup: (String) -> Unit,
    dragHandleModifier: Modifier
) {
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "phraseCardElevation"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isDragging) 1.015f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "phraseCardScale"
    )

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val targetGroups = remember(groups, currentGroupId) {
        groups.filter { group ->
            group.id != currentGroupId
        }
    }

    Card(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = dragHandleModifier.size(40.dp),
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
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
                    text = phrase.label,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = phrase.speech,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Box {
                IconButton(
                    onClick = {
                        menuExpanded = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多"
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    }
                ) {
                    if (targetGroups.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text("没有可移动的分组")
                            },
                            enabled = false,
                            onClick = {}
                        )
                    } else {
                        targetGroups.forEach { group ->
                            DropdownMenuItem(
                                text = {
                                    Text(moveToGroupMenuText(group))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMoveToGroup(group.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 构建移动分组菜单文本，确保分组名称后缀为“分组”，并添加“移动到”前缀。
 */
private fun moveToGroupMenuText(group: PhraseGroup): String {
    val name = group.name.trim()

    val label = if (name.endsWith("分组")) {
        name
    } else {
        "$name 分组"
    }

    return "移动到 $label"
}