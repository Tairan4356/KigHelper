package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 短语管理界面。
 * 当前版本按分组筛选后只在组内排序，避免 header 和短语混排造成拖拽闪烁。
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun EditScreen(
    contentPadding: PaddingValues,
    phrases: List<Phrase>,
    groups: List<PhraseGroup>,
    onDelete: (Phrase) -> Unit,
    onMove: (List<Phrase>) -> Unit,
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onAddGroup: (String) -> Boolean,
    onDeleteGroup: (String) -> Unit,
    onMovePhraseToGroup: (phraseId: String, groupId: String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    val outerStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val outerEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val outerBottomPadding = contentPadding.calculateBottomPadding()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val horizontalPadding = 16.dp
    val itemSpacing = 8.dp

    val phraseSnapshot = phrases.toList()
    val groupSnapshot = groups.toList()
    val sortedGroups = remember(groupSnapshot) {
        ensureDefaultGroup(groupSnapshot)
            .distinctBy { it.id }
            .sortedBy { it.order }
    }
    val knownGroupIds = remember(sortedGroups) { sortedGroups.map { it.id }.toSet() }

    var selectedGroupId by rememberSaveable { mutableStateOf(PhraseGroup.DEFAULT_ID) }
    var showAddGroupDialog by rememberSaveable { mutableStateOf(false) }
    var groupPendingDelete by remember { mutableStateOf<PhraseGroup?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val selectedGroup = sortedGroups.firstOrNull { it.id == selectedGroupId }
        ?: sortedGroups.firstOrNull()
    val currentGroupId = selectedGroup?.id ?: PhraseGroup.DEFAULT_ID
    val visiblePhrases = remember(phraseSnapshot, knownGroupIds, currentGroupId) {
        phraseSnapshot.filter { phrase ->
            phrase.effectiveGroupId(knownGroupIds) == currentGroupId
        }
    }
    val localPhrases = remember {
        mutableStateListOf<Phrase>().also { it.addAll(visiblePhrases) }
    }
    val currentOnMove by rememberUpdatedState(onMove)

    LaunchedEffect(sortedGroups, selectedGroupId) {
        if (sortedGroups.isNotEmpty() && sortedGroups.none { it.id == selectedGroupId }) {
            selectedGroupId = sortedGroups.firstOrNull { it.id == PhraseGroup.DEFAULT_ID }?.id
                ?: sortedGroups.first().id
        }
    }

    LaunchedEffect(visiblePhrases, currentGroupId) {
        if (!isDragging) {
            localPhrases.clear()
            localPhrases.addAll(visiblePhrases)
        }
    }

    fun persistCurrentGroupOrder() {
        currentOnMove(
            buildPhraseListWithGroupOrder(
                allPhrases = phraseSnapshot,
                groups = sortedGroups,
                knownGroupIds = knownGroupIds,
                reorderedGroupId = currentGroupId,
                reorderedPhrases = localPhrases
            )
        )
    }

    fun handleReorder(fromIdx: Int, toIdx: Int) {
        if (fromIdx !in localPhrases.indices || toIdx !in localPhrases.indices || fromIdx == toIdx) {
            return
        }

        val item = localPhrases.removeAt(fromIdx)
        localPhrases.add(toIdx.coerceIn(0, localPhrases.size), item)
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        handleReorder(from.index, to.index)
    }

    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState
    ) { from, to ->
        handleReorder(from.index, to.index)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("管理短语") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddGroupDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "新建分组"
                        )
                    }

                    if (currentGroupId != PhraseGroup.DEFAULT_ID) {
                        IconButton(onClick = { selectedGroup?.let { groupPendingDelete = it } }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除当前分组"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                modifier = Modifier.padding(
                    start = outerStartPadding,
                    end = outerEndPadding,
                    bottom = outerBottomPadding
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建短语"
                )
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            GroupFilterRow(
                groups = sortedGroups,
                selectedGroupId = currentGroupId,
                onGroupSelected = { selectedGroupId = it },
                modifier = Modifier.padding(
                    start = outerStartPadding + horizontalPadding,
                    top = itemSpacing,
                    end = outerEndPadding + horizontalPadding,
                    bottom = itemSpacing
                )
            )

            val listContentPadding = PaddingValues(
                start = outerStartPadding + horizontalPadding,
                top = 0.dp,
                end = outerEndPadding + horizontalPadding,
                bottom = outerBottomPadding + 88.dp
            )

            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = lazyGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding,
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    itemsIndexed(
                        items = localPhrases,
                        key = { _, phrase -> phrase.id }
                    ) { _, phrase ->
                        ReorderableItem(
                            state = reorderableLazyGridState,
                            key = phrase.id,
                            animateItemModifier = Modifier
                        ) { dragging ->
                            val interactionSource = remember { MutableInteractionSource() }

                            PhraseEditItem(
                                phrase = phrase,
                                groups = sortedGroups,
                                isDragging = dragging,
                                interactionSource = interactionSource,
                                onDelete = { onDelete(phrase) },
                                onEdit = { onNavigateToEdit(phrase.id) },
                                onMoveToGroup = { targetGroupId ->
                                    onMovePhraseToGroup(phrase.id, targetGroupId)
                                },
                                dragHandleModifier = Modifier.draggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = { isDragging = true },
                                    onDragStopped = {
                                        isDragging = false
                                        persistCurrentGroupOrder()
                                    }
                                )
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding,
                    verticalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    itemsIndexed(
                        items = localPhrases,
                        key = { _, phrase -> phrase.id }
                    ) { _, phrase ->
                        ReorderableItem(
                            state = reorderableLazyListState,
                            key = phrase.id,
                            animateItemModifier = Modifier
                        ) { dragging ->
                            val interactionSource = remember { MutableInteractionSource() }

                            PhraseEditItem(
                                phrase = phrase,
                                groups = sortedGroups,
                                isDragging = dragging,
                                interactionSource = interactionSource,
                                onDelete = { onDelete(phrase) },
                                onEdit = { onNavigateToEdit(phrase.id) },
                                onMoveToGroup = { targetGroupId ->
                                    onMovePhraseToGroup(phrase.id, targetGroupId)
                                },
                                dragHandleModifier = Modifier.draggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = { isDragging = true },
                                    onDragStopped = {
                                        isDragging = false
                                        persistCurrentGroupOrder()
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddGroupDialog) {
        AddGroupDialog(
            existingGroupNames = sortedGroups.map { it.name },
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { name ->
                if (onAddGroup(name)) {
                    showAddGroupDialog = false
                }
            }
        )
    }

    groupPendingDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupPendingDelete = null },
            icon = {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
            },
            title = { Text("删除分组") },
            text = { Text("删除“${group.name}”后，里面的短语会移动到默认分组。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGroup(group.id)
                        groupPendingDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupPendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun GroupFilterRow(
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

@Composable
private fun AddGroupDialog(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseEditItem(
    phrase: Phrase,
    groups: List<PhraseGroup>,
    isDragging: Boolean,
    interactionSource: MutableInteractionSource,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveToGroup: (String) -> Unit,
    dragHandleModifier: Modifier
) {
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 1.dp,
        label = "phraseCardElevation"
    )
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = cardElevation,
            pressedElevation = cardElevation,
            focusedElevation = cardElevation,
            hoveredElevation = cardElevation,
            draggedElevation = cardElevation
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = dragHandleModifier.size(40.dp),
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "拖拽排序",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = phrase.label,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = phrase.speech,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        enabled = group.id != phrase.groupId,
                        onClick = {
                            onMoveToGroup(group.id)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun ensureDefaultGroup(groups: List<PhraseGroup>): List<PhraseGroup> {
    return if (groups.any { it.id == PhraseGroup.DEFAULT_ID }) {
        groups
    } else {
        listOf(
            PhraseGroup(
                id = PhraseGroup.DEFAULT_ID,
                name = PhraseGroup.DEFAULT_NAME,
                order = 0
            )
        ) + groups
    }
}

private fun Phrase.effectiveGroupId(knownGroupIds: Set<String>): String {
    return groupId.takeIf { it in knownGroupIds } ?: PhraseGroup.DEFAULT_ID
}

private fun buildPhraseListWithGroupOrder(
    allPhrases: List<Phrase>,
    groups: List<PhraseGroup>,
    knownGroupIds: Set<String>,
    reorderedGroupId: String,
    reorderedPhrases: List<Phrase>
): List<Phrase> {
    val reorderedIds = reorderedPhrases.map { it.id }.toSet()
    val result = mutableListOf<Phrase>()

    for (group in groups) {
        if (group.id == reorderedGroupId) {
            result.addAll(reorderedPhrases.map { it.copy(groupId = reorderedGroupId) })
        } else {
            result.addAll(
                allPhrases.filter { phrase ->
                    phrase.id !in reorderedIds &&
                        phrase.effectiveGroupId(knownGroupIds) == group.id
                }
            )
        }
    }

    return result
}
