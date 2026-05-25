package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics

/**
 * 主界面：提供大字显示区域和短语快捷按钮网格。
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    phrases: List<Phrase>,
    groups: List<PhraseGroup> = emptyList(),
    displayText: String,
    isShowingInitialHint: Boolean,
    isPhrasesLoading: Boolean,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onAddPhrase: (label: String, speech: String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val phraseGridState = rememberLazyGridState()
    var isDisplayCompressed by remember { mutableStateOf(false) }
    var showAddPhraseDialog by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val layoutDirection = LocalLayoutDirection.current
    val pagePadding = 16.dp

    val outerStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val outerTopPadding = contentPadding.calculateTopPadding()
    val outerEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val outerBottomPadding = contentPadding.calculateBottomPadding()

    val screenModifier = modifier
        .fillMaxSize()
        .padding(
            start = outerStartPadding + pagePadding,
            top = outerTopPadding + pagePadding,
            end = outerEndPadding + pagePadding,
            bottom = outerBottomPadding
        )

    val hasPhrases = phrases.isNotEmpty()
    val showPhraseGrid = !isPhrasesLoading && hasPhrases
    val showEmptyState = !isPhrasesLoading && !hasPhrases

    val effectiveDisplayText = when {
        isPhrasesLoading && isShowingInitialHint -> ""
        !hasPhrases && isShowingInitialHint -> "先添加一个常用短语吧"
        else -> displayText
    }

    val effectiveIsSubtle = isShowingInitialHint

    val isPhraseGridAtTop by remember {
        derivedStateOf {
            phraseGridState.firstVisibleItemIndex == 0 && phraseGridState.firstVisibleItemScrollOffset == 0
        }
    }

    val displayWeight by animateFloatAsState(
        targetValue = if (!isLandscape && isDisplayCompressed) 0.38f else 0.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
        ),
        label = "displayAreaWeight"
    )

    val phraseAreaWeight = 1f - displayWeight

    val phraseGridNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset, source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f) {
                    isDisplayCompressed = true
                }

                return Offset.Zero
            }
        }
    }
    // TODO: 第一次加载时分组信息可能还没准备好，导致分组显示异常。
    val groupedSections = remember(phrases.toList(), groups.toList()) {
        val defaultGroupId = PhraseGroup.DEFAULT_ID
        val defaultGroup = groups.firstOrNull { it.id == defaultGroupId } ?: PhraseGroup(
            defaultGroupId, PhraseGroup.DEFAULT_NAME
        )

        val distinctGroups = groups.distinctBy { it.id }.filter { it.id != defaultGroupId }

        val groupById = distinctGroups.associateBy { it.id }

        val grouped = phrases.groupBy { phrase ->
            groupById[phrase.groupId] ?: defaultGroup
        }

        val activeGroups = mutableListOf<PhraseGroup>()

        for (group in distinctGroups) {
            val groupPhrases = grouped[group]
            if (!groupPhrases.isNullOrEmpty()) {
                activeGroups.add(group)
            }
        }

        val defaultPhrases = grouped[defaultGroup]
        if (!defaultPhrases.isNullOrEmpty()) {
            activeGroups.add(defaultGroup)
        }

        val result = mutableListOf<Pair<PhraseGroup, List<Phrase>>>()
        for (group in activeGroups.sortedBy { it.order }) {
            val groupPhrases = grouped[group] ?: emptyList()
            result.add(group to groupPhrases.distinctBy { it.id })
        }

        result
    }

    // 每个分组 header 在 Grid 中的全局索引
    val groupHeaderPositions = remember(groupedSections) {
        var offset = 0
        val map = mutableMapOf<String, Int>()
        for ((group, groupPhrases) in groupedSections) {
            map[group.id] = offset
            offset += 1 + groupPhrases.size
        }
        map
    }

    // 当前可见分组索引（根据 firstVisibleItemIndex 推导）
    val currentGroupIndex by remember(groupedSections) {
        derivedStateOf {
            val visibleIndex = phraseGridState.firstVisibleItemIndex
            var offset = 0
            for ((i, entry) in groupedSections.withIndex()) {
                val groupPhrases = entry.second
                val groupEnd = offset + groupPhrases.size
                if (visibleIndex in offset..groupEnd) return@derivedStateOf i
                offset = groupEnd + 1
            }
            groupedSections.indices.firstOrNull() ?: 0
        }
    }

    // 点击 tab 时触发的滚动目标
    var scrollToGroupIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(scrollToGroupIndex) {
        val targetIndex = scrollToGroupIndex ?: return@LaunchedEffect
        val targetGroup = groupedSections.getOrNull(targetIndex) ?: return@LaunchedEffect
        val headerPos = groupHeaderPositions[targetGroup.first.id] ?: return@LaunchedEffect
        phraseGridState.animateScrollToItem(headerPos)
        scrollToGroupIndex = null
    }

    LaunchedEffect(effectiveDisplayText) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(isPhraseGridAtTop, hasPhrases, isPhrasesLoading) {
        if (isPhrasesLoading || !hasPhrases || isPhraseGridAtTop) {
            isDisplayCompressed = false
        }
    }

    if (showAddPhraseDialog) {
        AddPhraseDialog(onDismiss = {
            showAddPhraseDialog = false
        }, onSave = { label, speech ->
            onAddPhrase(label, speech)
            showAddPhraseDialog = false
        })
    }

    if (isLandscape) {
        Row(
            modifier = screenModifier, horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                DisplaySurface(
                    text = effectiveDisplayText,
                    isSubtle = effectiveIsSubtle,
                    scrollState = scrollState,
                    onClear = onClearClick
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    showPhraseGrid -> {
                        Column {
                            if (groupedSections.size > 1) {
                                ScrollableTabRow(
                                    selectedTabIndex = currentGroupIndex,
                                    modifier = Modifier.padding(horizontal = -pagePadding),
                                    edgePadding = pagePadding
                                ) {
                                    groupedSections.forEachIndexed { index, (group, _) ->
                                        Tab(
                                            selected = currentGroupIndex == index,
                                            onClick = { scrollToGroupIndex = index },
                                            text = { Text(group.name) })
                                    }
                                }
                            }

                            PhraseGrid(
                                modifier = Modifier.fillMaxSize(),
                                groupedSections = groupedSections,
                                state = phraseGridState,
                                columns = GridCells.Fixed(2),
                                onPhraseClick = onPhraseClick,
                                onDisplayShouldExpand = {
                                    isDisplayCompressed = false
                                })
                        }
                    }

                    showEmptyState -> {
                        EmptyPhraseState(
                            modifier = Modifier.fillMaxSize(), onAddClick = {
                                showAddPhraseDialog = true
                            })
                    }

                    else -> {
                        LoadingPhraseState(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = screenModifier
        ) {
            Box(modifier = Modifier.weight(displayWeight)) {
                DisplaySurface(
                    text = effectiveDisplayText,
                    isSubtle = effectiveIsSubtle,
                    scrollState = scrollState,
                    onClear = onClearClick
                )
            }

            Spacer(Modifier.height(16.dp))

            when {
                showPhraseGrid -> {
                    Column(modifier = Modifier.weight(phraseAreaWeight)) {
                        if (groupedSections.size > 1) {
                            ScrollableTabRow(
                                selectedTabIndex = currentGroupIndex, edgePadding = 0.dp
                            ) {
                                groupedSections.forEachIndexed { index, (group, _) ->
                                    Tab(
                                        selected = currentGroupIndex == index,
                                        onClick = { scrollToGroupIndex = index },
                                        text = { Text(group.name) })
                                }
                            }
                        }

                        PhraseGrid(
                            modifier = Modifier
                                .weight(1f)
                                .nestedScroll(phraseGridNestedScrollConnection),
                            groupedSections = groupedSections,
                            state = phraseGridState,
                            columns = GridCells.Fixed(2),
                            onPhraseClick = onPhraseClick,
                            onDisplayShouldExpand = {
                                isDisplayCompressed = false
                            })
                    }
                }

                showEmptyState -> {
                    EmptyPhraseState(
                        modifier = Modifier
                            .weight(phraseAreaWeight)
                            .fillMaxWidth(), onAddClick = {
                            showAddPhraseDialog = true
                        })
                }

                else -> {
                    LoadingPhraseState(
                        modifier = Modifier
                            .weight(phraseAreaWeight)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplaySurface(
    text: String,
    isSubtle: Boolean,
    scrollState: ScrollState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = text, transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                }, label = "textAnimation"
            ) { targetText ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (targetText.length > 20) 36.sp else 48.sp,
                            lineHeight = if (targetText.length > 20) 40.sp else 52.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary.copy(
                            alpha = if (isSubtle) 0.55f else 1f
                        )
                    )
                }
            }

            if (text.isNotEmpty()) {
                IconButton(
                    onClick = onClear, modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingPhraseState(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier)
}

@Composable
private fun EmptyPhraseState(
    onAddClick: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier
                    .padding(18.dp)
                    .size(36.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "还没有短语",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "添加常用短语后，它们会显示在这里，点击即可显示并播报。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )

            Spacer(Modifier.size(ButtonDefaults.IconSpacing))

            Text("添加短语")
        }
    }
}

@Composable
private fun AddPhraseDialog(
    onDismiss: () -> Unit, onSave: (label: String, speech: String) -> Unit
) {
    var label by rememberSaveable { mutableStateOf("") }
    var speech by rememberSaveable { mutableStateOf("") }

    val canSave = label.isNotBlank() && speech.isNotBlank()

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(
            imageVector = Icons.Default.Add, contentDescription = null
        )
    }, title = {
        Text("添加短语")
    }, text = {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = label, onValueChange = {
                label = it
            }, label = {
                Text("按钮标签")
            }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = speech, onValueChange = {
                speech = it
            }, label = {
                Text("播报内容")
            }, modifier = Modifier.fillMaxWidth(), minLines = 3
            )
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                onSave(label.trim(), speech.trim())
            }, enabled = canSave
        ) {
            Text("保存")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("取消")
        }
    })
}

@Composable
private fun PhraseGrid(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    state: LazyGridState,
    columns: GridCells,
    onPhraseClick: (Phrase) -> Unit,
    onDisplayShouldExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val performButtonHaptic = rememberPhysicalButtonHaptics()

    LazyVerticalGrid(
        columns = columns,
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedSections.forEach { (group, groupPhrases) ->
            item(
                key = "header_${group.id}", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 4.dp, top = 12.dp, bottom = 4.dp
                    )
                )
            }

            items(
                items = groupPhrases, key = { phrase -> phrase.id }) { phrase ->
                Button(
                    onClick = {
                        performButtonHaptic()
                        onDisplayShouldExpand()
                        onPhraseClick(phrase)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = phrase.label,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}