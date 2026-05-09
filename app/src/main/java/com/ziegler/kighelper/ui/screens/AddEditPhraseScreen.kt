package com.ziegler.kighelper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase

/**
 * 添加/编辑短语表单。
 * 只接收数据和回调，避免页面直接依赖 ViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPhraseScreen(
    phrase: Phrase?,
    isEditMode: Boolean,
    onSave: (label: String, speech: String) -> Unit,
    onBack: () -> Unit
) {
    var label by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.label.orEmpty())
    }
    var speech by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.speech.orEmpty())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑短语" else "添加短语") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("按钮标签") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = speech,
                onValueChange = { speech = it },
                label = { Text("播报内容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Button(
                onClick = {
                    onSave(label, speech)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = label.isNotBlank() && speech.isNotBlank()
            ) {
                Text("保存")
            }
        }
    }
}
