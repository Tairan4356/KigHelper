// 短语管理界面编排：分组筛选、短语排序、分组操作和编辑入口。
package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.edit.AddGroupDialog
import com.ziegler.kighelper.ui.screens.edit.DeleteGroupDialog
import com.ziegler.kighelper.ui.screens.edit.GroupFilterRow
import com.ziegler.kighelper.ui.screens.edit.PhraseManagementList
import com.ziegler.kighelper.ui.screens.phrase.buildPhraseListWithGroupOrder
import com.ziegler.kighelper.ui.screens.phrase.effectiveGroupId
import com.ziegler.kighelper.ui.screens.phrase.sortedVisibleGroups

/**
 * 短语管理界面。
 * 当前版本按分组筛选后只在组内排序，避免 header 和短语混排造成拖拽闪烁。
 */
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class
)
@Composable
fun EditScreen(
    contentPadding: PaddingValues,
    phrases: List<Phrase>,
    groups: List<PhraseGroup>,
    onDelete: (Phrase) -> Unit,
    onMove: (List<Phrase>) -> Unit,
    onBack: () -> Unit,
    onNavigateToAdd: (String) -> Unit,
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

    val horizontalPadding = 16.dp
    val itemSpacing = 8.dp

    val phraseSnapshot = phrases.toList()
    val groupSnapshot = groups.toList()

    val sortedGroups = remember(groupSnapshot) {
        sortedVisibleGroups(groupSnapshot)
    }
    val knownGroupIds = remember(sortedGroups) { sortedGroups.map { it.id }.toSet() }

    var selectedGroupId by rememberSaveable { mutableStateOf(PhraseGroup.DEFAULT_ID) }
    var showAddGroupDialog by rememberSaveable { mutableStateOf(false) }
    var groupPendingDelete by remember { mutableStateOf<PhraseGroup?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val selectedGroup =
        sortedGroups.firstOrNull { it.id == selectedGroupId } ?: sortedGroups.firstOrNull()

    val currentGroupId = selectedGroup?.id ?: PhraseGroup.DEFAULT_ID

    val visiblePhrases = remember(phraseSnapshot, knownGroupIds, currentGroupId) {
        phraseSnapshot.filter { phrase ->
            phrase.effectiveGroupId(knownGroupIds) == currentGroupId
        }
    }

    val localPhrases = remember {
        mutableStateListOf<Phrase>().also {
            it.addAll(visiblePhrases)
        }
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

    Scaffold(
        topBar = {
        TopAppBar(
            title = {
            Text("管理短语")
        }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        }, actions = {
            IconButton(onClick = { showAddGroupDialog = true }) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "新建分组"
                )
            }

            if (currentGroupId != PhraseGroup.DEFAULT_ID) {
                IconButton(onClick = { selectedGroup?.let { groupPendingDelete = it } }) {
                    Icon(
                        imageVector = Icons.Default.FolderDelete,
                        contentDescription = "删除当前分组"
                    )
                }
            }
        }
        )
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = {
                onNavigateToAdd(currentGroupId)
            },
            modifier = Modifier.padding(
                start = outerStartPadding, end = outerEndPadding, bottom = outerBottomPadding
            ),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Add, contentDescription = "新建短语"
            )
        }
    }, contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
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

            PhraseManagementList(
                isLandscape = isLandscape,
                phrases = localPhrases,
                groups = sortedGroups,
                contentPadding = listContentPadding,
                itemSpacing = itemSpacing,
                onReorder = ::handleReorder,
                onDelete = onDelete,
                onEdit = { phrase -> onNavigateToEdit(phrase.id) },
                onMoveToGroup = { phrase, targetGroupId ->
                    onMovePhraseToGroup(phrase.id, targetGroupId)
                },
                onDragStarted = { isDragging = true },
                onDragStopped = {
                    isDragging = false
                    persistCurrentGroupOrder()
                })
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
            })
    }

    groupPendingDelete?.let { group ->
        DeleteGroupDialog(group = group, onDismiss = { groupPendingDelete = null }, onConfirm = {
            onDeleteGroup(group.id)
            groupPendingDelete = null
        })
    }
}
