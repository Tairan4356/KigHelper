// 自由输入界面编排：手动输入文字、适配输入法空间并触发朗读。
package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics

/**
 * 自由输入界面：允许用户手动输入文字并朗读
 *
 * @param modifier 外部传入的修饰符
 * @param contentPadding 来自父布局的内边距
 * @param onSpeak 触发朗读的回调，传入当前文本
 * @param onStop 触发停止朗读的回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSpeak: (String) -> Unit,
    onStop: () -> Unit,
    fontSizeMultiplier: Float = 1.0f
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

    // 焦点状态：用于驱动占位符颜色与文本缩放强调动画
    var isFocused by remember { mutableStateOf(false) }

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
            val startOffset = if (delta > 0) 24.dp else -24.dp
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = fontSize.sp,
                lineHeight = lineHeight.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                color = Color.Transparent
            ),
            cursorBrush = SolidColor(inputTextColor),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(
                            start = safeStartPadding + if (isLandscape) 32.dp else 16.dp,
                            top = if (isLandscape) 24.dp else 32.dp,
                            end = safeEndPadding + if (isLandscape) 32.dp else 16.dp,
                            bottom = if (isLandscape) 24.dp else 32.dp
                        ), contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // 编辑层：文字透明以隐藏重复内容，光标仍可见并与输入位置同步
                            innerTextField()
                        }
                        if (text.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(if (isLandscape) 64.dp else 80.dp))
                        }
                    }
                }
            })

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    start = safeStartPadding + 16.dp,
                    top = 16.dp,
                    end = safeEndPadding + 16.dp,
                    bottom = actionBottomPadding + 16.dp
                ),
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
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("朗读", fontWeight = FontWeight.Bold)
            }
        }
    }
}
