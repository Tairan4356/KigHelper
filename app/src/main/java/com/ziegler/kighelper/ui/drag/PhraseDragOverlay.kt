package com.ziegler.kighelper.ui.drag

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val DeleteDropTargetAnimationMillis = 200
private const val DeleteDropTargetColorAnimationMillis = 160
private const val DeleteDropTargetActiveDarkenFactor = 0.72f
private const val DraggedPhraseOverlayScale = 1.20f

@Composable
internal fun DeleteDropTarget(
    visible: Boolean,
    isActive: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    // 背景全程保持不透明：未命中使用主题 error 色，命中时只把色阶压深。
    val targetContainerColor =
        if (isActive) {
            MaterialTheme.colorScheme.error.darken(DeleteDropTargetActiveDarkenFactor)
        } else {
            MaterialTheme.colorScheme.error
        }

    // 颜色变化用短动画过渡，拖入和移出删除区时都不会突然跳色。
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(DeleteDropTargetColorAnimationMillis),
        label = "deleteDropTargetContainerColor"
    )

    // 删除区自身仍然沿用飞入/飞出动画，只在拖拽期间覆盖底部导航。
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(DeleteDropTargetAnimationMillis)
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(DeleteDropTargetAnimationMillis)
        )
    ) {
        // Surface 记录自己的根坐标范围，供放手时判断触摸点是否落在删除区内。
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInRoot())
                },
            shape = MaterialTheme.shapes.extraLarge,
            color = animatedContainerColor,
            tonalElevation = if (isActive) 16.dp else 4.dp,
            shadowElevation = if (isActive) 24.dp else 6.dp
        ) {
            // 文案随命中状态变化，比单纯变色更直接地提示“松手即删除”。
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError
                )

                Spacer(Modifier.size(8.dp))

                Text(
                    text = if (isActive) "松手删除" else "拖到这里删除",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun Color.darken(factor: Float): Color =
    copy(
        red = red * factor,
        green = green * factor,
        blue = blue * factor,
        alpha = 1f
    )

@Composable
internal fun DraggedPhraseOverlay(
    dragInfo: PhraseDragInfo?,
    modifier: Modifier = Modifier
) {
    if (dragInfo == null || dragInfo.buttonSize.width <= 0 || dragInfo.buttonSize.height <= 0) {
        return
    }

    val density = LocalDensity.current
    val overlayScale = DraggedPhraseOverlayScale

    // 拖拽副本复用原按钮的像素尺寸，再按拖拽视觉比例转成 dp 尺寸。
    val overlayWidth = with(density) { (dragInfo.buttonSize.width * overlayScale).toDp() }
    val overlayHeight = with(density) { (dragInfo.buttonSize.height * overlayScale).toDp() }

    // 根层副本的坐标不能直接复用 PhraseGrid 内部 translation：
    // 用“触摸根坐标 - 触摸在按钮内的偏移”计算 top-start，才能保证副本严格跟随触摸点。
    // FIXME: 少数短语按钮拖动时仍可能出现轻微“不跟手”；优先检查这里的根坐标与局部触点偏移换算。
    val touchOffsetInOverlay = dragInfo.touchOffsetInButton * overlayScale
    val overlayTopStart = dragInfo.pointerPositionInRoot - touchOffsetInOverlay

    // 拖拽副本必须绘制在删除区之后，由调用方决定层级；这里只负责外观和位置。
    Surface(
        modifier = modifier
            .offset {
                IntOffset(
                    overlayTopStart.x.roundToInt(),
                    overlayTopStart.y.roundToInt()
                )
            }
            .size(overlayWidth, overlayHeight)
            .graphicsLayer {
                shadowElevation = 16f
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dragInfo.phrase.label,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
