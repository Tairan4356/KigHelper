// 短语管理界面中的分组筛选、新建分组和删除分组确认组件。
package com.ziegler.kighelper.ui.screens.edit

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ziegler.kighelper.data.PhraseGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 分组筛选组件，使用 FlowRow 实现横向流式布局，适应不同屏幕宽度。
 *
 * @param groups 可选的分组列表
 * @param selectedGroupId 当前选中的分组 id，用于设置 FilterChip 的选中状态
 * @param onGroupSelected 分组选择事件回调，参数为被选中的分组 id
 * @param modifier 可选的修饰符，用于调整组件布局
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupFilterRow(
    groups: List<PhraseGroup>,
    selectedGroupId: String,
    onGroupSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            FilterChip(
                selected = group.id == selectedGroupId,
                onClick = { onGroupSelected(group.id) },
                label = { Text(group.name) })
        }
    }
}

/**
 * 新建分组对话框，包含输入框和重复名称检查。
 *
 * @param existingGroupNames 已有分组名称列表，用于检查重复
 * @param onDismiss 取消事件回调
 * @param onConfirm 确认事件回调，参数为新分组名称
 */
@Composable
internal fun AddGroupDialog(
    existingGroupNames: List<String>, onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val normalizedName = name.trim()
    val isDuplicate = existingGroupNames.any { it.equals(normalizedName, ignoreCase = true) }

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(
            imageVector = Icons.Default.Folder, contentDescription = null
        )
    }, title = { Text("新建分组") }, text = {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("分组名称") },
            isError = isDuplicate,
            supportingText = {
                if (isDuplicate) {
                    Text("已经有同名分组")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }, confirmButton = {
        TextButton(
            onClick = { onConfirm(normalizedName) },
            enabled = normalizedName.isNotBlank() && !isDuplicate
        ) {
            Text("创建")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}

/**
 * 删除分组确认对话框，提示用户删除分组后短语会移动到默认分组。
 *
 * @param group 要删除的分组对象，用于显示分组名称
 * @param onDismiss 取消事件回调
 * @param onConfirm 确认事件回调，执行删除操作
 */
@Composable
internal fun DeleteGroupDialog(
    group: PhraseGroup, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
        },
        title = { Text("删除分组") },
        text = { Text("删除“${group.name}”后，里面的短语会移动到默认分组。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        })
}

/**
 * 分组管理对话框，集成分组的添加、重命名、删除和拖拽排序。
 *
 * 列表顺序由本地状态维护：增删改通过回调实时同步到 ViewModel，groups 流变化时合并到本地
 * 副本（保留本地拖拽后的顺序），关闭时一次性调用 onReorderGroups 持久化最终顺序。
 *
 * @param groups 当前分组列表（已排序）
 * @param onDismiss 关闭对话框回调
 * @param onAddGroup 添加分组回调，返回是否添加成功（用于清空输入框）
 * @param onRenameGroup 重命名分组回调
 * @param onDeleteGroup 删除分组回调
 * @param onReorderGroups 持久化分组新顺序的回调
 */
@Composable
internal fun GroupManagementDialog(
    groups: List<PhraseGroup>,
    onDismiss: () -> Unit,
    onAddGroup: (String) -> Boolean,
    onRenameGroup: (groupId: String, newName: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onReorderGroups: (List<PhraseGroup>) -> Unit
) {
    var localGroups by remember { mutableStateOf(groups) }
    LaunchedEffect(groups) {
        localGroups = mergeGroupsWithLocalOrder(localGroups, groups)
    }

    var newGroupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var editingName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<PhraseGroup?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        localGroups = localGroups.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "分组管理", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.size(12.dp))

                AddGroupRow(
                    groups = localGroups,
                    name = newGroupName,
                    onNameChange = { newGroupName = it },
                    onAdd = {
                        if (onAddGroup(newGroupName.trim())) newGroupName = ""
                    }
                )

                Spacer(Modifier.size(12.dp))

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = localGroups, key = { it.id }) { group ->
                        ReorderableItem(state = reorderableState, key = group.id) { dragging ->
                            val interactionSource = remember { MutableInteractionSource() }
                            GroupManagementRow(
                                group = group,
                                isDragging = dragging,
                                isEditing = editingGroupId == group.id,
                                editingName = editingName,
                                allGroups = localGroups,
                                dragHandleModifier = Modifier.draggableHandle(
                                    interactionSource = interactionSource
                                ),
                                onStartEdit = {
                                    editingGroupId = group.id
                                    editingName = group.name
                                },
                                onEditingNameChange = { editingName = it },
                                onConfirmRename = {
                                    val trimmed = editingName.trim()
                                    if (trimmed.isNotEmpty() && trimmed != group.name) {
                                        onRenameGroup(group.id, trimmed)
                                    }
                                    editingGroupId = null
                                },
                                onCancelRename = { editingGroupId = null },
                                onDelete = { pendingDelete = group }
                            )
                        }
                    }
                }

                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onReorderGroups(localGroups)
                        onDismiss()
                    }) { Text("完成") }
                }
            }
        }
    }

    pendingDelete?.let { group ->
        DeleteGroupDialog(
            group = group,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteGroup(group.id)
                localGroups = localGroups.filter { it.id != group.id }
                pendingDelete = null
            }
        )
    }
}

/**
 * 分组管理对话框顶部的添加分组输入行：输入框 + 「添加分组」按钮，含重复名校验。
 */
@Composable
private fun AddGroupRow(
    groups: List<PhraseGroup>,
    name: String,
    onNameChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    val normalized = name.trim()
    val isDuplicate = isGroupNameDuplicate(groups, normalized)
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("新分组名称") },
            singleLine = true,
            isError = isDuplicate,
            supportingText = { if (isDuplicate) Text("已经有同名分组") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onAdd, enabled = normalized.isNotBlank() && !isDuplicate
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("添加分组")
            }
        }
    }
}

/**
 * 分组管理对话框中的单行外壳：拖拽手柄 + Card 容器，根据 [isEditing] 切换显示态与编辑态。
 */
@Composable
private fun GroupManagementRow(
    group: PhraseGroup,
    isDragging: Boolean,
    isEditing: Boolean,
    editingName: String,
    allGroups: List<PhraseGroup>,
    dragHandleModifier: Modifier,
    onStartEdit: () -> Unit,
    onEditingNameChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit,
    onDelete: () -> Unit
) {
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 1.dp, label = "groupRowElevation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "拖拽排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp).then(dragHandleModifier)
            )
            Spacer(Modifier.width(8.dp))

            if (isEditing) {
                GroupManagementRowEditor(
                    group = group,
                    editingName = editingName,
                    isDuplicate = isGroupNameDuplicate(
                        allGroups, editingName, excludeId = group.id
                    ),
                    onEditingNameChange = onEditingNameChange,
                    onConfirmRename = onConfirmRename,
                    onCancelRename = onCancelRename
                )
            } else {
                GroupManagementRowDisplay(
                    group = group, onStartEdit = onStartEdit, onDelete = onDelete
                )
            }
        }
    }
}

/**
 * 行只读态：分组名称 + 重命名按钮 + 删除按钮（默认分组禁用删除）。
 */
@Composable
private fun RowScope.GroupManagementRowDisplay(
    group: PhraseGroup, onStartEdit: () -> Unit, onDelete: () -> Unit
) {
    val isDefault = group.id == PhraseGroup.DEFAULT_ID
    Text(
        text = group.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
    )
    IconButton(onClick = onStartEdit) {
        Icon(Icons.Default.Edit, contentDescription = "重命名分组")
    }
    IconButton(onClick = onDelete, enabled = !isDefault) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "删除分组",
            tint = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )
    }
}

/**
 * 行编辑态：输入框 + 确认/取消按钮，重复名或与原名相同时禁用确认。
 */
@Composable
private fun RowScope.GroupManagementRowEditor(
    group: PhraseGroup,
    editingName: String,
    isDuplicate: Boolean,
    onEditingNameChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit
) {
    val normalized = editingName.trim()
    val canConfirm = normalized.isNotEmpty() &&
            normalized != group.name && !isDuplicate
    OutlinedTextField(
        value = editingName,
        onValueChange = onEditingNameChange,
        label = { Text("分组名称") },
        singleLine = true,
        isError = isDuplicate,
        supportingText = { if (isDuplicate) Text("已经有同名分组") },
        modifier = Modifier.weight(1f)
    )
    IconButton(onClick = onConfirmRename, enabled = canConfirm) {
        Icon(Icons.Default.Check, contentDescription = "确认重命名")
    }
    IconButton(onClick = onCancelRename) {
        Icon(Icons.Default.Close, contentDescription = "取消重命名")
    }
}

/**
 * 判断候选名称是否与现有分组重复（忽略大小写、trim 后比较）。空字符串视为不重复。
 *
 * @param excludeId 需要排除的分组 id，用于重命名时排除自身。
 */
private fun isGroupNameDuplicate(
    groups: List<PhraseGroup>, candidate: String, excludeId: String? = null
): Boolean {
    val normalized = candidate.trim()
    if (normalized.isEmpty()) return false
    return groups.any { it.id != excludeId && it.name.equals(normalized, ignoreCase = true) }
}

/**
 * 将远程分组列表合并到本地顺序上：保留本地拖拽后的顺序与 [PhraseGroup.order]，
 * 同步远程的改名/删除，并在末尾追加本地不存在的新增分组。
 */
private fun mergeGroupsWithLocalOrder(
    local: List<PhraseGroup>, remote: List<PhraseGroup>
): List<PhraseGroup> {
    val remoteById = remote.associateBy { it.id }
    val merged = local.mapNotNull { localGroup ->
        remoteById[localGroup.id]?.copy(order = localGroup.order)
    }
    val localIds = local.map { it.id }.toSet()
    return merged + remote.filter { it.id !in localIds }
}
