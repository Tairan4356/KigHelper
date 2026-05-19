package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 短语管理界面。
 * 提供添加、删除、编辑、拖拽排序短语的功能。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditScreen(
    contentPadding: PaddingValues,
    phrases: List<Phrase>,
    onDelete: (Phrase) -> Unit,
    onMove: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit
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

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        currentOnMove(from.index, to.index)
    }

    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState
    ) { from, to ->
        currentOnMove(from.index, to.index)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                modifier = Modifier.padding(
                    start = outerStartPadding,
                    end = outerEndPadding,
                    bottom = outerBottomPadding
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加"
                )
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
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
                    items = phrases,
                    key = { _, phrase -> phrase.id }
                ) { _, phrase ->
                    ReorderableItem(
                        state = reorderableLazyGridState,
                        key = phrase.id
                    ) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }

                        PhraseEditItem(
                            phrase = phrase,
                            isDragging = isDragging,
                            interactionSource = interactionSource,
                            onDelete = { onDelete(phrase) },
                            onEdit = { onNavigateToEdit(phrase.id) },
                            dragHandleModifier = Modifier.draggableHandle(
                                interactionSource = interactionSource
                            )
                        )
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
                itemsIndexed(
                    items = phrases,
                    key = { _, phrase -> phrase.id }
                ) { _, phrase ->
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = phrase.id
                    ) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }

                        PhraseEditItem(
                            phrase = phrase,
                            isDragging = isDragging,
                            interactionSource = interactionSource,
                            onDelete = { onDelete(phrase) },
                            onEdit = { onNavigateToEdit(phrase.id) },
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
        targetValue = if (isDragging) 10.dp else 1.dp,
        label = "phraseCardElevation"
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
        }
    }
}