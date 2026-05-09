package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase

/**
 * 短语管理界面
 * 提供添加、删除、排序短语的功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditScreen(
    contentPadding: PaddingValues,
    phrases: List<Phrase>,
    onDelete: (Phrase) -> Unit,
    onMove: (Int, Int) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToAbout: () -> Unit
) {
    // 根据屏幕方向切换列表密度
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current
    val outerStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val outerEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val outerBottomPadding = contentPadding.calculateBottomPadding()
    val listContentPadding = PaddingValues(
        start = outerStartPadding,
        end = outerEndPadding,
        bottom = outerBottomPadding + 88.dp
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理短语") },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.Info, "关于")
                    }
                }
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
                Icon(Icons.Default.Add, "添加")
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->
        if (isLandscape) {
            // 横屏使用 2 列网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentPadding = listContentPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(phrases, key = { _, p -> p.id }) { index, phrase ->
                    Box(modifier = Modifier.animateItem()) {
                        PhraseEditItem(
                            phrase = phrase,
                            onDelete = { onDelete(phrase) },
                            onEdit = { onNavigateToEdit(phrase.id) },
                            onMoveUp = if (index > 0) {
                                { onMove(index, index - 1) }
                            } else null,
                            onMoveDown = if (index < phrases.size - 1) {
                                { onMove(index, index + 1) }
                            } else null)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentPadding = listContentPadding
            ) {
                itemsIndexed(phrases, key = { _, p -> p.id }) { index, phrase ->
                    Box(modifier = Modifier.animateItem()) {
                        PhraseEditItem(
                            phrase = phrase,
                            onDelete = { onDelete(phrase) },
                            onEdit = { onNavigateToEdit(phrase.id) },
                            onMoveUp = if (index > 0) {
                                { onMove(index, index - 1) }
                            } else null,
                            onMoveDown = if (index < phrases.size - 1) {
                                { onMove(index, index + 1) }
                            } else null)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PhraseEditItem(
    phrase: Phrase,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            // 排序手柄图标区
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp ?: {},
                    enabled = onMoveUp != null,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                }
                IconButton(
                    onClick = onMoveDown ?: {},
                    enabled = onMoveDown != null,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(phrase.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    phrase.speech,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
