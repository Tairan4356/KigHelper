// 主界面使用的添加和编辑短语弹窗。
package com.ziegler.kighelper.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import com.ziegler.kighelper.data.Phrase

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

    PhraseFormDialog(
        title = "添加短语",
        confirmText = "保存",
        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
        label = label,
        speech = speech,
        canSave = canSave,
        onLabelChange = { label = it },
        onSpeechChange = { speech = it },
        onDismiss = onDismiss,
        onSave = { onSave(label.trim(), speech.trim()) })
}

/**
 * 在主界面短语网格中长按后，用于更新已有短语的弹窗。
 *
 * @param phrase 待编辑的短语，弹窗会预填充其标签和播报内容。
 * @param onDismiss 关闭弹窗的回调。
 * @param onSave 保存更新的回调，提供输入的标签和播报内容
 */
@Composable
internal fun EditPhraseDialog(
    phrase: Phrase, onDismiss: () -> Unit, onSave: (label: String, speech: String) -> Unit
) {
    var label by rememberSaveable(phrase.id) { mutableStateOf(phrase.label) }
    var speech by rememberSaveable(phrase.id) { mutableStateOf(phrase.speech) }
    val canSave = label.isNotBlank() && speech.isNotBlank()

    PhraseFormDialog(
        title = "编辑短语",
        confirmText = "保存",
        icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
        label = label,
        speech = speech,
        canSave = canSave,
        onLabelChange = { label = it },
        onSpeechChange = { speech = it },
        onDismiss = onDismiss,
        onSave = { onSave(label.trim(), speech.trim()) })
}

/**
 * 用于添加和编辑短语的通用表单弹窗，包含标签和播报内容输入。
 *
 * @param title 弹窗标题，如“添加短语”或“编辑短语”。
 * @param confirmText 确认按钮文本，如“保存”。
 * @param icon 弹窗图标，如添加或编辑图标。
 * @param label 当前标签输入值。
 * @param speech 当前播报内容输入值。
 * @param canSave 是否启用保存按钮，通常基于输入有效性。
 * @param onLabelChange 标签输入变化回调。
 * @param onSpeechChange 播报内容输入变化回调。
 * @param onDismiss 关闭弹窗回调。
 * @param onSave 保存回调，通常会传递当前输入的标签和播报内容。
 */
@Composable
private fun PhraseFormDialog(
    title: String,
    confirmText: String,
    icon: @Composable () -> Unit,
    label: String,
    speech: String,
    canSave: Boolean,
    onLabelChange: (String) -> Unit,
    onSpeechChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, icon = icon, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text("按钮标签") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = speech,
                onValueChange = onSpeechChange,
                label = { Text("播报内容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }, confirmButton = {
        TextButton(
            onClick = onSave, enabled = canSave
        ) {
            Text(confirmText)
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}
