// 主界面短语网格渲染；此文件刻意不包含拖拽行为。
package com.ziegler.kighelper.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics

/**
 * 为主 AAC 界面渲染分组短语按钮和分组标题。
 *
 * @param groupedSections 已按分组整理的短语列表，分组顺序由外部控制。
 * @param state 网格滚动状态，供外部控制和分组标签滚动定位使用。
 * @param columns 网格列配置，供不同屏幕尺寸适配使用。
 * @param onPhraseClick 点击短语的回调，提供被点击的短语。
 * @param onPhraseLongClick 长按短语的回调，提供被长按的短语。
 * @param onPhraseDelete 删除短语的回调，提供被删除的短语。
 * @param onDisplayShouldExpand 请求主界面展开以显示短语内容的回调，供短语按钮点击时调用。
 * @param modifier 可选的修饰符，供外部布局使用。
 * @param cardFontSize 短语按钮文本的字体大小，供不同屏幕尺寸适配使用。
 * @param cardHeight 短语按钮的高度，供不同屏幕尺寸适配使用。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhraseGrid(
    groupedSections: List<Pair<PhraseGroup, List<Phrase>>>,
    state: LazyGridState,
    columns: GridCells,
    onPhraseClick: (Phrase) -> Unit,
    onPhraseLongClick: (Phrase) -> Unit,
    onPhraseDelete: (Phrase) -> Unit,
    onDisplayShouldExpand: () -> Unit,
    modifier: Modifier = Modifier,
    cardFontSize: TextUnit = 18.sp,
    cardHeight: Dp = 80.dp,
    hapticFeedback: Boolean = true
) {
    LazyVerticalGrid(
        columns = columns,
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedSections.forEach { (group, groupPhrases) ->
            // 只有存在多个有效分组时才显示分组标题。
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
                PhraseButton(
                    phrase = phrase,
                    onPhraseClick = onPhraseClick,
                    onPhraseLongClick = onPhraseLongClick,
                    onPhraseDelete = onPhraseDelete,
                    onDisplayShouldExpand = onDisplayShouldExpand,
                    cardFontSize = cardFontSize,
                    cardHeight = cardHeight,
                    hapticFeedback = hapticFeedback
                )
            }
        }
    }
}

/**
 * 渲染单个短语按钮，支持点击和长按交互，长按会显示上下文菜单。
 *
 * @param phrase 当前按钮对应的短语数据。
 * @param onPhraseClick 点击按钮的回调，提供当前短语。
 * @param onPhraseLongClick 长按按钮的回调，提供当前短语。
 * @param onPhraseDelete 删除短语的回调，提供当前短语。
 * @param onDisplayShouldExpand 请求主界面展开以显示短语内容的回调，供按钮点击时调用。
 * @param cardFontSize 按钮文本的字体大小，供不同屏幕尺寸适配使用。
 * @param cardHeight 按钮的高度，供不同屏幕尺寸适配使用。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhraseButton(
    phrase: Phrase,
    onPhraseClick: (Phrase) -> Unit,
    onPhraseLongClick: (Phrase) -> Unit,
    onPhraseDelete: (Phrase) -> Unit,
    onDisplayShouldExpand: () -> Unit,
    cardFontSize: TextUnit,
    cardHeight: Dp,
    hapticFeedback: Boolean = true
) {
    val performButtonHaptic = rememberPhysicalButtonHaptics(enabled = hapticFeedback)
    val buttonShape = MaterialTheme.shapes.large
    val density = LocalDensity.current
    var isMenuExpanded by remember(phrase.id) { mutableStateOf(false) }
    var buttonSize by remember(phrase.id) { mutableStateOf(IntSize.Zero) }
    val menuMinWidth = with(density) { buttonSize.width.toDp() }
    val menuGapPx = with(density) { PhraseContextMenuGap.roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .onSizeChanged { buttonSize = it }) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(buttonShape)
                .combinedClickable(role = Role.Button, onClick = {
                    performButtonHaptic()
                    onDisplayShouldExpand()
                    onPhraseClick(phrase)
                }, onLongClick = {
                    performButtonHaptic()
                    isMenuExpanded = true
                }),
            shape = buttonShape,
            color = phrase.cardColor?.let { Color(it.toInt()) }
                ?: MaterialTheme.colorScheme.secondaryContainer,
            contentColor = phrase.cardColor?.let {
                val luminance = (0.299 * ((it shr 16) and 0xFF) +
                        0.587 * ((it shr 8) and 0xFF) +
                        0.114 * (it and 0xFF)) / 255
                if (luminance > 0.5) Color.Black else Color.White
            } ?: MaterialTheme.colorScheme.onSecondaryContainer
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

        if (isMenuExpanded) {
            PhraseContextMenu(
                minWidth = menuMinWidth,
                gapPx = menuGapPx,
                onDismiss = { isMenuExpanded = false },
                onEdit = {
                    isMenuExpanded = false
                    onPhraseLongClick(phrase)
                },
                onDelete = {
                    isMenuExpanded = false
                    onPhraseDelete(phrase)
                })
        }
    }
}

/**
 * 渲染短语按钮的上下文菜单，包含编辑和删除选项，菜单位置根据按钮位置智能调整。
 *
 * @param minWidth 菜单的最小宽度，通常设置为按钮宽度以保持视觉对齐。
 * @param gapPx 菜单与按钮之间的间距，单位为像素，用于计算菜单位置。
 * @param onDismiss 关闭菜单的回调，通常在点击菜单项或外部区域时调用。
 * @param onEdit 编辑短语的回调，供编辑菜单项使用。
 * @param onDelete 删除短语的回调，供删除菜单项使用。
 */
@Composable
private fun PhraseContextMenu(
    minWidth: Dp, gapPx: Int, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val positionProvider = remember(gapPx) {
        PhraseContextMenuPositionProvider(gapPx)
    }

    // 菜单项目前是静态集合；如需新增项目，先在这里增加回调参数，再在 Column 中按样式添加一行。
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = minWidth),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                PhraseContextMenuItem(
                    text = "编辑", itemIcon = Icons.Default.Edit, onClick = onEdit
                )

                PhraseContextMenuItem(
                    text = "删除",
                    itemIcon = Icons.Default.Delete,
                    onClick = onDelete,
                    style = PCMI_STYLE_DELETE
                )/*
                * 手动增加短语菜单项时需要完成三步：
                * 1. 在 PhraseContextMenu 的参数列表中新增对应回调，并在 PhraseButton 调用处把当前 phrase 传给业务层；
                * 2. 在 PhraseContextMenu 的 Column 中新增一个 PhraseContextMenuItem；如果需要新样式，先新增 PCMI_STYLE_ 常量，
                *    再在 PhraseContextMenuItem 的 when 分支里集中覆盖该样式需要修改的视觉属性；
                * 3. 点击菜单项前先关闭 isMenuExpanded，避免业务弹窗、删除刷新列表后，旧 Popup 仍挂在已移除的按钮锚点上。
                */
            }
        }
    }
}

/**
 * 渲染短语上下文菜单的单个菜单项，支持不同风格（如删除项）以区分视觉层级。
 *
 * @param text 菜单项文本，如“编辑”或“删除”。
 * @param itemIcon 菜单项图标，如编辑或删除图标，默认为 null 表示无图标。
 * @param onClick 点击菜单项的回调，通常会调用业务逻辑处理对应操作。
 * @param style 菜单项风格标识，默认为 PCMI_STYLE_DEFAULT；特殊风格如 PCMI_STYLE_DELETE 会覆盖默认视觉属性以突出显示。
 */
@Composable
private fun PhraseContextMenuItem(
    text: String,
    itemIcon: ImageVector? = null,
    onClick: () -> Unit,
    style: String = PCMI_STYLE_DEFAULT
) {
    var backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    var contentColor = MaterialTheme.colorScheme.onSurface
    var itemModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp, vertical = 4.dp)
    var itemShape = MaterialTheme.shapes.extraSmall

    // 每种菜单项风格只在一个分支里集中覆盖默认视觉属性，避免多个属性各自分散判断。
    when (style) {
        PCMI_STYLE_DELETE -> {
            backgroundColor = MaterialTheme.colorScheme.error
            contentColor = MaterialTheme.colorScheme.onError
            itemShape = MaterialTheme.shapes.small
        }
    }

    Surface(
        onClick = onClick,
        modifier = itemModifier,
        shape = itemShape,
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp, vertical = 12.dp
            ), verticalAlignment = Alignment.CenterVertically
        ) {
            if (itemIcon != null) {
                Icon(
                    imageVector = itemIcon, contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text, fontWeight = FontWeight.Bold, maxLines = 1
            )
        }
    }
}

/**
 * 将短语菜单锚定到按钮右上方；如果上方空间不足，则退回到按钮右下方，避免菜单被屏幕裁掉。
 */
private class PhraseContextMenuPositionProvider(
    private val gapPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = maxOf(0, windowSize.width - popupContentSize.width)
        val maxY = maxOf(0, windowSize.height - popupContentSize.height)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val topY = anchorBounds.top - popupContentSize.height - gapPx
        val fallbackY = anchorBounds.bottom + gapPx
        val y = if (topY >= 0) {
            topY
        } else {
            fallbackY.coerceAtMost(maxY)
        }.coerceAtLeast(0)

        return IntOffset(x, y)
    }
}

private val PhraseContextMenuGap = 8.dp
private const val PCMI_STYLE_DEFAULT = "default"
private const val PCMI_STYLE_DELETE = "delete"
