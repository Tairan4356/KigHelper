package com.ziegler.kighelper.ui.screens.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.PhraseGroup

@Composable
fun PhraseExportDialog(
    groups: List<PhraseGroup>,
    onDismiss: () -> Unit,
    onConfirm: (selectedGroupIds: Set<String>, includeAudio: Boolean, fileName: String) -> Unit
) {
    var includeAudio by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("phrases") }
    val selectedGroupIds = remember { mutableStateOf(groups.map { it.id }.toMutableSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出短语") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名") },
                    singleLine = true,
                    suffix = { Text(".kigphrase") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("包含音频文件", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includeAudio,
                        onCheckedChange = { includeAudio = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "导出短语关联的音频文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("选择分组", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "取消勾选以排除对应分组",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                groups.forEach { group ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val ids = selectedGroupIds.value
                                if (group.id in ids) ids.remove(group.id) else ids.add(group.id)
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = group.id in selectedGroupIds.value,
                            onCheckedChange = { checked ->
                                val ids = selectedGroupIds.value
                                if (checked) ids.add(group.id) else ids.remove(group.id)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(group.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        selectedGroupIds.value.toSet(),
                        includeAudio,
                        fileName.ifBlank { "phrases" }
                    )
                }
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
