package com.ziegler.kighelper.ui.screens.main

import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.Phrase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * MainScreen 的布局组件，处理横屏和竖屏布局
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreenLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    state: MainScreenState,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onAddPhrase: (label: String, speech: String) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    fontSizeMultiplier: Float = 1.0f,
    hapticFeedback: Boolean = true
) {
    if (state.isLandscape) {
        LandscapeLayout(
            modifier = modifier,
            contentPadding = contentPadding,
            state = state,
            onPhraseClick = onPhraseClick,
            onClearClick = onClearClick,
            onAddPhrase = onAddPhrase,
            onDeletePhrase = onDeletePhrase,
            onUpdatePhrase = onUpdatePhrase,
            animatedVisibilityScope = animatedVisibilityScope,
            fontSizeMultiplier = fontSizeMultiplier,
            hapticFeedback = hapticFeedback
        )
    } else {
        PortraitLayout(
            modifier = modifier,
            contentPadding = contentPadding,
            state = state,
            onPhraseClick = onPhraseClick,
            onClearClick = onClearClick,
            onAddPhrase = onAddPhrase,
            onDeletePhrase = onDeletePhrase,
            onUpdatePhrase = onUpdatePhrase,
            animatedVisibilityScope = animatedVisibilityScope,
            fontSizeMultiplier = fontSizeMultiplier,
            hapticFeedback = hapticFeedback
        )
    }
}

/**
 * 横屏布局
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun LandscapeLayout(
    modifier: Modifier,
    contentPadding: PaddingValues,
    state: MainScreenState,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onAddPhrase: (label: String, speech: String) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    fontSizeMultiplier: Float = 1.0f,
    hapticFeedback: Boolean = true
) {
    val layoutDirection = LocalLayoutDirection.current
    val outerStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val outerTopPadding = contentPadding.calculateTopPadding()
    val outerEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val outerBottomPadding = contentPadding.calculateBottomPadding()

    val screenModifier = modifier
        .fillMaxSize()
        .padding(
            start = outerStartPadding + 16.dp,
            top = outerTopPadding + 16.dp,
            end = outerEndPadding + 16.dp,
            bottom = outerBottomPadding
        )

    Row(
        modifier = screenModifier, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(2f)
                .padding(bottom = 16.dp)
        ) {
            DisplaySurface(
                text = state.effectiveDisplayText,
                isSubtle = state.isShowingInitialHint,
                scrollState = rememberScrollState(),
                onClear = onClearClick,
                onClick = if (state.canEnterFullScreen) {
                    { state.onFullScreenChange(true) }
                } else null,
                modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                fontSizeMultiplier = fontSizeMultiplier
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            PhraseAreaContent(
                state = state,
                onPhraseClick = onPhraseClick,
                onClearClick = onClearClick,
                onDeletePhrase = onDeletePhrase,
                onUpdatePhrase = onUpdatePhrase,
                modifier = Modifier.fillMaxSize(),
                hapticFeedback = hapticFeedback
            )
        }
    }
}

/**
 * 竖屏布局
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun PortraitLayout(
    modifier: Modifier,
    contentPadding: PaddingValues,
    state: MainScreenState,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onAddPhrase: (label: String, speech: String) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    fontSizeMultiplier: Float = 1.0f,
    hapticFeedback: Boolean = true
) {
    val layoutDirection = LocalLayoutDirection.current
    val outerStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val outerTopPadding = contentPadding.calculateTopPadding()
    val outerEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val outerBottomPadding = contentPadding.calculateBottomPadding()

    val screenModifier = modifier
        .fillMaxSize()
        .padding(
            start = outerStartPadding + 16.dp,
            top = outerTopPadding + 16.dp,
            end = outerEndPadding + 16.dp,
            bottom = outerBottomPadding
        )

    Column(modifier = screenModifier) {
        Box(modifier = Modifier.weight(state.displayWeight)) {
            DisplaySurface(
                text = state.effectiveDisplayText,
                isSubtle = state.isShowingInitialHint,
                scrollState = rememberScrollState(),
                onClear = onClearClick,
                onClick = if (state.canEnterFullScreen) {
                    { state.onFullScreenChange(true) }
                } else null,
                modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                fontSizeMultiplier = fontSizeMultiplier
            )
        }

        Spacer(Modifier.height(16.dp))

        PhraseAreaContent(
            state = state,
            onPhraseClick = onPhraseClick,
            onClearClick = onClearClick,
            onDeletePhrase = onDeletePhrase,
            onUpdatePhrase = onUpdatePhrase,
            modifier = Modifier
                .weight(state.phraseAreaWeight)
                .fillMaxWidth(),
            hapticFeedback = hapticFeedback
        )
    }
}

/**
 * 短语区域内容（网格、空状态、加载状态）
 */
@Composable
private fun PhraseAreaContent(
    state: MainScreenState,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
    modifier: Modifier = Modifier,
    hapticFeedback: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var expandJob: Job? by remember { mutableStateOf<Job?>(null) }
    val groupedSections = state.getGroupedSections()
    val groupToFlatIndexMap = state.getGroupToFlatIndexMap(groupedSections)
    val selectedGroupIndex = state.getSelectedGroupIndex(groupedSections)

    when {
        state.showPhraseGrid -> {
            Column(modifier = modifier) {
                if (groupedSections.size > 1) {
                    GroupTabs(
                        groupedSections = groupedSections,
                        selectedGroupIndex = selectedGroupIndex,
                        onGroupSelected = { index ->
                            val targetId = groupedSections.getOrNull(index)?.first?.id
                            if (targetId != null) {
                                state.selectedGroupId = targetId
                                val targetFlatIndex = groupToFlatIndexMap[index] ?: 0
                                coroutineScope.launch {
                                    state.phraseGridState.animateScrollToItem(targetFlatIndex)
                                }
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                PhraseGrid(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (!state.isLandscape) {
                                Modifier.nestedScroll(
                                    createNestedScrollConnection(state)
                                )
                            } else {
                                Modifier
                            }
                        ),
                    groupedSections = groupedSections,
                    state = state.phraseGridState,
                    columns = state.gridColumns,
                    cardFontSize = state.cardFontSize,
                    cardHeight = state.cardHeight,
                    onPhraseClick = onPhraseClick,
                    onPhraseLongClick = { state.editingPhrase = it },
                    onPhraseDelete = onDeletePhrase,
                    onDisplayShouldExpand = {
                        if (!state.isLandscape) {
                            expandJob?.cancel()
                            expandJob = coroutineScope.launch {
                                animate(
                                    initialValue = state.collapseOffset,
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                ) { value, _ ->
                                    state.collapseOffset = value
                                }
                            }
                        }
                    },
                    hapticFeedback = hapticFeedback
                )
            }
        }

        state.showEmptyState -> {
            EmptyPhraseState(
                modifier = modifier, onAddClick = { state.showAddPhraseDialog = true })
        }

        else -> {
            LoadingPhraseState(modifier = modifier)
        }
    }
}

/**
 * 创建嵌套滚动连接
 */
private fun createNestedScrollConnection(state: MainScreenState): NestedScrollConnection {
    return object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset, source: NestedScrollSource
        ): Offset {
            val dy = available.y
            return if (dy < 0f) {
                val remainingCollapse = state.maxCollapseDistancePx - state.collapseOffset
                if (remainingCollapse > 0f) {
                    val consumedY = maxOf(dy, -remainingCollapse)
                    state.collapseOffset -= consumedY
                    Offset(0f, consumedY)
                } else {
                    Offset.Zero
                }
            } else if (dy > 0f) {
                val isAtTop =
                    state.phraseGridState.firstVisibleItemIndex == 0 && state.phraseGridState.firstVisibleItemScrollOffset == 0
                if (isAtTop && state.collapseOffset > 0f) {
                    val consumedY = minOf(dy, state.collapseOffset)
                    state.collapseOffset -= consumedY
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
