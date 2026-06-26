// 主界面使用的添加短语弹窗。
package com.ziegler.kighelper.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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

/**
 * 在不离开 AAC 面板的情况下，从主界面创建短语的弹窗。
 *
 * @param onDismiss 关闭弹窗的回调。
 * @param onSave 保存短语的回调，提供输入的标签和播报内容。
 */
@Composable
internal fun AddPhraseDialog(
    onDismiss: () -> Unit, onSave: (label: String, speech: String) -> Unit
) {
    var label by rememberSaveable { mutableStateOf("") }
    var speech by rememberSaveable { mutableStateOf("") }
    val canSave = label.isNotBlank() && speech.isNotBlank()

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
    }, title = { Text("添加短语") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("按钮标签") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = speech,
                onValueChange = { speech = it },
                label = { Text("播报内容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }, confirmButton = {
        TextButton(
            onClick = { onSave(label.trim(), speech.trim()) }, enabled = canSave
        ) {
            Text("保存")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}
