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
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
 * 定义扁平化的视觉展示实体
 */
private sealed interface VisualItem {
    data class Header(val group: PhraseGroup) : VisualItem
    data class PhraseItem(val phrase: Phrase) : VisualItem
}

/**
 * 短语管理界面。
 * 提供添加、删除、编辑、拖拽排序短语的功能。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    onAddGroup: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    val outerStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val outerEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val outerBottomPadding = contentPadding.calculateBottomPadding()


    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listHorizontalPadding = 16.dp
    val itemSpacing = 8.dp

    val listContentPadding = PaddingValues(
        start = outerStartPadding + listHorizontalPadding,
        top = itemSpacing,
        end = outerEndPadding + listHorizontalPadding,
        bottom = outerBottomPadding + 88.dp
    )

    val currentOnMove by rememberUpdatedState(onMove)

    // 保留所有分组（即使是空的），并按顺序生成扁平的视觉实体列表
    val visualList = remember(phrases, groups) {
        val list = mutableListOf<VisualItem>()
        val groupedMap = phrases.groupBy { it.groupId }
        groups.sortedBy { it.order }.forEach { group ->
            list.add(VisualItem.Header(group))
            val groupPhrases = groupedMap[group.id] ?: emptyList()
            for (phrase in groupPhrases) {
                list.add(VisualItem.PhraseItem(phrase))
            }
        }
        list
    }

    // 抽离统一的重新排序逻辑：处理元素交换，并动态重算短语属于哪一个 Header 对应的 groupId
    val handleReorder = remember(visualList) {
        { fromIdx: Int, toIdx: Int ->
            if (fromIdx in visualList.indices && toIdx in visualList.indices) {
                // 仅允许拖拽短语，Header 无法被拖拽
                val draggedItem = visualList[fromIdx] as? VisualItem.PhraseItem
                if (draggedItem != null) {
                    val mutableVisualList = visualList.toMutableList()
                    val item = mutableVisualList.removeAt(fromIdx)
                    mutableVisualList.add(toIdx, item)

                    // 重新扫描整个列表，动态为每一个短语分配在其上方最近的 Header 分组 ID
                    var currentGroupId = PhraseGroup.DEFAULT_ID
                    val updatedPhrases = mutableListOf<Phrase>()

                    for (element in mutableVisualList) {
                        when (element) {
                            is VisualItem.Header -> {
                                currentGroupId = element.group.id
                            }

                            is VisualItem.PhraseItem -> {
                                updatedPhrases.add(element.phrase.copy(groupId = currentGroupId))
                            }
                        }
                    }
                    currentOnMove(updatedPhrases)
                }
            }
        }
    }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showAddGroupDialog by rememberSaveable { mutableStateOf(false) }

    val fabRotation by animateFloatAsState(
        targetValue = if (fabMenuExpanded) 45f else 0f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ), label = "FAB Rotation"
    )

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
                scrollBehavior = scrollBehavior
            )
        }, floatingActionButton = {
            Box(
                modifier = Modifier.padding(
                    start = outerStartPadding, end = outerEndPadding, bottom = outerBottomPadding
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = fabMenuExpanded,
                        enter = fadeIn(animationSpec = tween(durationMillis = 200)) + slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 150)) + slideOutVertically(
                            targetOffsetY = { it / 3 }, animationSpec = tween(durationMillis = 150)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 新建分组
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    tonalElevation = 1.dp
                                ) {
                                    Text(
                                        text = "新建分组",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp, vertical = 6.dp
                                        )
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        showAddGroupDialog = true
                                        fabMenuExpanded = false
                                    },
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "新建分组",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // 新建短语
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    tonalElevation = 1.dp
                                ) {
                                    Text(
                                        text = "新建短语",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp, vertical = 6.dp
                                        )
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = {
                                        onNavigateToAdd()
                                        fabMenuExpanded = false
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = "新建短语",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 主 FAB
                    FloatingActionButton(
                        onClick = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (fabMenuExpanded) "关闭" else "添加",
                            modifier = Modifier.graphicsLayer {
                                rotationZ = fabRotation
                            }
                        )
                    }
                }
            }
        },  contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { innerPadding ->
        if (isLandscape) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = lazyGridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = listContentPadding,
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                itemsIndexed(
                    items = visualList,
                    key = { _, item ->
                        when (item) {
                            is VisualItem.Header -> "header_${item.group.id}"
                            is VisualItem.PhraseItem -> item.phrase.id
                        }
                    },
                    span = { _, item ->
                        if (item is VisualItem.Header) {
                            GridItemSpan(maxLineSpan)
                        } else {
                            GridItemSpan(1)
                        }
                    }
                ) { _, item ->
                    when (item) {
                        is VisualItem.Header -> {
                            GroupHeader(name = item.group.name)
                        }
                        is VisualItem.PhraseItem -> {
                            ReorderableItem(
                                state = reorderableLazyGridState,
                                key = item.phrase.id
                            ) { isDragging ->
                                val interactionSource = remember { MutableInteractionSource() }

                                PhraseEditItem(
                                    phrase = item.phrase,
                                    isDragging = isDragging,
                                    interactionSource = interactionSource,
                                    onDelete = { onDelete(item.phrase) },
                                    onEdit = { onNavigateToEdit(item.phrase.id) },
                                    dragHandleModifier = Modifier.draggableHandle(
                                        interactionSource = interactionSource
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = listContentPadding,
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                // 直接使用平铺列表进行高效渲染
                itemsIndexed(
                    items = visualList,
                    key = { _, item ->
                        when (item) {
                            is VisualItem.Header -> "header_${item.group.id}"
                            is VisualItem.PhraseItem -> item.phrase.id
                        }
                    }
                ) { _, item ->
                    when (item) {
                        is VisualItem.Header -> {
                            GroupHeader(name = item.group.name)
                        }
                        is VisualItem.PhraseItem -> {
                            ReorderableItem(
                                state = reorderableLazyListState,
                                key = item.phrase.id
                            ) { isDragging ->
                                val interactionSource = remember { MutableInteractionSource() }

                                PhraseEditItem(
                                    phrase = item.phrase,
                                    isDragging = isDragging,
                                    interactionSource = interactionSource,
                                    onDelete = { onDelete(item.phrase) },
                                    onEdit = { onNavigateToEdit(item.phrase.id) },
                                    dragHandleModifier = Modifier.draggableHandle(
                                        interactionSource = interactionSource
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGroupDialog) {
        AddGroupDialog(onDismiss = { showAddGroupDialog = false }, onConfirm = { name ->
            onAddGroup(name)
            showAddGroupDialog = false
        })
    }
}

@Composable
private fun AddGroupDialog(
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(
            imageVector = Icons.Default.Folder, contentDescription = null
        )
    }, title = { Text("新建分组") }, text = {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("分组名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }, confirmButton = {
        TextButton(
            onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()
        ) {
            Text("创建")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseEditItem(
    phrase: Phrase,
    isDragging: Boolean,
    interactionSource: MutableInteractionSource,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    dragHandleModifier: Modifier
) {
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 1.dp, label = "phraseCardElevation"
    )

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
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = dragHandleModifier.size(40.dp), onClick = {}) {
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
                    text = phrase.label, style = MaterialTheme.typography.titleMedium
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
        }
    }
}

@Composable
private fun GroupHeader(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}