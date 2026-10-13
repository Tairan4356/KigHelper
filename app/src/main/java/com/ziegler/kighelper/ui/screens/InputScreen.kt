// 自由输入界面编排：常用字词面板优先、可切换输入法输入、插入文字并触发朗读。
package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.data.InputWord
import com.ziegler.kighelper.data.InputWordGroup
import com.ziegler.kighelper.ui.screens.input.WordPanel
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics
import kotlin.math.roundToInt

/**
 * 自由输入界面：常用字词面板优先展示，支持切换输入法手动输入并朗读
 *
 * @param modifier 外部传入的修饰符
 * @param contentPadding 来自父布局的内边距
 * @param onSpeak 触发朗读的回调，传入当前文本
 * @param onStop 触发停止朗读的回调
 * @param words 全部常用字词
 * @param groups 全部字词分组
 * @param autoPopupMode 进入页面时自动弹起的模式（常用字词 / 输入法键盘）
 * @param onNavigateToManage 跳转到常用字词管理页的回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSpeak: (String) -> Unit,
    onStop: () -> Unit,
    fontSizeMultiplier: Float = 1.0f,
    words: List<InputWord> = emptyList(),
    groups: List<InputWordGroup> = emptyList(),
    autoPopupMode: Int = AppSettings.INPUT_AUTO_POPUP_WORDS,
    onNavigateToManage: () -> Unit = {}
) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val text = textFieldValue.text
    val performButtonHaptic = rememberPhysicalButtonHaptics()

    // 面板展开状态：进入页面按设置自动弹起一次，之后手动收起/展开
    var panelExpanded by rememberSaveable {
        mutableStateOf(autoPopupMode == AppSettings.INPUT_AUTO_POPUP_WORDS)
    }
    // 输入法是否由用户显式请求弹出（仅 ⌨ 按钮）：
    // 字词面板模式下点按文本区只调光标、不自动弹起输入法
    var imeRequestedByUser by remember { mutableStateOf(false) }
    // 字词面板模式下文本区为只读：禁止 IME 弹出（readOnly 不会拉起软键盘），
    // 光标定位/选择仍可用，文字由面板（插入/退格）与程序写入驱动
    val imeSuppressed =
        autoPopupMode == AppSettings.INPUT_AUTO_POPUP_WORDS && !imeRequestedByUser
    // 字词分组选中标点组外的第一个分组
    val normalGroups = remember(groups) {
        groups.filter { it.id != InputWordGroup.PUNCTUATION_ID }
    }
    var selectedGroupId by rememberSaveable { mutableStateOf(InputWordGroup.DEFAULT_ID) }
    LaunchedEffect(normalGroups) {
        if (normalGroups.isNotEmpty() && normalGroups.none { it.id == selectedGroupId }) {
            selectedGroupId = normalGroups.first().id
        }
    }

    // 焦点状态：用于驱动占位符颜色与文本缩放强调动画
    var isFocused by remember { mutableStateOf(false) }

    // 已渲染文本的布局结果与当前光标偏移：只读模式下用于叠加自定义光标
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // 只读模式下自行接管指针：记录文字容器与指针覆盖层的坐标，用于把触点换算为文本偏移
    var textContainerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pointerOverlayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val caretOffset = if (textFieldValue.selection.collapsed) {
        textFieldValue.selection.start
    } else {
        textFieldValue.selection.end
    }

    // MD3 Expressive：占位符在焦点变化时平滑过渡透明度与颜色
    val placeholderColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outline.copy(
            alpha = if (isFocused) 0.65f else 0.5f
        ), animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium
        ), label = "placeholderColor"
    )
    // 输入文本颜色：随焦点状态平滑过渡，聚焦时更醒目
    val inputTextColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium
        ), label = "inputTextColor"
    )
    // MD3 Expressive：聚焦时文本轻微放大强调
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow
        ), label = "focusScale"
    )
    // 输入/删除文字后，整体从对应偏移端平滑滑动至居中：
    // 插入 → 从右侧偏移滑向左归位；删除 → 从左侧偏移滑向右归位
    val slideOffsetX = remember { Animatable(0f) }
    var previousLength by remember { mutableIntStateOf(0) }

    LaunchedEffect(text) {
        val newLength = text.length
        val delta = newLength - previousLength
        previousLength = newLength
        if (delta != 0) {
            val startOffset = if (delta > 0) 24.dp else (-24).dp
            slideOffsetX.snapTo(with(density) { startOffset.toPx() })
            slideOffsetX.animateTo(
                targetValue = 0f, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    // 输入法可见时拦截返回键，先收起输入法而不是直接返回上一级
    val isImeVisible = WindowInsets.isImeVisible
    BackHandler(enabled = isImeVisible) {
        keyboardController?.hide()
    }

    val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val navigationStartPadding = with(density) {
        WindowInsets.navigationBars.getLeft(this, layoutDirection).toDp()
    }
    val navigationEndPadding = with(density) {
        WindowInsets.navigationBars.getRight(this, layoutDirection).toDp()
    }
    val safeStartPadding = maxOf(
        contentPadding.calculateStartPadding(layoutDirection), navigationStartPadding
    )
    val safeEndPadding = maxOf(
        contentPadding.calculateEndPadding(layoutDirection), navigationEndPadding
    )

    val actionBottomPadding = maxOf(imeBottomPadding, contentPadding.calculateBottomPadding())

    // 进入页面按设置自动弹起字词面板或输入法
    LaunchedEffect(autoPopupMode) {
        if (autoPopupMode == AppSettings.INPUT_AUTO_POPUP_WORDS) {
            panelExpanded = true
        } else {
            panelExpanded = false
            imeRequestedByUser = true
        }
    }

    // 用户显式请求输入法时才弹起：先随状态变更加载出可编辑字段，再聚焦并显示软键盘
    LaunchedEffect(imeRequestedByUser) {
        if (imeRequestedByUser) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // 字词面板模式下持续压制输入法自动弹起：仅用户点按 ⌨ 才保持可见；
    // 输入法收起后复位标记，保证下一次点按文本区仍不弹输入法。
    LaunchedEffect(isImeVisible) {
        if (autoPopupMode == AppSettings.INPUT_AUTO_POPUP_WORDS) {
            if (isImeVisible && !imeRequestedByUser) {
                keyboardController?.hide()
            } else if (!isImeVisible) {
                imeRequestedByUser = false
            }
        }
    }

    // 面板实际可见状态：手动展开且输入法未弹起（输入法优先，收起后自动恢复）
    val showPanel = panelExpanded && !isImeVisible

    // 底部栈高度：驱动巨型文本区底部留白，避免被面板遮挡
    var bottomStackHeight by remember { mutableStateOf(0.dp) }
    val textBottomPadding by animateDpAsState(
        targetValue = 20.dp + bottomStackHeight + maxOf(
            0.dp, actionBottomPadding - imeBottomPadding
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium
        ), label = "textBottomPadding"
    )

    val smallestScreenWidth = configuration.smallestScreenWidthDp
    // 动态计算缩放系数与字号目标值（按文本长度跨越 20 字符阈值切换）
    val targetFontSizeSp = when {
        smallestScreenWidth < 360 -> {
            if (text.length > 20) 32f else 56f
        }

        smallestScreenWidth < 600 -> {
            if (text.length > 20) 56f else 72f
        }

        smallestScreenWidth < 720 -> {
            if (text.length > 20) 72f else 84f
        }

        else -> {
            if (text.length > 20) 80f else 110f
        }
    }
    // MD3 Expressive：字号的跨档位切换使用低阻尼回弹弹簧动画，产生自然柔和的放大/收缩
    val fontSize by animateFloatAsState(
        targetValue = targetFontSizeSp * fontSizeMultiplier, animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium
        ), label = "inputFontSize"
    )
    val lineHeight = fontSize * 1.15f

    // 点击字词插入文本：聚焦时插入光标处，未聚焦时追加到末尾
    fun insertText(textToInsert: String) {
        performButtonHaptic()
        val current = textFieldValue
        val anchor = (if (isFocused && current.selection.collapsed) {
            current.selection.start
        } else {
            current.text.length
        }).coerceIn(0, current.text.length)
        val newText =
            current.text.substring(0, anchor) + textToInsert + current.text.substring(anchor)
        textFieldValue = TextFieldValue(
            text = newText, selection = TextRange(anchor + textToInsert.length)
        )
    }

    // 退格：有选区删除选区，否则删除光标前一个字符（正确处理代理对/emoji）
    fun deleteBeforeCursor() {
        performButtonHaptic()
        val current = textFieldValue
        val selection = current.selection
        if (!selection.collapsed) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)
            textFieldValue = TextFieldValue(
                text = current.text.substring(0, start) + current.text.substring(end),
                selection = TextRange(start)
            )
            return
        }
        val cursor = selection.start.coerceIn(0, current.text.length)
        if (cursor == 0) return
        var offset = cursor - 1
        if (offset > 0) {
            val codeUnit = current.text[offset]
            if (Character.isLowSurrogate(codeUnit) &&
                Character.isHighSurrogate(current.text[offset - 1])
            ) {
                offset--
            }
        }
        textFieldValue = TextFieldValue(
            text = current.text.substring(0, offset) + current.text.substring(cursor),
            selection = TextRange(offset)
        )
    }

    // 当前分组字词与底栏标点
    val groupWords = words.filter { it.groupId == selectedGroupId }.sortedBy { it.order }
    val punctuationWords = remember(words) {
        words.filter { it.groupId == InputWordGroup.PUNCTUATION_ID }
            .sortedBy { it.order }
            .take(InputWordGroup.PUNCTUATION_MAX)
    }

    // 只读模式下手动定位光标：把指针位置换算到文本布局坐标并取最近字符偏移
    fun moveCursorToPointer(pointerPosition: Offset) {
        val layout = textLayoutResult ?: return
        val overlay = pointerOverlayCoords ?: return
        val textNode = textContainerCoords ?: return
        val current = textFieldValue
        val layoutText = layout.layoutInput.text.text
        val displayedText = current.text.ifEmpty { "请输入文字" }
        // 文本变化过渡期布局可能与当前文字不一致，此时忽略
        if (layoutText != displayedText) return
        val textLocal = textNode.localPositionOf(overlay, pointerPosition)
        val offset = layout.getOffsetForPosition(textLocal).coerceIn(0, current.text.length)
        val newSelection = TextRange(offset)
        if (current.selection != newSelection) {
            textFieldValue = current.copy(selection = newSelection)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            readOnly = imeSuppressed,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused
                    // 只读模式兜底：输入法若仍被拉起（用户未请求），立即压制
                    if (it.isFocused && imeSuppressed) {
                        keyboardController?.hide()
                    }
                },
            textStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = fontSize.sp,
                lineHeight = lineHeight.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                color = Color.Transparent
            ),
            cursorBrush = SolidColor(inputTextColor),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                start = safeStartPadding + if (isLandscape) 32.dp else 16.dp,
                                top = if (isLandscape) 24.dp else 32.dp,
                                end = safeEndPadding + if (isLandscape) 32.dp else 16.dp,
                                bottom = maxOf(
                                    textBottomPadding,
                                    if (isLandscape) 24.dp else 32.dp
                                )
                            ), contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { textContainerCoords = it }
                                    .graphicsLayer {
                                        translationX = slideOffsetX.value
                                        scaleX = focusScale
                                        scaleY = focusScale
                                    }, contentAlignment = Alignment.Center
                            ) {
                            // 旧文字淡出 + 缩小，新文字淡入 + 从 0.9 放大
                            AnimatedContent(
                                targetState = text, transitionSpec = {
                                    (fadeIn() + scaleIn(initialScale = 0.9f)).togetherWith(
                                        fadeOut() + scaleOut(
                                            targetScale = 0.9f
                                        )
                                    )
                                }, label = "inputTextColorAnimation"
                            ) { targetText ->
                                Text(
                                    text = targetText.ifEmpty { "请输入文字" },
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = fontSize.sp,
                                        lineHeight = lineHeight.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = if (targetText.isEmpty()) placeholderColor
                                    else inputTextColor,
                                    onTextLayout = { layout ->
                                        // 仅记录当前（非过渡中的旧文字）文本布局
                                        if (targetText == text) {
                                            textLayoutResult = layout
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // 编辑层：文字透明以隐藏重复内容，光标仍可见并与输入位置同步
                            innerTextField()
                            // 只读（禁止输入法）模式下系统不绘制光标，叠加一个闪烁的自定义光标
                            if (imeSuppressed && isFocused) {
                                textLayoutResult?.let { layout ->
                                    val layoutText = layout.layoutInput.text.text
                                    val displayedText = text.ifEmpty { "请输入文字" }
                                    // 仅在布局与当前显示文字一致时绘制，避免文本变化过渡期用到过期布局
                                    if (layoutText == displayedText) {
                                        val offset = caretOffset.coerceIn(0, layoutText.length)
                                        CaretIndicator(
                                            rect = layout.getCursorRect(offset),
                                            color = inputTextColor,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )
                                    }
                                }
                            }
                        }
                        if (text.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(if (isLandscape) 64.dp else 80.dp))
                        }
                    }
                }

                    // 只读模式接管指针：自行定位光标，规避系统放大镜与选择手柄
                    if (imeSuppressed) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { pointerOverlayCoords = it }
                                .pointerInput(imeSuppressed) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        focusRequester.requestFocus()
                                        moveCursorToPointer(down.position)
                                        down.consume()
                                        var pointer = down.position
                                        var isDragging = false
                                        val touchSlop = viewConfiguration.touchSlop
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                                ?: break
                                            if (!change.pressed) break
                                            val delta = change.position - pointer
                                            if (!isDragging && delta.getDistance() > touchSlop) {
                                                isDragging = true
                                            }
                                            if (isDragging) {
                                                scrollState.dispatchRawDelta(-delta.y)
                                            } else {
                                                moveCursorToPointer(change.position)
                                            }
                                            pointer = change.position
                                            change.consume()
                                        }
                                    }
                                }
                        )
                    }
                }
            })

        // 底部栈：左侧展开/收起面板切换 + 右侧清空/朗读 + 字词面板，输入法弹起时面板让位
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = safeStartPadding + 12.dp,
                    end = safeEndPadding + 12.dp,
                    bottom = actionBottomPadding + 12.dp
                )
                .onSizeChanged { size ->
                    bottomStackHeight = with(density) { size.height.toDp() }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧切换按钮：面板展开时显示收起，收起时显示展开；输入法弹起时不显示
                AnimatedVisibility(
                    visible = !isImeVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FilledTonalIconButton(onClick = {
                        performButtonHaptic()
                        panelExpanded = !panelExpanded
                    }) {
                        Icon(
                            imageVector = if (panelExpanded) Icons.Filled.KeyboardArrowDown
                            else Icons.Filled.KeyboardArrowUp,
                            contentDescription = if (panelExpanded) "收起字词面板"
                            else "展开常用字词"
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                textFieldValue = TextFieldValue("")
                                onStop()
                            }) {
                            Icon(
                                imageVector = Icons.Default.Clear, contentDescription = "清空内容"
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                performButtonHaptic()
                                onSpeak(text)
                            }
                        }, enabled = text.isNotBlank(), shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("朗读", fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(
                visible = showPanel,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { it / 2 } + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { it / 2 } + fadeOut(),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                WordPanel(
                    groups = normalGroups,
                    words = groupWords,
                    punctuationWords = punctuationWords,
                    selectedGroupId = selectedGroupId,
                    onGroupSelect = { selectedGroupId = it },
                    onWordClick = ::insertText,
                    onPunctuationClick = ::insertText,
                    onKeyboardClick = {
                        performButtonHaptic()
                        imeRequestedByUser = true
                    },
                    onBackspaceClick = ::deleteBeforeCursor,
                    onManageClick = {
                        performButtonHaptic()
                        onNavigateToManage()
                    }
                )
            }
        }
    }
}

/**
 * 自定义文本光标：只读（禁止输入法）模式下系统不绘制光标，
 * 依据文本布局在光标位置叠加一条闪烁的竖线。
 *
 * @param rect 光标在文本布局坐标系中的矩形（像素）
 * @param color 光标颜色
 */
@Composable
private fun CaretIndicator(
    rect: Rect,
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val blink by rememberInfiniteTransition(label = "caretBlink").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretBlinkAlpha"
    )
    Box(
        modifier = modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .width(2.5.dp)
            .height(with(density) { rect.height.toDp() })
            .graphicsLayer { alpha = blink }
            .background(color = color, shape = RoundedCornerShape(1.25.dp))
    )
}
