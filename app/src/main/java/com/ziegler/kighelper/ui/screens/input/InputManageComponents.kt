// 常用字词管理页组件：字词增删改对话框、分组筛选与分组管理对话框。
package com.ziegler.kighelper.ui.screens.input

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.ziegler.kighelper.data.InputWord
import com.ziegler.kighelper.data.InputWordGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 分组筛选组件，横向流式排布，标点组同样可筛选以便编辑底栏字词。
 *
 * @param groups 分组列表
 * @param selectedGroupId 当前选中的分组 id
 * @param onGroupSelected 分组选择回调
 * @param wordCountByGroup 各分组的字词数量，用于展示计数
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InputGroupFilterRow(
    modifier: Modifier = Modifier,
    groups: List<InputWordGroup>,
    selectedGroupId: String,
    onGroupSelected: (String) -> Unit,
    wordCountByGroup: Map<String, Int> = emptyMap()
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            val count = wordCountByGroup[group.id] ?: 0
            FilterChip(
                selected = group.id == selectedGroupId,
                onClick = { onGroupSelected(group.id) },
                label = {
                    Text(text = group.name)
                    if (count > 0) {
                        Text(
                            text = " $count",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                })
        }
    }
}

/**
 * 新增/编辑字词对话框：输入字词内容并选择所属分组。
 *
 * @param groups 可选分组列表
 * @param editingWord 编辑时的原字词，新增时为 null
 * @param initialGroupId 新增时的默认分组 id
 * @param isGroupFull 目标分组已满（标点组 4 条上限）时禁用确认
 * @param onDismiss 取消回调
 * @param onConfirm 确认回调，参数为字词内容与目标分组 id
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditWordDialog(
    groups: List<InputWordGroup>,
    editingWord: InputWord? = null,
    initialGroupId: String,
    isGroupFull: (String) -> Boolean = { false },
    onDismiss: () -> Unit,
    onConfirm: (text: String, groupId: String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(editingWord?.text ?: "") }
    var groupId by rememberSaveable { mutableStateOf(editingWord?.groupId ?: initialGroupId) }
    var menuExpanded by remember { mutableStateOf(false) }
    val normalized = text.trim()
    val selectedGroup = groups.firstOrNull { it.id == groupId }
    // 标点组：固定 4 条、不可增删，编辑时锁定分组且内容限制为 1 个字符
    val isEditingPunctuation = editingWord?.groupId == InputWordGroup.PUNCTUATION_ID
    val isPunctuationTarget = groupId == InputWordGroup.PUNCTUATION_ID
    val cannotConfirm = normalized.isEmpty() ||
            (editingWord?.groupId != groupId && isGroupFull(groupId))

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(
            imageVector = Icons.Default.Edit, contentDescription = null
        )
    }, title = {
        Text(if (editingWord == null) "添加字词" else "编辑字词")
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = if (groupId == InputWordGroup.PUNCTUATION_ID) input.take(1) else input
                },
                label = { Text("字词内容") },
                singleLine = true,
                isError = normalized.isEmpty() && text.isNotEmpty(),
                supportingText = {
                    if (isPunctuationTarget) Text("标点最多输入 1 个字符")
                },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = menuExpanded, onExpandedChange = { menuExpanded = it }) {
                OutlinedTextField(
                    value = selectedGroup?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isEditingPunctuation,
                    label = { Text("所属分组") },
                    trailingIcon = {
                        if (!isEditingPunctuation) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    groups.forEach { group ->
                        val isPunctuationOption = group.id == InputWordGroup.PUNCTUATION_ID
                        // 标点组不可作为目标：编辑标点时锁定为该组，其余情况禁用该选项
                        val optionEnabled = if (isEditingPunctuation) isPunctuationOption
                        else !isPunctuationOption
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            enabled = optionEnabled,
                            onClick = {
                                groupId = group.id
                                menuExpanded = false
                            })
                    }
                }
            }
            if (editingWord?.groupId != groupId && isGroupFull(groupId)) {
                Text(
                    text = "该分组字词数量已达上限",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = { onConfirm(normalized, groupId) },
            enabled = !cannotConfirm
        ) {
            Text("保存")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}

/**
 * 删除字词确认对话框。
 */
@Composable
internal fun DeleteWordDialog(word: InputWord, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
        },
        title = { Text("删除字词") },
        text = { Text("确定删除“${word.text}”吗？") },
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
 * 删除分组确认对话框，明确提示会级联删除组内字词。
 */
@Composable
internal fun DeleteInputGroupDialog(
    group: InputWordGroup, wordCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
        },
        title = { Text("删除分组") },
        text = {
            Text(
                if (wordCount > 0) "删除“${group.name}”后，其中 $wordCount 个字词会一并删除。"
                else "确定删除分组“${group.name}”吗？"
            )
        },
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
 * 标点组固定显示在输入页底栏，禁止删除。
 *
 * 列表顺序由本地状态维护：增删改通过回调实时同步，groups 流变化时合并到本地副本，
 * 关闭时一次性调用 onReorderGroups 持久化最终顺序。
 */
@Composable
internal fun InputGroupManagementDialog(
    groups: List<InputWordGroup>,
    wordCountByGroup: Map<String, Int>,
    onDismiss: () -> Unit,
    onAddGroup: (String) -> Boolean,
    onRenameGroup: (groupId: String, newName: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onReorderGroups: (List<InputWordGroup>) -> Unit
) {
    var localGroups by remember { mutableStateOf(groups) }
    LaunchedEffect(groups) {
        localGroups = mergeInputGroupsWithLocalOrder(localGroups, groups)
    }

    var newGroupName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var editingName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<InputWordGroup?>(null) }

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

                AddInputGroupRow(
                    groups = localGroups,
                    name = newGroupName,
                    onNameChange = { newGroupName = it },
                    onAdd = {
                        if (onAddGroup(newGroupName.trim())) newGroupName = ""
                    })

                Spacer(Modifier.size(12.dp))

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = localGroups, key = { it.id }) { group ->
                        ReorderableItem(state = reorderableState, key = group.id) { dragging ->
                            val interactionSource = remember { MutableInteractionSource() }
                            InputGroupManagementRow(
                                group = group,
                                isDragging = dragging,
                                isEditing = editingGroupId == group.id,
                                editingName = editingName,
                                allGroups = localGroups,
                                canDelete = group.id != InputWordGroup.PUNCTUATION_ID,
                                modifier = Modifier.draggableHandle(
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
                                onDelete = { pendingDelete = group })
                        }
                    }
                }

                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
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
        DeleteInputGroupDialog(
            group = group,
            wordCount = wordCountByGroup[group.id] ?: 0,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteGroup(group.id)
                localGroups = localGroups.filter { it.id != group.id }
                pendingDelete = null
            })
    }
}

/**
 * 分组管理对话框顶部的添加分组输入行，含重复名校验。
 */
@Composable
private fun AddInputGroupRow(
    groups: List<InputWordGroup>, name: String, onNameChange: (String) -> Unit, onAdd: () -> Unit
) {
    val normalized = name.trim()
    val isDuplicate = isInputGroupNameDuplicate(groups, normalized)
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
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
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
private fun InputGroupManagementRow(
    group: InputWordGroup,
    isDragging: Boolean,
    isEditing: Boolean,
    editingName: String,
    allGroups: List<InputWordGroup>,
    canDelete: Boolean,
    modifier: Modifier,
    onStartEdit: () -> Unit,
    onEditingNameChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit,
    onDelete: () -> Unit
) {
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 1.dp, label = "inputGroupRowElevation"
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
                modifier = Modifier
                    .size(24.dp)
                    .then(modifier)
            )
            Spacer(Modifier.width(8.dp))

            if (isEditing) {
                InputGroupRowEditor(
                    group = group,
                    editingName = editingName,
                    isDuplicate = isInputGroupNameDuplicate(
                        allGroups, editingName, excludeId = group.id
                    ),
                    onEditingNameChange = onEditingNameChange,
                    onConfirmRename = onConfirmRename,
                    onCancelRename = onCancelRename
                )
            } else {
                InputGroupRowDisplay(
                    group = group,
                    canDelete = canDelete,
                    onStartEdit = onStartEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

/**
 * 行只读态：分组名称 + 重命名按钮 + 删除按钮（标点组禁用删除）。
 */
@Composable
private fun RowScope.InputGroupRowDisplay(
    group: InputWordGroup, canDelete: Boolean, onStartEdit: () -> Unit, onDelete: () -> Unit
) {
    Text(
        text = group.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
    )
    if (group.id == InputWordGroup.PUNCTUATION_ID) {
        Text(
            text = "底栏",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
    IconButton(onClick = onStartEdit) {
        Icon(Icons.Default.Edit, contentDescription = "重命名分组")
    }
    IconButton(onClick = onDelete, enabled = canDelete) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "删除分组",
            tint = if (canDelete) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 行编辑态：输入框 + 确认/取消按钮，重复名或与原名相同时禁用确认。
 */
@Composable
private fun RowScope.InputGroupRowEditor(
    group: InputWordGroup,
    editingName: String,
    isDuplicate: Boolean,
    onEditingNameChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit
) {
    val normalized = editingName.trim()
    val canConfirm = normalized.isNotEmpty() && normalized != group.name && !isDuplicate
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
 */
private fun isInputGroupNameDuplicate(
    groups: List<InputWordGroup>, candidate: String, excludeId: String? = null
): Boolean {
    val normalized = candidate.trim()
    if (normalized.isEmpty()) return false
    return groups.any { it.id != excludeId && it.name.equals(normalized, ignoreCase = true) }
}

/**
 * 将远程分组列表合并到本地顺序上：保留本地拖拽后的顺序与 order，
 * 同步远程的改名/删除，并在末尾追加本地不存在的新增分组。
 */
private fun mergeInputGroupsWithLocalOrder(
    local: List<InputWordGroup>, remote: List<InputWordGroup>
): List<InputWordGroup> {
    val remoteById = remote.associateBy { it.id }
    val merged = local.mapNotNull { localGroup ->
        remoteById[localGroup.id]?.copy(order = localGroup.order)
    }
    val localIds = local.map { it.id }.toSet()
    return merged + remote.filter { it.id !in localIds }
}
