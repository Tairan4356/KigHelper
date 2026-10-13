// 常用字词管理页：按分组筛选字词，支持增删改、拖拽排序与分组管理。
package com.ziegler.kighelper.ui.screens.input

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.InputWord
import com.ziegler.kighelper.data.InputWordGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 常用字词管理页。
 *
 * @param words 全部字词
 * @param groups 全部分组
 * @param onBack 返回回调
 * @param onAddWord 添加字词，返回是否成功（标点组超限失败）
 * @param onUpdateWord 编辑字词
 * @param onDeleteWord 删除字词
 * @param onReorderWords 持久化组内字词顺序
 * @param onAddGroup 添加分组，返回是否成功
 * @param onRenameGroup 重命名分组
 * @param onDeleteGroup 删除分组（级联删除组内字词）
 * @param onReorderGroups 持久化分组顺序
 * @param canAddWordToGroup 目标分组是否还能添加字词
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputWordsManageScreen(
    words: List<InputWord>,
    groups: List<InputWordGroup>,
    onBack: () -> Unit,
    onAddWord: (text: String, groupId: String) -> Boolean,
    onUpdateWord: (id: String, text: String, groupId: String) -> Unit,
    onDeleteWord: (InputWord) -> Unit,
    onReorderWords: (groupId: String, orderedWords: List<InputWord>) -> Unit,
    onAddGroup: (String) -> Boolean,
    onRenameGroup: (groupId: String, newName: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onReorderGroups: (List<InputWordGroup>) -> Unit,
    canAddWordToGroup: (String) -> Boolean
) {
    var selectedGroupId by rememberSaveable { mutableStateOf(InputWordGroup.DEFAULT_ID) }
    LaunchedEffect(groups) {
        if (groups.isNotEmpty() && groups.none { it.id == selectedGroupId }) {
            selectedGroupId = groups.first().id
        }
    }

    val wordCountByGroup = remember(words) { words.groupingBy { it.groupId }.eachCount() }
    val filteredWords = remember(words, selectedGroupId) {
        words.filter { it.groupId == selectedGroupId }.sortedBy { it.order }
    }

    // 拖拽排序期间使用本地副本，变更即时持久化
    var localWords by remember { mutableStateOf(filteredWords) }
    LaunchedEffect(filteredWords) { localWords = filteredWords }

    var editingWordId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddWord by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteWordId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var showGroupManagement by rememberSaveable { mutableStateOf(false) }

    val isPunctuationGroup = selectedGroupId == InputWordGroup.PUNCTUATION_ID
    val canAddCurrentGroup = !isPunctuationGroup && canAddWordToGroup(selectedGroupId)

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localWords = localWords.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorderWords(selectedGroupId, localWords)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("管理常用字词") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (canAddCurrentGroup) showAddWord = true },
                modifier = Modifier.alpha(if (canAddCurrentGroup) 1f else 0.4f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加字词")
            }
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.size(8.dp))

            InputGroupFilterRow(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onGroupSelected = { selectedGroupId = it },
                wordCountByGroup = wordCountByGroup
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isPunctuationGroup -> "标点组固定在输入页底栏，固定 4 个标点、不可增删，单个标点限 1 个字符"
                        canAddCurrentGroup -> "拖动字词可调整顺序"
                        else -> "该分组字词数量已达上限"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showGroupManagement = true }) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("分组管理")
                }
            }

            if (localWords.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "该分组暂无字词",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "点击右下角按钮添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = localWords, key = { it.id }) { word ->
                        ReorderableItem(state = reorderableState, key = word.id) { dragging ->
                            val interactionSource = remember { MutableInteractionSource() }
                            ManageWordRow(
                                word = word,
                                isDragging = dragging,
                                canDelete = !isPunctuationGroup,
                                modifier = Modifier.draggableHandle(
                                    interactionSource = interactionSource
                                ),
                                onEdit = { editingWordId = word.id },
                                onDelete = { pendingDeleteWordId = word.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加 / 编辑字词
    if (showAddWord || editingWordId != null) {
        val editingWord = editingWordId?.let { id -> words.firstOrNull { it.id == id } }
        AddEditWordDialog(
            groups = groups,
            editingWord = editingWord,
            initialGroupId = selectedGroupId,
            isGroupFull = { groupId -> !canAddWordToGroup(groupId) },
            onDismiss = {
                showAddWord = false
                editingWordId = null
            },
            onConfirm = { text, groupId ->
                if (editingWord == null) {
                    onAddWord(text, groupId)
                } else {
                    onUpdateWord(editingWord.id, text, groupId)
                }
                showAddWord = false
                editingWordId = null
            })
    }

    // 删除字词
    pendingDeleteWordId?.let { id ->
        words.firstOrNull { it.id == id }?.let { word ->
            DeleteWordDialog(word = word, onDismiss = { pendingDeleteWordId = null }, onConfirm = {
                onDeleteWord(word)
                pendingDeleteWordId = null
            })
        } ?: run { pendingDeleteWordId = null }
    }

    // 删除分组（级联删除组内字词）
    pendingDeleteGroupId?.let { id ->
        groups.firstOrNull { it.id == id }?.let { group ->
            DeleteInputGroupDialog(group = group,
                wordCount = wordCountByGroup[group.id] ?: 0,
                onDismiss = { pendingDeleteGroupId = null },
                onConfirm = {
                    onDeleteGroup(group.id)
                    pendingDeleteGroupId = null
                })
        } ?: run { pendingDeleteGroupId = null }
    }

    // 分组管理
    if (showGroupManagement) {
        InputGroupManagementDialog(
            groups = groups,
            wordCountByGroup = wordCountByGroup,
            onDismiss = { showGroupManagement = false },
            onAddGroup = onAddGroup,
            onRenameGroup = onRenameGroup,
            onDeleteGroup = onDeleteGroup,
            onReorderGroups = onReorderGroups
        )
    }
}

/**
 * 管理页中的单行字词：拖拽手柄 + 内容 + 编辑/删除按钮。
 */
@Composable
private fun ManageWordRow(
    word: InputWord,
    isDragging: Boolean,
    canDelete: Boolean,
    modifier: Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 1.dp, label = "manageWordRowElevation"
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = word.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑字词")
            }
            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除字词",
                    tint = if (canDelete) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
