// 主 AAC 界面编排：展示区、短语网格布局、全屏模式和跨组件状态。
package com.ziegler.kighelper.ui.screens

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.main.AddPhraseDialog
import com.ziegler.kighelper.ui.screens.main.DisplaySurface
import com.ziegler.kighelper.ui.screens.main.EditPhraseDialog
import com.ziegler.kighelper.ui.screens.main.EmptyPhraseState
import com.ziegler.kighelper.ui.screens.main.GroupTabs
import com.ziegler.kighelper.ui.screens.main.LoadingPhraseState
import com.ziegler.kighelper.ui.screens.main.PhraseGrid
import com.ziegler.kighelper.ui.screens.main.rememberGroupedSections
import kotlinx.coroutines.launch

/**
 * 主界面：提供大字显示区域和短语快捷按钮网格。
 */
@OptIn(
    ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class
)
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
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val phraseGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var showAddPhraseDialog by rememberSaveable { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<Phrase?>(null) }

    val view = LocalView.current
    val context = LocalContext.current

    // 监听全屏状态以隐藏/显示系统状态栏
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
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 全屏时物理返回键拦截，退回普通状态
    BackHandler(enabled = isFullScreen) {
        onFullScreenChange(false)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp

    // 动态计算卡片字号、高度和列数
    val smallestScreenWidth = configuration.smallestScreenWidthDp

    // 动态卡片规格参数定义
    val (cardFontSize, cardHeight) = remember(smallestScreenWidth) {
        when {
            smallestScreenWidth < 360 -> 14.sp to 64.dp     // 小屏手机
            smallestScreenWidth < 600 -> 16.sp to 80.dp     // 标准手机
            smallestScreenWidth < 720 -> 20.sp to 96.dp     // 折叠屏/小平板 (通常 sw600dp ~ sw720dp)
            else -> 24.sp to 112.dp                         // 大屏平板 (通常 sw720dp+)
        }
    }

    val gridColumns = remember(screenWidth, isLandscape) {
        when {
            screenWidth < 1080 -> GridCells.Fixed(2)
            else -> GridCells.Fixed(3)
        }
    }

    // 连续滑动折叠状态设计
    val minWeight = 0.35f
    val maxWeight = 0.55f
    val density = LocalDensity.current
    // 设定滑动多少 dp 会使面板完全缩到最小
    val maxCollapseDistancePx = remember(density) { with(density) { 200.dp.toPx() } }

    // 连续变化的滑动偏移量（0f 代表完全展开，maxCollapseDistancePx 代表完全收起）
    var collapseOffset by remember { mutableFloatStateOf(0f) }

    // 将偏移量映射到权重
    val displayWeight = remember(collapseOffset, isLandscape) {
        if (isLandscape) {
            0.55f // 横屏不参与滑动收缩
        } else {
            val fraction = (collapseOffset / maxCollapseDistancePx).coerceIn(0f, 1f)
            maxWeight - fraction * (maxWeight - minWeight)
        }
    }
    val phraseAreaWeight = 1f - displayWeight

    // 拦截滑动事件的 NestedScrollConnection
    val phraseGridNestedScrollConnection = remember(maxCollapseDistancePx, isLandscape) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isLandscape) return Offset.Zero

                val dy = available.y
                return if (dy < 0f) {
                    // 向上滑动（手指上推）：在列表滚动前，优先缩减显示面板的大小
                    val remainingCollapse = maxCollapseDistancePx - collapseOffset
                    if (remainingCollapse > 0f) {
                        val consumedY = maxOf(dy, -remainingCollapse)
                        collapseOffset -= consumedY
                        Offset(0f, consumedY)
                    } else {
                        Offset.Zero
                    }
                } else if (dy > 0f) {
                    // 向下滑动（手指下拖）：只有在列表滚动到最顶部后，继续向下滑动才放大显示面板
                    val isAtTop =
                        phraseGridState.firstVisibleItemIndex == 0 && phraseGridState.firstVisibleItemScrollOffset == 0
                    if (isAtTop && collapseOffset > 0f) {
                        val consumedY = minOf(dy, collapseOffset)
                        collapseOffset -= consumedY
                        Offset(0f, consumedY)
                    } else {
                        Offset.Zero
                    }
                } else {
                    Offset.Zero
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
    val canEnterFullScreen = hasPhrases && !isShowingInitialHint

    val groupedSections = rememberGroupedSections(phrases = phrases, groups = groups)

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

    LaunchedEffect(effectiveDisplayText) {
        scrollState.scrollTo(0)
    }

    // 当空状态或加载中时，重置折叠高度
    LaunchedEffect(isPhrasesLoading, hasPhrases) {
        if (isPhrasesLoading || !hasPhrases) {
            collapseOffset = 0f
        }
    }

    if (showAddPhraseDialog) {
        AddPhraseDialog(onDismiss = { showAddPhraseDialog = false }, onSave = { label, speech ->
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

    // 引入共享元素过渡容器，协调切换
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !isFullScreen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(
                targetOffsetY = { it / 6 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            val animatedVisibilityScope = this

            val sharedModifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "display_surface"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = scaleToBounds(),
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(24.dp)),
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                )
                .clip(RoundedCornerShape(24.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLandscape) {
                    Row(
                        modifier = screenModifier,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                                onClear = onClearClick,
                                onClick = if (canEnterFullScreen) {
                                    { onFullScreenChange(true) }
                                } else null,
                                smallestScreenWidth = smallestScreenWidth,
                                modifier = sharedModifier)
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
                                                    val targetId =
                                                        groupedSections.getOrNull(index)?.first?.id
                                                    if (targetId != null) {
                                                        selectedGroupId = targetId
                                                        val targetFlatIndex =
                                                            groupToFlatIndexMap[index] ?: 0
                                                        coroutineScope.launch {
                                                            isProgrammaticScroll = true
                                                            phraseGridState.animateScrollToItem(
                                                                targetFlatIndex
                                                            )
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
                                                // 横屏下一般无需动画，直接重置
                                                collapseOffset = 0f
                                            })
                                    }
                                }

                                showEmptyState -> {
                                    EmptyPhraseState(
                                        modifier = Modifier.fillMaxSize(),
                                        onAddClick = { showAddPhraseDialog = true })
                                }

                                else -> {
                                    LoadingPhraseState(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = screenModifier) {
                        Box(modifier = Modifier.weight(displayWeight)) {
                            DisplaySurface(
                                text = effectiveDisplayText,
                                isSubtle = isShowingInitialHint,
                                scrollState = scrollState,
                                onClear = onClearClick,
                                onClick = if (canEnterFullScreen) {
                                    { onFullScreenChange(true) }
                                } else null,
                                smallestScreenWidth = smallestScreenWidth,
                                modifier = sharedModifier)
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
                                                val targetId =
                                                    groupedSections.getOrNull(index)?.first?.id
                                                if (targetId != null) {
                                                    selectedGroupId = targetId
                                                    val targetFlatIndex =
                                                        groupToFlatIndexMap[index] ?: 0
                                                    coroutineScope.launch {
                                                        isProgrammaticScroll = true
                                                        phraseGridState.animateScrollToItem(
                                                            targetFlatIndex
                                                        )
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
                                            // 点击卡片播报时，平滑回弹展开面板
                                            coroutineScope.launch {
                                                animate(
                                                    initialValue = collapseOffset,
                                                    targetValue = 0f,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                                ) { value, _ ->
                                                    collapseOffset = value
                                                }
                                            }
                                        })
                                }
                            }

                            showEmptyState -> {
                                EmptyPhraseState(
                                    modifier = Modifier
                                        .weight(phraseAreaWeight)
                                        .fillMaxWidth(),
                                    onAddClick = { showAddPhraseDialog = true })
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
        // 全屏显示的过渡动画层
        AnimatedVisibility(
            visible = isFullScreen,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            val fullscreenScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
            ) {
                DisplaySurface(
                    text = effectiveDisplayText,
                    isSubtle = isShowingInitialHint,
                    scrollState = fullscreenScrollState,
                    onClear = {
                        onClearClick()
                        onFullScreenChange(false)
                    },
                    onClick = { onFullScreenChange(false) }, // 点击退出全屏
                    smallestScreenWidth = smallestScreenWidth,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "display_surface"),
                            animatedVisibilityScope = this@AnimatedVisibility,
                            resizeMode = scaleToBounds(),
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(24.dp)),
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        )
                        .clip(RoundedCornerShape(24.dp))
                )
            }
        }
    }
}