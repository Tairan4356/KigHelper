package com.ziegler.kighelper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ziegler.kighelper.data.Phrase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    phrases: List<Phrase>,
    onAdd: (String, String) -> Unit,
    onDelete: (Phrase) -> Unit,
    onReset: () -> Unit
) {
    var newLabel by remember { mutableStateOf("") }
    var newSpeech by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("管理短语", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        // 新增短语卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("按钮显示 (如: 谢谢)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newSpeech,
                    onValueChange = { newSpeech = it },
                    label = { Text("朗读内容 (如: 谢谢你)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newLabel.isNotBlank() && newSpeech.isNotBlank()) {
                            onAdd(newLabel, newSpeech)
                            newLabel = ""; newSpeech = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("添加")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("列表", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onReset) {
                Text("重置默认", color = MaterialTheme.colorScheme.error)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(phrases) { phrase ->
                PhraseItem(phrase = phrase, onDelete = { onDelete(phrase) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PhraseItem(phrase: Phrase, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phrase.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "语音: ${phrase.speech}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}