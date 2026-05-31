// 当前选中或输入短语文本的大字展示区。
package com.ziegler.kighelper.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 展示区布局策略：普通横竖屏分别控制换行倾向，全屏模式允许更充分地使用空间。
 */
internal enum class DisplaySurfaceLayoutMode {
    Portrait,
    Landscape,
    Fullscreen
}

/**
 * 以响应式字号显示当前 AAC 文本，并提供清除按钮。
 */
@Composable
internal fun DisplaySurface(
    text: String,
    isSubtle: Boolean,
    scrollState: ScrollState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    layoutMode: DisplaySurfaceLayoutMode = DisplaySurfaceLayoutMode.Portrait,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            val displayTextStyle = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold
            )
            val contentPadding = layoutMode.contentPadding

            AnimatedContent(
                targetState = text,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "textAnimation"
            ) { targetText ->
                val baseFontSize = rememberDisplayFontSize(
                    text = targetText,
                    containerWidth = maxWidth - contentPadding * 2,
                    containerHeight = maxHeight - contentPadding * 2,
                    baseStyle = displayTextStyle,
                    layoutMode = layoutMode
                )
                val lineHeight = baseFontSize * DisplayLineHeightMultiplier

                val currentTextIsHint = targetText == "点击下面按钮文字在此显示" ||
                        targetText == "先添加一个常用短语吧" ||
                        (targetText == text && isSubtle)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .verticalScroll(scrollState),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = targetText,
                        style = displayTextStyle.copy(
                            fontSize = baseFontSize,
                            lineHeight = lineHeight
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary.copy(
                            alpha = if (currentTextIsHint) 0.55f else 1f
                        )
                    )
                }
            }

            if (text.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            CircleShape
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

/**
 * 通过 TextMeasurer 实测文本布局，并用二分搜索找到不溢出且换行合理的最大字号。
 */
@Composable
private fun rememberDisplayFontSize(
    text: String,
    containerWidth: Dp,
    containerHeight: Dp,
    baseStyle: TextStyle,
    layoutMode: DisplaySurfaceLayoutMode
): TextUnit {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val minFontSize = layoutMode.minFontSize
    val maxFontSize = layoutMode.maxFontSize

    return remember(
        text,
        containerWidth,
        containerHeight,
        baseStyle,
        layoutMode,
        minFontSize,
        maxFontSize,
        density
    ) {
        if (text.isBlank()) {
            maxFontSize
        } else {
            val maxWidthPx = with(density) { containerWidth.roundToPx().coerceAtLeast(1) }
            val maxHeightPx = with(density) { containerHeight.roundToPx().coerceAtLeast(1) }
            val layoutRules = DisplayTextLayoutRules.from(text, layoutMode)
            var low = minFontSize.value
            var high = maxFontSize.value

            repeat(DisplayFontSearchIterations) {
                val candidate = (low + high) / 2f
                val candidateSize = candidate.sp
                val layoutResult = textMeasurer.measure(
                    text = text,
                    style = baseStyle.copy(
                        fontSize = candidateSize,
                        lineHeight = candidateSize * DisplayLineHeightMultiplier
                    ),
                    constraints = Constraints(maxWidth = maxWidthPx)
                )

                if (
                    layoutResult.size.width <= maxWidthPx &&
                    layoutResult.size.height <= maxHeightPx &&
                    layoutResult.lineCount <= layoutRules.maxLineCount
                ) {
                    low = candidate
                } else {
                    high = candidate
                }
            }

            low.sp
        }
    }
}

/**
 * 根据文本长度和展示模式给出行数上限，避免展示区为了填满高度而一两个字一行。
 */
private data class DisplayTextLayoutRules(
    val maxLineCount: Int
) {
    companion object {
        fun from(text: String, layoutMode: DisplaySurfaceLayoutMode): DisplayTextLayoutRules {
            val contentLength = text.count { !it.isWhitespace() }
            val naturalMaxLineCount = when (layoutMode) {
                DisplaySurfaceLayoutMode.Portrait -> portraitMaxLineCount(contentLength)
                DisplaySurfaceLayoutMode.Landscape -> landscapeMaxLineCount(contentLength)
                DisplaySurfaceLayoutMode.Fullscreen -> fullscreenMaxLineCount(contentLength)
            }
            val explicitLineCount = text.lineSequence().count().coerceAtLeast(1)

            return DisplayTextLayoutRules(
                maxLineCount = maxOf(naturalMaxLineCount, explicitLineCount)
            )
        }

        private fun portraitMaxLineCount(contentLength: Int): Int = when {
            contentLength <= 8 -> 1
            contentLength <= 18 -> 2
            contentLength <= 32 -> 3
            else -> 4
        }

        private fun landscapeMaxLineCount(contentLength: Int): Int = when {
            contentLength <= 10 -> 1
            contentLength <= 28 -> 2
            contentLength <= 48 -> 3
            else -> 4
        }

        private fun fullscreenMaxLineCount(contentLength: Int): Int = when {
            contentLength <= 6 -> 1
            contentLength <= 16 -> 2
            contentLength <= 32 -> 3
            contentLength <= 56 -> 4
            else -> 6
        }
    }
}

private val DisplaySurfaceLayoutMode.contentPadding: Dp
    get() = when (this) {
        DisplaySurfaceLayoutMode.Portrait -> 20.dp
        DisplaySurfaceLayoutMode.Landscape -> 16.dp
        DisplaySurfaceLayoutMode.Fullscreen -> 24.dp
    }

private val DisplaySurfaceLayoutMode.minFontSize: TextUnit
    get() = when (this) {
        DisplaySurfaceLayoutMode.Landscape -> 20.sp
        else -> 24.sp
    }

private val DisplaySurfaceLayoutMode.maxFontSize: TextUnit
    get() = when (this) {
        DisplaySurfaceLayoutMode.Fullscreen -> 124.sp
        else -> 110.sp
    }

private const val DisplayLineHeightMultiplier = 1.15f
private const val DisplayFontSearchIterations = 8
