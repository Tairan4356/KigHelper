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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    // 局部状态：输入框的文字
    var newLabel by remember { mutableStateOf("") }
    var newSpeech by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("管理短语") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }, actions = {
                // 重置按钮放在右上角
                TextButton(onClick = onReset) {
                    Text("重置默认", color = MaterialTheme.colorScheme.error)
                }
            })
        }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- 添加新短语表单 ---
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("新增短语", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("按钮显示的文字 (如: 谢谢)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newSpeech,
                        onValueChange = { newSpeech = it },
                        label = { Text("语音播报的内容 (如: 谢谢你的帮助)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newLabel.isNotBlank() && newSpeech.isNotBlank()) {
                                onAdd(newLabel, newSpeech)
                                newLabel = "" // 清空输入框
                                newSpeech = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = newLabel.isNotBlank() && newSpeech.isNotBlank()
                    ) {
                        Text("添加到列表")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("现有短语列表", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // --- 短语列表 ---
            LazyColumn(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(phrases) { phrase ->
                    PhraseItem(
                        phrase = phrase, onDelete = { onDelete(phrase) })
                }
            }

            // --- 完成按钮 ---
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("保存并返回", style = MaterialTheme.typography.labelLarge)
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
                Text(text = phrase.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "语音: ${phrase.speech}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}