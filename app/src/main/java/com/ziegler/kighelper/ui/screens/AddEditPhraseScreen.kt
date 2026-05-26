package com.ziegler.kighelper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup

/**
 * 添加/编辑短语表单。
 * 只接收数据和回调，避免页面直接依赖 ViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPhraseScreen(
    phrase: Phrase?,
    isEditMode: Boolean,
    groups: List<PhraseGroup>,
    onSave: (label: String, speech: String, groupId: String) -> Unit,
    onBack: () -> Unit
) {
    var label by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.label.orEmpty())
    }
    var speech by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.speech.orEmpty())
    }
    var selectedGroupId by rememberSaveable(phrase?.id) {
        mutableStateOf(phrase?.groupId ?: PhraseGroup.DEFAULT_ID)
    }
    var groupMenuExpanded by remember { mutableStateOf(false) }

    val selectedGroupName = groups.firstOrNull { it.id == selectedGroupId }?.name ?: PhraseGroup.DEFAULT_NAME

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
            ExposedDropdownMenuBox(
                expanded = groupMenuExpanded,
                onExpandedChange = { groupMenuExpanded = !groupMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedGroupName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分组") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = groupMenuExpanded,
                    onDismissRequest = { groupMenuExpanded = false }
                ) {
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                selectedGroupId = group.id
                                groupMenuExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
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
                    onSave(label, speech, selectedGroupId)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = label.isNotBlank() && speech.isNotBlank()
            ) {
                Text("保存")
            }
        }
    }
}
