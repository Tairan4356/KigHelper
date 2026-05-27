package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.drag.PhraseDragInfo
import com.ziegler.kighelper.ui.screens.main.PhraseGrid

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
    onSpeakClick: (String) -> Unit,
    onNavigateToAddPhrase: () -> Unit,
    onPhraseDragStart: (PhraseDragInfo) -> Unit,
    onPhraseDragMove: (PhraseDragInfo) -> Unit,
    onPhraseDragEnd: (PhraseDragInfo) -> Unit,
) {
    val scrollState = rememberScrollState()
    val phraseGridState = rememberLazyGridState()
    var isDisplayCompressed by remember { mutableStateOf(false) }

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
    val fullscreenLandscapeModifier = modifier
        .fillMaxSize()
        // 横屏全屏模式隐藏了导航栏，四边使用一致边距，避免显示卡片产生偏心感。
        .padding(pagePadding)

    val hasPhrases = phrases.isNotEmpty()
    val showPhraseGrid = !isPhrasesLoading && hasPhrases
    val showEmptyState = !isPhrasesLoading && !hasPhrases

    val effectiveDisplayText = when {
        isPhrasesLoading && isShowingInitialHint -> ""
        !hasPhrases && isShowingInitialHint -> "先添加一个常用短语吧"
        else -> displayText
    }

    val effectiveIsSubtle = isShowingInitialHint

    val groupedSections = remember(phrases.toList(), groups.toList()) {
        buildGroupedSections(phrases = phrases, groups = groups)
    }

    val groupHeaderPositions = remember(groupedSections) {
        var offset = 0
        val positions = mutableMapOf<String, Int>()
        for ((group, groupPhrases) in groupedSections) {
            positions[group.id] = offset
            offset += 1 + groupPhrases.size
        }
        positions
    }

    val currentGroupIndex by remember(groupedSections) {
        derivedStateOf {
            val visibleIndex = phraseGridState.firstVisibleItemIndex
            var offset = 0
            for ((index, entry) in groupedSections.withIndex()) {
                val groupPhrases = entry.second
                val groupEnd = offset + groupPhrases.size
                if (visibleIndex in offset..groupEnd) return@derivedStateOf index
                offset = groupEnd + 1
            }
            groupedSections.indices.firstOrNull() ?: 0
        }
    }

    var scrollToGroupIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(scrollToGroupIndex) {
        val targetIndex = scrollToGroupIndex ?: return@LaunchedEffect
        val targetGroup = groupedSections.getOrNull(targetIndex) ?: return@LaunchedEffect
        val headerPosition = groupHeaderPositions[targetGroup.first.id] ?: return@LaunchedEffect
        phraseGridState.animateScrollToItem(headerPosition)
        scrollToGroupIndex = null
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

    LaunchedEffect(effectiveDisplayText) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(isPhraseGridAtTop, hasPhrases, isPhrasesLoading) {
        if (isPhrasesLoading || !hasPhrases || isPhraseGridAtTop) {
            isDisplayCompressed = false
        }
    }

    AnimatedContent(
        targetState = isLandscape, transitionSpec = {
            // 横竖屏切换只改变主屏布局，不改变当前显示的文本状态。
            (fadeIn(animationSpec = spring()) + scaleIn(initialScale = 0.98f)).togetherWith(
                fadeOut(
                    animationSpec = spring()
                ) + scaleOut(targetScale = 0.98f)
            )
        }, label = "mainScreenOrientationLayout"
    ) { landscapeFullscreen ->
        if (landscapeFullscreen) {
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .nestedScroll(phraseGridNestedScrollConnection),
                                    groupedSections = groupedSections,
                                    state = phraseGridState,
                                    columns = GridCells.Fixed(2),
                                    onPhraseClick = onPhraseClick,
                                    onAddPhraseClick = onNavigateToAddPhrase,
                                    onPhraseDragStart = onPhraseDragStart,
                                    onPhraseDragMove = onPhraseDragMove,
                                    onPhraseDragEnd = onPhraseDragEnd,
                                    onDisplayShouldExpand = {
                                        isDisplayCompressed = false
                                    })
                            }
                        }

                        showEmptyState -> {
                            EmptyPhraseState(
                                modifier = Modifier.fillMaxWidth(),
                                onAddClick = onNavigateToAddPhrase
                            )
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
                            GroupTabRow(
                                groupedSections = groupedSections,
                                currentGroupIndex = currentGroupIndex,
                                onGroupClick = { index ->
                                    scrollToGroupIndex = index
                                })

                            // 短语网格、加号按钮和 PhraseButton 拖拽手势作为一个整体移到了 ui.screens.main.PhraseGrid.kt。
                            PhraseGrid(
                                modifier = Modifier
                                    .weight(1f)
                                    .nestedScroll(phraseGridNestedScrollConnection),
                                groupedSections = groupedSections,
                                state = phraseGridState,
                                columns = GridCells.Fixed(2),
                                onPhraseClick = onPhraseClick,
                                onAddPhraseClick = onNavigateToAddPhrase,
                                onPhraseDragStart = onPhraseDragStart,
                                onPhraseDragMove = onPhraseDragMove,
                                onPhraseDragEnd = onPhraseDragEnd,
                                onDisplayShouldExpand = {
                                    isDisplayCompressed = false
                                })
                        }
                    }

                    showEmptyState -> {
                        EmptyPhraseState(
                            modifier = Modifier
                                .weight(phraseAreaWeight)
                                .fillMaxWidth(),
                            onAddClick = onNavigateToAddPhrase
                        )
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
}

private fun buildGroupedSections(
    phrases: List<Phrase>, groups: List<PhraseGroup>
): List<Pair<PhraseGroup, List<Phrase>>> {
    val defaultGroup = groups.firstOrNull { it.id == PhraseGroup.DEFAULT_ID }
        ?: PhraseGroup(id = PhraseGroup.DEFAULT_ID, name = PhraseGroup.DEFAULT_NAME, order = 0)
    val nonDefaultGroups = groups.distinctBy { it.id }.filterNot { it.id == PhraseGroup.DEFAULT_ID }
        .sortedBy { it.order }
    val groupById = nonDefaultGroups.associateBy { it.id }
    val groupedPhrases = phrases.groupBy { phrase ->
        groupById[phrase.groupId] ?: defaultGroup
    }

    val sections = mutableListOf<Pair<PhraseGroup, List<Phrase>>>()
    for (group in nonDefaultGroups) {
        val groupPhrases = groupedPhrases[group].orEmpty()
        if (groupPhrases.isNotEmpty()) {
            sections += group to groupPhrases.distinctBy { it.id }
        }
    }

    val defaultPhrases = groupedPhrases[defaultGroup].orEmpty()
    if (defaultPhrases.isNotEmpty()) {
        sections += defaultGroup to defaultPhrases.distinctBy { it.id }
    }

    return sections
}

@Composable
private fun GroupTabRow(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    currentGroupIndex: Int,
    onGroupClick: (Int) -> Unit
) {
    if (groupedSections.size <= 1) return

    ScrollableTabRow(
        selectedTabIndex = currentGroupIndex.coerceIn(groupedSections.indices), edgePadding = 0.dp
    ) {
        groupedSections.forEachIndexed { index, (group, _) ->
            Tab(
                selected = currentGroupIndex == index,
                onClick = { onGroupClick(index) },
                text = { Text(group.name) })
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
            AnimatedContent(
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
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DisplaySurfaceActionButton(
                        onClick = onClear, contentDescription = "清除"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplaySurfaceActionButton(
    onClick: () -> Unit, contentDescription: String, content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape
            )) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)
        ) {
            content()
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
