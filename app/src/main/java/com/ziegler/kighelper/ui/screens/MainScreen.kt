package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics
import kotlinx.coroutines.launch

/**
 * 主界面：提供大字显示区域和短语快捷按钮网格。
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    phrases: List<Phrase>,
    groups: List<PhraseGroup> = emptyList(),
    displayText: String,
    isShowingInitialHint: Boolean,
    isPhrasesLoading: Boolean,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onAddPhrase: (label: String, speech: String) -> Unit,
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val phraseGridState = rememberLazyGridState()
    var isDisplayCompressed by remember { mutableStateOf(false) }
    var showAddPhraseDialog by rememberSaveable { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<Phrase?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp

    // 根据屏幕宽度动态计算卡片的字号、高度和网格列数
    val (cardFontSize, cardHeight) = remember(screenWidth) {
        when {
            screenWidth < 1080 -> 14.sp to 80.dp
            else -> 24.sp to 112.dp
        }
    }

    val gridColumns = remember(screenWidth, isLandscape) {
        when {
            screenWidth < 1080 -> GridCells.Fixed(2)
            else -> GridCells.Fixed(3)
        }
    }
    val screenWidth = configuration.screenWidthDp

    // 根据屏幕宽度动态计算卡片的字号、高度和网格列数
    val (cardFontSize, cardHeight) = remember(screenWidth) {
        when {
            screenWidth < 360 -> 14.sp to 64.dp      // 小屏手机
            screenWidth < 600 -> 16.sp to 80.dp      // 标准手机
            screenWidth < 840 -> 20.sp to 96.dp      // 折叠屏/小平板
            else -> 24.sp to 112.dp                  // 大屏平板
        }
    }

    val gridColumns = remember(screenWidth, isLandscape) {
        when {
            screenWidth < 1080 -> GridCells.Fixed(2)
            else -> GridCells.Fixed(3)
        }
    }

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

    val coroutineScope = rememberCoroutineScope()
    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // 计算每个分组在网格平铺布局中的起始平铺索引（包含分组 Header 的位置占用）
    val groupToFlatIndexMap = remember(groupedSections) {
        val map = mutableMapOf<Int, Int>()
        var currentIndex = 0
        groupedSections.forEachIndexed { index, (_, phrases) ->
            map[index] = currentIndex
            // 如果分组数大于 1，则会有 Header 占用 1 个网格项位置
            val headerCount = if (groupedSections.size > 1) 1 else 0
            currentIndex += headerCount + phrases.size
        }
        map
    }

    // 当前选中的 Tab 索引
    val selectedGroupIndex by remember(groupedSections, selectedGroupId) {
        derivedStateOf {
            groupedSections.indexOfFirst { (group, _) -> group.id == selectedGroupId }
                .takeIf { it >= 0 } ?: 0
        }
    }

    // 根据网格当前可视的第一项，反向推导当前应当高亮的分组索引（用于滚动同步 Tab）
    val currentVisibleGroupIndex by remember(groupToFlatIndexMap, groupedSections) {
        derivedStateOf {
            val firstVisible = phraseGridState.firstVisibleItemIndex
            groupToFlatIndexMap.entries.lastOrNull { it.value <= firstVisible }?.key ?: 0
        }
    }

    // 监听滚动状态更新 Tab（仅在非主动点击 Tab 触发的滚动中同步）
    LaunchedEffect(currentVisibleGroupIndex, groupedSections) {
        if (!isProgrammaticScroll) {
            val targetId = groupedSections.getOrNull(currentVisibleGroupIndex)?.first?.id
            if (targetId != null && targetId != selectedGroupId) {
                selectedGroupId = targetId
            }
        }
    }

    LaunchedEffect(groupedSections) {
        val currentSelectionStillExists = groupedSections.any { (group, _) ->
            group.id == selectedGroupId
        }
        if (!currentSelectionStillExists) {
            selectedGroupId = groupedSections.firstOrNull()?.first?.id
        }
    }

    LaunchedEffect(selectedGroupId) {
        if (selectedGroupId != null) {
            isDisplayCompressed = false
        }
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

    editingPhrase?.let { phrase ->
        EditPhraseDialog(
            phrase = phrase,
            onDismiss = { editingPhrase = null },
            onSave = { label, speech ->
                onUpdatePhrase(phrase, label, speech)
                editingPhrase = null
            })
    }

    if (isLandscape) {
        Row(
            modifier = screenModifier, horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(2f)
                    .padding(bottom = 16.dp)
            ) {
                DisplaySurface(
                    text = effectiveDisplayText,
                    isSubtle = isShowingInitialHint,
                    scrollState = scrollState,
                    onClear = onClearClick
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    showPhraseGrid -> {
                        Column {
                            if (groupedSections.size > 1) {
                                GroupTabs(
                                    groupedSections = groupedSections,
                                    selectedGroupIndex = selectedGroupIndex,
                                    onGroupSelected = { index ->
                                        val targetId = groupedSections.getOrNull(index)?.first?.id
                                        if (targetId != null) {
                                            selectedGroupId = targetId
                                            val targetFlatIndex = groupToFlatIndexMap[index] ?: 0
                                            coroutineScope.launch {
                                                isProgrammaticScroll = true
                                                phraseGridState.animateScrollToItem(targetFlatIndex)
                                                isProgrammaticScroll = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            PhraseGrid(
                                modifier = Modifier.fillMaxSize(),
                                groupedSections = groupedSections,
                                state = phraseGridState,
                                columns = gridColumns,
                                cardFontSize = cardFontSize,
                                cardHeight = cardHeight,
                                onPhraseClick = onPhraseClick,
                                onPhraseLongClick = { editingPhrase = it },
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
                    isSubtle = isShowingInitialHint,
                    scrollState = scrollState,
                    onClear = onClearClick
                )
            }

            Spacer(Modifier.height(16.dp))

            when {
                showPhraseGrid -> {
                    Column(modifier = Modifier.weight(phraseAreaWeight)) {
                        if (groupedSections.size > 1) {
                            GroupTabs(
                                groupedSections = groupedSections,
                                selectedGroupIndex = selectedGroupIndex,
                                onGroupSelected = { index ->
                                    val targetId = groupedSections.getOrNull(index)?.first?.id
                                    if (targetId != null) {
                                        selectedGroupId = targetId
                                        val targetFlatIndex = groupToFlatIndexMap[index] ?: 0
                                        coroutineScope.launch {
                                            isProgrammaticScroll = true
                                            phraseGridState.animateScrollToItem(targetFlatIndex)
                                            isProgrammaticScroll = false
                                        }
                                    }
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        PhraseGrid(
                            modifier = Modifier
                                .weight(1f)
                                .nestedScroll(phraseGridNestedScrollConnection),
                            groupedSections = groupedSections,
                            state = phraseGridState,
                            columns = gridColumns,
                            cardFontSize = cardFontSize,
                            cardHeight = cardHeight,
                            onPhraseClick = onPhraseClick,
                            onPhraseLongClick = { editingPhrase = it },
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
private fun GroupTabs(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    selectedGroupIndex: Int,
    onGroupSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedGroupIndex,
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp,
        divider = {} // 移除底部的默认分割线
    ) {
        groupedSections.forEachIndexed { index, (group, _) ->
            val selected = selectedGroupIndex == index
            Tab(selected = selected, onClick = { onGroupSelected(index) }, text = {
                Text(
                    text = group.name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.titleSmall
                )
            })
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
        // 2. 使用 BoxWithConstraints 动态获取当前展示区域的大小
        BoxWithConstraints(
            contentAlignment = Alignment.Center, modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight

            // 根据可用容器大小和字数，动态缩放展示区的文字大小
            val baseFontSize = when {
                containerHeight < 120.dp || containerWidth < 280.dp -> {
                    // 高度高度压缩时
                    if (text.length > 20) 26.sp else 36.sp
                }
                containerHeight < 220.dp || containerWidth < 380.dp -> {
                    // 中度压缩状态（或较小的横屏手机）
                    if (text.length > 20) 40.sp else 56.sp
                }
                containerHeight < 360.dp || containerWidth < 600.dp -> {
                    // 标准手机的正常显示区域
                    if (text.length > 20) 60.sp else 84.sp
                }
                else -> {
                    // 平板设备或宽大显示区域
                    if (text.length > 20) 80.sp else 110.sp
                }
            }
            val lineHeight = baseFontSize * 1.15f

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
                            fontSize = baseFontSize,
                            lineHeight = lineHeight
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
private fun EditPhraseDialog(
    phrase: Phrase, onDismiss: () -> Unit, onSave: (label: String, speech: String) -> Unit
) {
    var label by rememberSaveable(phrase.id) { mutableStateOf(phrase.label) }
    var speech by rememberSaveable(phrase.id) { mutableStateOf(phrase.speech) }

    val canSave = label.isNotBlank() && speech.isNotBlank()

    AlertDialog(onDismissRequest = onDismiss, icon = {
        Icon(
            imageVector = Icons.Default.Edit, contentDescription = null
        )
    }, title = {
        Text("编辑短语")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhraseGrid(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    state: LazyGridState,
    columns: GridCells,
    onPhraseClick: (Phrase) -> Unit,
    onPhraseLongClick: (Phrase) -> Unit,
    onDisplayShouldExpand: () -> Unit,
    modifier: Modifier = Modifier,
    cardFontSize: TextUnit = 18.sp,
    cardHeight: Dp = 80.dp
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
            // 如果分组数多于一个，则在每个分组上方增加一个横跨全宽的标题 Header
            if (groupedSections.size > 1) {
                item(
                    key = "header_${group.id}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }
            }

            items(
                items = groupPhrases, key = { phrase -> phrase.id }) { phrase ->
                val buttonShape = MaterialTheme.shapes.large

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .clip(buttonShape)
                        .combinedClickable(role = Role.Button, onClick = {
                            performButtonHaptic()
                            onDisplayShouldExpand()
                            onPhraseClick(phrase)
                        }, onLongClick = {
                            performButtonHaptic()
                            onPhraseLongClick(phrase)
                        }),
                    shape = buttonShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = phrase.label,
                            fontSize = cardFontSize,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
