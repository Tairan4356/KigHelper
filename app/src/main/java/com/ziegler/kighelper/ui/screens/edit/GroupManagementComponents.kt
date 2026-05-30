// 短语管理界面中的分组筛选、新建分组和删除分组确认组件。
package com.ziegler.kighelper.ui.screens.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.PhraseGroup

/**
 * 显示所有分组的筛选 Chip，并把选中分组 id 回传给管理页。
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
                label = { Text(group.name) }
            )
        }
    }
}

/**
 * 新建分组弹窗，负责输入校验和同名分组提示。
 */
@Composable
internal fun AddGroupDialog(
    existingGroupNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val normalizedName = name.trim()
    val isDuplicate = existingGroupNames.any { it.equals(normalizedName, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null
            )
        },
        title = { Text("新建分组") },
        text = {
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
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(normalizedName) },
                enabled = normalizedName.isNotBlank() && !isDuplicate
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 删除分组确认弹窗，明确提示组内短语会回到默认分组。
 */
@Composable
internal fun DeleteGroupDialog(
    group: PhraseGroup,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
        }
    )
}
