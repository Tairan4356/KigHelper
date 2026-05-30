// 短语管理界面编排：分组筛选、短语排序、分组操作和编辑入口。
package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.edit.AddGroupDialog
import com.ziegler.kighelper.ui.screens.edit.GroupFilterRow
import com.ziegler.kighelper.ui.screens.edit.PhraseManagementList
import com.ziegler.kighelper.ui.screens.phrase.buildPhraseListWithGroupOrder
import com.ziegler.kighelper.ui.screens.phrase.effectiveGroupId
import com.ziegler.kighelper.ui.screens.phrase.ensureDefaultGroup

/**
 * 短语管理界面。
 * 当前版本按分组筛选后只在组内排序，避免 header 和短语混排造成拖拽闪烁。
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val view = LocalView.current

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

    val knownGroupIds = remember(sortedGroups) {
        sortedGroups.map { it.id }.toSet()
    }

    var selectedGroupId by rememberSaveable {
        mutableStateOf(PhraseGroup.DEFAULT_ID)
    }

    var showAddGroupDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var groupPendingDelete by remember {
        mutableStateOf<PhraseGroup?>(null)
    }

    var isDragging by remember {
        mutableStateOf(false)
    }

    val selectedGroup = sortedGroups.firstOrNull { it.id == selectedGroupId }
        ?: sortedGroups.firstOrNull()

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

        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                        IconButton(
                            onClick = {
                                selectedGroup?.let {
                                    groupPendingDelete = it
                                }
                            }
                        ) {
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
                onClick = { onNavigateToAdd(currentGroupId) },
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

            PhraseManagementList(
                isLandscape = isLandscape,
                localPhrases = localPhrases,
                sortedGroups = sortedGroups,
                currentGroupId = currentGroupId,
                contentPadding = PaddingValues(
                    start = outerStartPadding + horizontalPadding,
                    top = 0.dp,
                    end = outerEndPadding + horizontalPadding,
                    bottom = outerBottomPadding + 88.dp
                ),
                onDelete = onDelete,
                onNavigateToEdit = onNavigateToEdit,
                onMovePhraseToGroup = { phraseId, targetGroupId ->
                    localPhrases.removeAll { it.id == phraseId }
                    onMovePhraseToGroup(phraseId, targetGroupId)
                },
                onDragStateChanged = { dragging -> isDragging = dragging },
                onReorder = ::handleReorder,
                onDragStopped = ::persistCurrentGroupOrder
            )
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
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
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
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error
                    )
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
