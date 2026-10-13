// 常用字词面板：分组切换、三行自适应字词网格与键盘/标点/空格底栏。
package com.ziegler.kighelper.ui.screens.input

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.InputWord
import com.ziegler.kighelper.data.InputWordGroup

/** 字词网格最多同时展示的行数，超出后该区域纵向滚动 */
private const val MAX_VISIBLE_ROWS = 3

private val KeyHeight = 44.dp
private val RowSpacing = 6.dp
private val KeyGap = 6.dp
private val KeyHorizontalPadding = 14.dp
private val PunctKeyWidth = 40.dp
private val PunctKeyPadding = 6.dp
private val PunctSegmentWidth = PunctKeyWidth * 2 + KeyGap

/**
 * 常用字词面板。
 *
 * 结构：分组 chips 行（含管理入口）→ 自适应高度的字词网格（最多 3 行，超出纵向滚动）
 * → 底栏（键盘、左侧标点、居中空格、右侧标点、退格）。
 *
 * @param groups 可切换的字词分组（不含标点组）
 * @param words 当前选中分组的字词
 * @param punctuationWords 标点组字词（最多 4 条，固定显示在底栏）
 * @param selectedGroupId 当前选中的分组 id
 * @param onGroupSelect 分组切换回调
 * @param onWordClick 点击字词回调，参数为字词内容
 * @param onPunctuationClick 点击标点回调，参数为标点内容
 * @param onKeyboardClick 弹出输入法回调
 * @param onBackspaceClick 退格回调，删除光标前一个字符
 * @param onManageClick 进入管理页回调
 */
@Composable
fun WordPanel(
    groups: List<InputWordGroup>,
    words: List<InputWord>,
    punctuationWords: List<InputWord>,
    selectedGroupId: String,
    onGroupSelect: (String) -> Unit,
    onWordClick: (String) -> Unit,
    onPunctuationClick: (String) -> Unit,
    onKeyboardClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GroupRow(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onGroupSelect = onGroupSelect,
                onManageClick = onManageClick
            )

            WordGrid(
                words = words,
                selectedGroupId = selectedGroupId,
                onWordClick = onWordClick
            )

            BottomBarRow(
                punctuationWords = punctuationWords,
                onPunctuationClick = onPunctuationClick,
                onKeyboardClick = onKeyboardClick,
                onBackspaceClick = onBackspaceClick
            )
        }
    }
}

/**
 * 分组 chips 行：横向滚动的分组筛选 + 固定在行尾的管理入口。
 */
@Composable
private fun GroupRow(
    groups: List<InputWordGroup>,
    selectedGroupId: String,
    onGroupSelect: (String) -> Unit,
    onManageClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                FilterChip(
                    selected = group.id == selectedGroupId,
                    onClick = { onGroupSelect(group.id) },
                    label = { Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        IconButton(onClick = onManageClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "管理常用字词",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 字词网格：按实测宽度贪心装填成行，行内按键拉伸对齐整行（类似实体键盘）；
 * 高度随行数自适应（最多 3 行），超过 3 行时纵向滚动。
 */
@Composable
private fun WordGrid(
    words: List<InputWord>,
    selectedGroupId: String,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "该分组暂无字词，点击 ⚙ 管理添加",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val density = LocalDensity.current
        val style = MaterialTheme.typography.labelLarge
        val textMeasurer = rememberTextMeasurer()

        // 依据实际排版宽度把字词装填成若干行，并按行拉伸铺满可用宽度
        val rows = remember(words, availableWidth, style, density) {
            val naturalWidths = words.map { word ->
                val textWidth = textMeasurer.measure(text = word.text, style = style).size.width
                (with(density) { textWidth.toDp() } + KeyHorizontalPadding * 2)
                    .coerceAtMost(availableWidth)
            }
            packWordRows(words, naturalWidths, availableWidth, KeyGap)
        }

        val visibleRows = minOf(rows.size, MAX_VISIBLE_ROWS)
        val gridHeight = KeyHeight * visibleRows + RowSpacing * (visibleRows - 1)
        val scrollState = rememberSaveable(selectedGroupId, saver = ScrollState.Saver) {
            ScrollState(0)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .then(
                    if (rows.size > MAX_VISIBLE_ROWS) Modifier.verticalScroll(scrollState)
                    else Modifier
                )
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(RowSpacing)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KeyHeight),
                    horizontalArrangement = Arrangement.spacedBy(KeyGap)
                ) {
                    row.forEach { (word, width) ->
                        WordKey(
                            text = word.text,
                            onClick = { onWordClick(word.text) },
                            modifier = Modifier.width(width)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底栏：键盘 → 左侧标点 → 居中空格 → 右侧标点 → 退格。
 * 标点段为固定宽度（左右各两个固定尺寸标点键），剩余横向空间全部由居中空格键填满。
 */
@Composable
private fun BottomBarRow(
    punctuationWords: List<InputWord>,
    onPunctuationClick: (String) -> Unit,
    onKeyboardClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leftPunctuation = punctuationWords.take(2)
    val rightPunctuation = punctuationWords.drop(2).take(2)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onKeyboardClick, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = "弹出输入法",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PunctuationSegment(
            words = leftPunctuation,
            horizontalArrangement = Arrangement.End,
            onWordClick = onPunctuationClick,
            modifier = Modifier.width(PunctSegmentWidth)
        )

        SpaceKey(onClick = { onPunctuationClick(" ") })

        PunctuationSegment(
            words = rightPunctuation,
            horizontalArrangement = Arrangement.Start,
            onWordClick = onPunctuationClick,
            modifier = Modifier.width(PunctSegmentWidth)
        )

        IconButton(onClick = onBackspaceClick, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "退格",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 底栏标点段：固定宽度，内含最多两个固定尺寸标点键，按键贴向空格一侧、相邻键之间保留键距。
 */
@Composable
private fun PunctuationSegment(
    words: List<InputWord>,
    horizontalArrangement: Arrangement.Horizontal,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        words.forEachIndexed { index, word ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(KeyGap))
            }
            WordKey(
                text = word.text,
                onClick = { onWordClick(word.text) },
                horizontalPadding = PunctKeyPadding,
                modifier = Modifier.width(PunctKeyWidth)
            )
        }
    }
}

/**
 * 居中大空格键，插入一个空格字符。
 */
@Composable
private fun RowScope.SpaceKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by keyScale(pressed)

    Surface(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .height(KeyHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "空格",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1
            )
        }
    }
}

/**
 * 单个字词按键：中等圆角、按下回弹缩放（MD3 Expressive）。
 */
@Composable
private fun WordKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = KeyHorizontalPadding
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by keyScale(pressed)

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(KeyHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 按键按下的弹簧缩放动画 */
@Composable
private fun keyScale(pressed: Boolean) = animateFloatAsState(
    targetValue = if (pressed) 0.94f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    label = "keyScale"
)

/**
 * 把字词按自然宽度贪心装填成行，随后把每行按键按比例拉伸至铺满行宽。
 * 单键行不拉伸，保留自然宽度。
 */
private fun packWordRows(
    words: List<InputWord>,
    naturalWidths: List<Dp>,
    availableWidth: Dp,
    gap: Dp
): List<List<Pair<InputWord, Dp>>> {
    if (words.isEmpty()) return emptyList()

    val rows = mutableListOf<List<Pair<InputWord, Dp>>>()
    var current = mutableListOf<Pair<InputWord, Dp>>()
    var currentNatural = 0.dp

    words.forEachIndexed { index, word ->
        val width = naturalWidths[index]
        val spacing = if (current.isEmpty()) 0.dp else gap
        if (current.isNotEmpty() && currentNatural + spacing + width > availableWidth) {
            rows.add(stretchRow(current, availableWidth, gap))
            current = mutableListOf()
            currentNatural = 0.dp
        }
        current.add(word to width)
        currentNatural += if (current.size == 1) width else gap + width
    }
    if (current.isNotEmpty()) {
        rows.add(stretchRow(current, availableWidth, gap))
    }
    return rows
}

/**
 * 把一行按键按比例拉伸到恰好铺满可用宽度（含键间距），末键吸收舍入误差。
 */
private fun stretchRow(
    keys: List<Pair<InputWord, Dp>>,
    availableWidth: Dp,
    gap: Dp
): List<Pair<InputWord, Dp>> {
    if (keys.size == 1) return keys

    val gaps = gap * (keys.size - 1)
    val naturalTotal = keys.fold(0.dp) { acc, (_, width) -> acc + width }
    if (naturalTotal.value <= 0f) return keys

    val scale = (availableWidth - gaps).value / naturalTotal.value
    var usedWidth = 0.dp
    return keys.mapIndexed { index, (word, naturalWidth) ->
        val target = if (index == keys.lastIndex) {
            availableWidth - gaps - usedWidth
        } else {
            naturalWidth * scale
        }
        usedWidth += target
        word to target
    }
}
