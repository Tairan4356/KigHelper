package com.ziegler.kighelper.ui.screens.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.drag.PhraseDragInfo
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val AddPhraseGridItemKey = "add_phrase_grid_item"
private const val DragClickSuppressMillis = 180L // 防止误触的宽容时长 单位毫秒
private const val DragLongPressThresholdMillis = 200L // 触发拖放的长按时间阈值，单位毫秒
private const val DragScaleAnimationMillis = 200 // 放大被拖放的按钮动画时长，单位毫秒
private const val DragActiveScale = 1.20f // 放大被拖放的按钮比例
private const val DragPlaceholderAlpha = 0.35f // 根层副本拖动时，原位置按钮只作为占位提示

@Composable
internal fun PhraseGrid(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    state: LazyGridState,
    columns: GridCells,
    onPhraseClick: (Phrase) -> Unit,
    onAddPhraseClick: () -> Unit,
    onPhraseDragStart: (PhraseDragInfo) -> Unit,
    onPhraseDragMove: (PhraseDragInfo) -> Unit,
    onPhraseDragEnd: (PhraseDragInfo) -> Unit,
    onDisplayShouldExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val performButtonHaptic = rememberPhysicalButtonHaptics()

    // LazyVerticalGrid 渲染分组标题、短语按钮和末尾添加按钮；拖拽手势仍集中在 PhraseButton。
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
                key = "header_${group.id}",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 12.dp,
                        bottom = 4.dp
                    )
                )
            }

            items(
                items = groupPhrases,
                key = { phrase -> phrase.id }
            ) { phrase ->
                PhraseButton(
                    phrase = phrase,
                    onPhraseClick = onPhraseClick,
                    onPhraseDragStart = onPhraseDragStart,
                    onPhraseDragMove = onPhraseDragMove,
                    onPhraseDragEnd = onPhraseDragEnd,
                    onDisplayShouldExpand = onDisplayShouldExpand
                )
            }
        }

        item(key = AddPhraseGridItemKey) {
            Button(
                onClick = {
                    performButtonHaptic()
                    onAddPhraseClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加短语",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PhraseButton(
    phrase: Phrase,
    onPhraseClick: (Phrase) -> Unit,
    onPhraseDragStart: (PhraseDragInfo) -> Unit,
    onPhraseDragMove: (PhraseDragInfo) -> Unit,
    onPhraseDragEnd: (PhraseDragInfo) -> Unit,
    onDisplayShouldExpand: () -> Unit
) {
    val performButtonHaptic = rememberPhysicalButtonHaptics()
    var isDragging by remember(phrase.id) { mutableStateOf(false) }
    var suppressNextClick by remember(phrase.id) { mutableStateOf(false) }
    var buttonTopStartInRoot by remember(phrase.id) { mutableStateOf(Offset.Zero) }
    var buttonSize by remember(phrase.id) { mutableStateOf(IntSize.Zero) }
    val animatedDragScale by animateFloatAsState(
        targetValue = if (isDragging) DragActiveScale else 1f,
        animationSpec = tween(durationMillis = DragScaleAnimationMillis),
        label = "phraseButtonDragScale"
    )

    // 拖拽结束后短暂屏蔽点击，避免放手事件又触发短语朗读。
    LaunchedEffect(suppressNextClick) {
        if (suppressNextClick) {
            delay(DragClickSuppressMillis)
            suppressNextClick = false
        }
    }

    Button(
        onClick = {
            if (suppressNextClick) return@Button
            performButtonHaptic()
            onDisplayShouldExpand()
            onPhraseClick(phrase)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { coordinates ->
                buttonTopStartInRoot = coordinates.boundsInRoot().topLeft
                buttonSize = coordinates.size
            }
            .graphicsLayer {
                // 原位置按钮只做占位提示；真正跟手移动的副本由 App 根层绘制。
                scaleX = animatedDragScale
                scaleY = animatedDragScale
                alpha = if (isDragging) DragPlaceholderAlpha else 1f
                shadowElevation = if (isDragging) 12f else 0f
            }
            .pointerInput(phrase.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPosition = down.position

                    var pointerStillEligible = true
                    val canceledBeforeThreshold = withTimeoutOrNull(DragLongPressThresholdMillis) {
                        while (pointerStillEligible) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                pointerStillEligible = false
                                break
                            }

                            val movedBeforeThreshold =
                                (change.position - startPosition).getDistance() >
                                    viewConfiguration.touchSlop

                            if (!change.pressed || change.isConsumed || movedBeforeThreshold) {
                                pointerStillEligible = false
                                break
                            }
                        }
                        true
                    } ?: false

                    if (canceledBeforeThreshold || !pointerStillEligible) return@awaitEachGesture

                    // 达到长按阈值后才开始消费事件，避免普通滑动 PhraseGrid 时误触发拖拽。
                    val touchOffsetInButton = down.position
                    var dragInfo = PhraseDragInfo(
                        phrase = phrase,
                        pointerPositionInRoot = buttonTopStartInRoot + touchOffsetInButton,
                        touchOffsetInButton = touchOffsetInButton,
                        buttonSize = buttonSize
                    )
                    isDragging = true
                    onPhraseDragStart(dragInfo)
                    onDisplayShouldExpand()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val dragAmount = change.positionChange()
                        if (dragAmount != Offset.Zero) {
                            change.consume()
                        }

                        // 持续上报根坐标和触点在按钮内的偏移，根层副本用它们精确贴住手指移动。
                        dragInfo = dragInfo.copy(
                            pointerPositionInRoot = buttonTopStartInRoot + change.position
                        )
                        onPhraseDragMove(dragInfo)
                    }

                    isDragging = false
                    suppressNextClick = true
                    onPhraseDragEnd(dragInfo)
                }
            },
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
