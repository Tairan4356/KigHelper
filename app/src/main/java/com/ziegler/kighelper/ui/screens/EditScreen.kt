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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.ziegler.kighelper.ui.components.PhraseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    phrases: List<Phrase>,
    onAdd: (String, String) -> Unit,
    onDelete: (Phrase) -> Unit,
    onMove: (Int, Int) -> Unit,
    onReset: () -> Unit
) {
    var newLabel by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp,0.dp)
    ) {
        Text("管理短语", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("按钮显示") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("完整内容") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newLabel.isNotBlank() && newText.isNotBlank()) {
                            onAdd(newLabel, newText)
                            newLabel = ""; newText = ""
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
            itemsIndexed(phrases, key = { _, phrase -> phrase.id }) { index, phrase ->
                PhraseItem(
                    phrase = phrase,
                    isFirst = index == 0,
                    isLast = index == phrases.size - 1,
                    onDelete = { onDelete(phrase) },
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}