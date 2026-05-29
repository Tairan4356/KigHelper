// 主 AAC 界面编排：展示区、短语网格布局、全屏模式和跨组件状态。
package com.ziegler.kighelper.ui.screens

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.main.AddPhraseDialog
import com.ziegler.kighelper.ui.screens.main.DisplaySurface
import com.ziegler.kighelper.ui.screens.main.DisplaySurfaceLayoutMode
import com.ziegler.kighelper.ui.screens.main.EditPhraseDialog
import com.ziegler.kighelper.ui.screens.main.EmptyPhraseState
import com.ziegler.kighelper.ui.screens.main.GroupTabs
import com.ziegler.kighelper.ui.screens.main.LoadingPhraseState
import com.ziegler.kighelper.ui.screens.main.PhraseGrid
import com.ziegler.kighelper.ui.screens.main.buildGroupStartIndexMap
import com.ziegler.kighelper.ui.screens.main.buildGroupedSections
import kotlinx.coroutines.launch

/**
 * 主 AAC 界面，协调展示文本、短语选择、分组标签和全屏模式。
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    phrases: List<Phrase>,
    groups: List<PhraseGroup> = emptyList(),
    displayText: String,
    isShowingInitialHint: Boolean,
    isPhrasesLoading: Boolean,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onAddPhrase: (label: String, speech: String) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val phraseGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var showAddPhraseDialog by rememberSaveable { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<Phrase?>(null) }

    val view = LocalView.current
    val context = LocalContext.current

    // 将 Android 系统栏状态与应用内全屏展示状态保持同步。
    DisposableEffect(isFullScreen) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (isFullScreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullScreen) {
        onFullScreenChange(false)
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }

    // 按屏幕宽度档位稳定短语按钮的字号和高度。
    val (cardFontSize, cardHeight) = remember(screenWidth) {
        when {
            screenWidth < 360.dp -> 14.sp to 64.dp
            screenWidth < 600.dp -> 16.sp to 80.dp
            screenWidth < 840.dp -> 20.sp to 96.dp
            else -> 24.sp to 112.dp
        }
    }

    val gridColumns = remember(screenWidth) {
        when {
            screenWidth < 1080.dp -> GridCells.Fixed(2)
            else -> GridCells.Fixed(3)
        }
    }

    val minWeight = 0.35f
    val maxWeight = 0.55f
    val maxCollapseDistancePx = remember(density) { with(density) { 200.dp.toPx() } }
    var collapseOffset by remember { mutableFloatStateOf(0f) }

    // 将拖动距离映射到展示区和网格区权重，让上方展示区先于网格滚动收缩。
    val displayWeight = remember(collapseOffset, isLandscape) {
        if (isLandscape) {
            0.55f
        } else {
            val fraction = (collapseOffset / maxCollapseDistancePx).coerceIn(0f, 1f)
            maxWeight - fraction * (maxWeight - minWeight)
        }
    }
    val phraseAreaWeight = 1f - displayWeight

    val phraseGridNestedScrollConnection = remember(maxCollapseDistancePx, isLandscape) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isLandscape) return Offset.Zero

                val dy = available.y
                return when {
                    dy < 0f -> {
                        val remainingCollapse = maxCollapseDistancePx - collapseOffset
                        if (remainingCollapse > 0f) {
                            val consumedY = maxOf(dy, -remainingCollapse)
                            collapseOffset -= consumedY
                            Offset(0f, consumedY)
                        } else {
                            Offset.Zero
                        }
                    }

                    dy > 0f -> {
                        val isAtTop =
                            phraseGridState.firstVisibleItemIndex == 0 &&
                                phraseGridState.firstVisibleItemScrollOffset == 0
                        if (isAtTop && collapseOffset > 0f) {
                            val consumedY = minOf(dy, collapseOffset)
                            collapseOffset -= consumedY
                            Offset(0f, consumedY)
                        } else {
                            Offset.Zero
                        }
                    }

                    else -> Offset.Zero
                }
            }
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

    val groupedSections = remember(phrases.toList(), groups.toList()) {
        buildGroupedSections(phrases = phrases, groups = groups)
    }
    val groupToFlatIndexMap = remember(groupedSections) {
        buildGroupStartIndexMap(groupedSections)
    }

    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    val selectedGroupIndex by remember(groupedSections, selectedGroupId) {
        derivedStateOf {
            groupedSections.indexOfFirst { (group, _) -> group.id == selectedGroupId }
                .takeIf { it >= 0 } ?: 0
        }
    }
    val currentVisibleGroupIndex by remember(groupToFlatIndexMap, groupedSections) {
        derivedStateOf {
            val firstVisible = phraseGridState.firstVisibleItemIndex
            groupToFlatIndexMap.entries.lastOrNull { it.value <= firstVisible }?.key ?: 0
        }
    }

    // 将网格滚动同步回选中的分组标签；由标签主动触发的滚动不反向同步。
    LaunchedEffect(currentVisibleGroupIndex, groupedSections) {
        if (!isProgrammaticScroll) {
            val targetId = groupedSections.getOrNull(currentVisibleGroupIndex)?.first?.id
            if (targetId != null && targetId != selectedGroupId) {
                selectedGroupId = targetId
            }
        }
    }

    LaunchedEffect(groupedSections) {
        if (groupedSections.none { (group, _) -> group.id == selectedGroupId }) {
            selectedGroupId = groupedSections.firstOrNull()?.first?.id
        }
    }

    LaunchedEffect(effectiveDisplayText) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(isPhrasesLoading, hasPhrases) {
        if (isPhrasesLoading || !hasPhrases) {
            collapseOffset = 0f
        }
    }

    if (showAddPhraseDialog) {
        AddPhraseDialog(
            onDismiss = { showAddPhraseDialog = false },
            onSave = { label, speech ->
                onAddPhrase(label, speech)
                showAddPhraseDialog = false
            }
        )
    }

    editingPhrase?.let { phrase ->
        EditPhraseDialog(
            phrase = phrase,
            onDismiss = { editingPhrase = null },
            onSave = { label, speech ->
                onUpdatePhrase(phrase, label, speech)
                editingPhrase = null
            }
        )
    }

    fun selectGroup(index: Int) {
        val targetId = groupedSections.getOrNull(index)?.first?.id ?: return
        selectedGroupId = targetId
        val targetFlatIndex = groupToFlatIndexMap[index] ?: 0
        coroutineScope.launch {
            isProgrammaticScroll = true
            phraseGridState.animateScrollToItem(targetFlatIndex)
            isProgrammaticScroll = false
        }
    }

    val displayLayoutMode = when {
        isFullScreen -> DisplaySurfaceLayoutMode.Fullscreen
        isLandscape -> DisplaySurfaceLayoutMode.Landscape
        else -> DisplaySurfaceLayoutMode.Portrait
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(
                modifier = screenModifier,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(if (isFullScreen) 1f else 2f)
                        .padding(bottom = if (isFullScreen) 0.dp else 16.dp)
                ) {
                    DisplaySurface(
                        text = effectiveDisplayText,
                        isSubtle = isShowingInitialHint,
                        scrollState = scrollState,
                        onClear = onClearClick,
                        onClick = { onFullScreenChange(!isFullScreen) },
                        layoutMode = displayLayoutMode
                    )
                }

                if (!isFullScreen) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            showPhraseGrid -> {
                                Column {
                                    GroupTabs(
                                        groupedSections = groupedSections,
                                        selectedGroupIndex = selectedGroupIndex,
                                        onGroupSelected = ::selectGroup,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    PhraseGrid(
                                        modifier = Modifier.fillMaxSize(),
                                        groupedSections = groupedSections,
                                        state = phraseGridState,
                                        columns = gridColumns,
                                        cardFontSize = cardFontSize,
                                        cardHeight = cardHeight,
                                        onPhraseClick = onPhraseClick,
                                        onPhraseLongClick = { editingPhrase = it },
                                        onPhraseDelete = onDeletePhrase,
                                        onDisplayShouldExpand = {
                                            collapseOffset = 0f
                                        }
                                    )
                                }
                            }

                            showEmptyState -> {
                                EmptyPhraseState(
                                    modifier = Modifier.fillMaxSize(),
                                    onAddClick = { showAddPhraseDialog = true }
                                )
                            }

                            else -> {
                                LoadingPhraseState(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = screenModifier) {
                Box(
                    modifier = Modifier.weight(if (isFullScreen) 1f else displayWeight)
                ) {
                    DisplaySurface(
                        text = effectiveDisplayText,
                        isSubtle = isShowingInitialHint,
                        scrollState = scrollState,
                        onClear = onClearClick,
                        onClick = { onFullScreenChange(!isFullScreen) },
                        layoutMode = displayLayoutMode
                    )
                }

                if (!isFullScreen) {
                    Spacer(Modifier.height(16.dp))

                    when {
                        showPhraseGrid -> {
                            Column(modifier = Modifier.weight(phraseAreaWeight)) {
                                GroupTabs(
                                    groupedSections = groupedSections,
                                    selectedGroupIndex = selectedGroupIndex,
                                    onGroupSelected = ::selectGroup,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

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
                                    onPhraseDelete = onDeletePhrase,
                                    onDisplayShouldExpand = {
                                        coroutineScope.launch {
                                            animate(
                                                initialValue = collapseOffset,
                                                targetValue = 0f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                            ) { value, _ ->
                                                collapseOffset = value
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        showEmptyState -> {
                            EmptyPhraseState(
                                modifier = Modifier
                                    .weight(phraseAreaWeight)
                                    .fillMaxWidth(),
                                onAddClick = { showAddPhraseDialog = true }
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
}
