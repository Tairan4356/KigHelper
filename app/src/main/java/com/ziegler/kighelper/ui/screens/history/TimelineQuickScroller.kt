package com.ziegler.kighelper.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * 快速浏览手柄的月份锚点。
 *
 * [gridIndex] 对应 LazyVerticalGrid 的 item 索引，而不是时光轴数据列表的索引；
 * 筛选栏、权限提示等前置卡片也会被计入。
 */
internal data class TimelineMonthMarker(
    val gridIndex: Int,
    val label: String
)

/**
 * 时光轴右侧的快速浏览手柄。
 *
 * 页面滚动时淡入，停止滚动后自动隐藏；拖动时把手柄位置换算成网格索引，
 * 并通过 [monthMarkers] 在柄旁展示当前月份，适合在很长的相册中快速跳转。
 */
@Composable
internal fun TimelineQuickScroller(
    gridState: LazyGridState,
    monthMarkers: List<TimelineMonthMarker>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // layoutInfo 变化频繁，放进 derivedStateOf，避免每次滚动都重组整个手柄。
    val totalItems by remember(gridState) {
        derivedStateOf { gridState.layoutInfo.totalItemsCount }
    }
    val scrollable by remember(gridState) {
        derivedStateOf { gridState.canScrollBackward || gridState.canScrollForward }
    }
    var visible by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var dragMonth by remember { mutableStateOf<String?>(null) }
    // 数据刷新导致锚点变化时，不重建正在进行的拖动手势。
    val latestMonthMarkers by rememberUpdatedState(monthMarkers)

    // 进入多选或删除流程时立即收起手柄，避免遮挡底部操作栏。
    LaunchedEffect(enabled) {
        if (!enabled) {
            isDragging = false
            dragMonth = null
        }
    }

    // 用首个可见 item 在“可滚动范围”内的比例估算手柄位置，滚到底部时能贴合轨道末端。
    val scrollProgress by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            if (total <= 1) {
                0f
            } else {
                val visibleCount = info.visibleItemsInfo.size.coerceAtLeast(1)
                val maxFirstIndex = (total - visibleCount).coerceAtLeast(1)
                (gridState.firstVisibleItemIndex.toFloat() / maxFirstIndex)
                    .coerceIn(0f, 1f)
            }
        }
    }

    // 页面滚动时显示手柄，停止滚动 1.8 秒后自动淡出；拖动期间一直保持可见。
    LaunchedEffect(gridState.isScrollInProgress, isDragging, enabled) {
        if (!enabled) {
            visible = false
            return@LaunchedEffect
        }
        if (isDragging || gridState.isScrollInProgress) {
            visible = true
        } else {
            delay(1_800L.milliseconds)
            if (!isDragging && !gridState.isScrollInProgress) visible = false
        }
    }

    AnimatedVisibility(
        visible = enabled && visible && scrollable && totalItems > 1,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally { width -> width / 2 },
        exit = fadeOut() + slideOutHorizontally { width -> width / 2 }
    ) {
        // 外层预留 160dp 宽度放月份气泡，手势条只占最右侧 48dp，其余区域不拦截列表操作。
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .width(160.dp)
        ) {
            val thumbHeight = 52.dp
            val bubbleHeight = 32.dp
            val maxThumbOffset = (maxHeight - thumbHeight).coerceAtLeast(0.dp)
            val progress = (if (isDragging) dragProgress else scrollProgress)
                .coerceIn(0f, 1f)
            val thumbOffset = maxThumbOffset * progress
            val bubbleOffset = (thumbOffset + thumbHeight / 2 - bubbleHeight / 2)
                .coerceIn(0.dp, (maxHeight - bubbleHeight).coerceAtLeast(0.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(gridState) {
                        // 目标索引可能落在同一月份的任意一天，取它之前最近的月份锚点。
                        fun monthAt(index: Int): String? {
                            return latestMonthMarkers.lastOrNull { it.gridIndex <= index }?.label
                                ?: latestMonthMarkers.firstOrNull()?.label
                        }

                        // 拖动位置按轨道高度归一化，再映射到网格索引；使用即时滚动避免惯性延迟。
                        fun scrollTo(y: Float) {
                            val total = gridState.layoutInfo.totalItemsCount
                            if (total <= 0) return
                            val fraction = (y / size.height).coerceIn(0f, 1f)
                            val target = (fraction * (total - 1))
                                .roundToInt()
                                .coerceIn(0, total - 1)
                            dragProgress = fraction
                            dragMonth = monthAt(target)
                            gridState.requestScrollToItem(target)
                        }

                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                scrollTo(offset.y)
                            },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                scrollTo(change.position.y)
                            },
                            onDragEnd = {
                                isDragging = false
                                dragMonth = null
                            },
                            onDragCancel = {
                                isDragging = false
                                dragMonth = null
                            })
                    }
            ) {
                // 半透明轨道提供拖动范围参考，实际拖动区域是右侧 48dp 的手势条。
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
                // 手柄本身保持较窄，避免遮挡缩略图内容。
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = thumbOffset)
                        .padding(end = 2.dp)
                        .width(12.dp)
                        .height(thumbHeight)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            RoundedCornerShape(6.dp)
                        )
                )
            }

            // 月份气泡只在拖动时显示，并跟随手柄中心上下移动。
            if (isDragging && dragMonth != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-36).dp, y = bubbleOffset)
                        .widthIn(max = 116.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = dragMonth.orEmpty(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
