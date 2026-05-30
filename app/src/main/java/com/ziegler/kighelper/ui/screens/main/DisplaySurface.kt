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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 以响应式字号显示当前 AAC 文本，并提供清除按钮。
 */
@Composable
internal fun DisplaySurface(
    text: String,
    isSubtle: Boolean,
    scrollState: ScrollState,
    onClick: (() -> Unit)?,
    onClear: () -> Unit,
    smallestScreenWidth: Int,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(clickModifier),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            val baseFontSize = when {
                smallestScreenWidth < 360 -> {
                    if (text.length > 20) 32.sp else 56.sp
                }

                smallestScreenWidth < 600 -> {
                    if (text.length > 20) 56.sp else 72.sp
                }

                smallestScreenWidth < 720 -> {
                    if (text.length > 20) 72.sp else 84.sp
                }

                else -> {
                    if (text.length > 20) 80.sp else 110.sp
                }
            }
            val lineHeight = baseFontSize * 1.15f

            AnimatedContent(
                targetState = text, transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                }, label = "textAnimation"
            ) { targetText ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = baseFontSize,
                            lineHeight = lineHeight
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary.copy(
                            alpha = if (isSubtle) 0.55f else 1f
                        )
                    )
                }
            }

            if (text.isNotEmpty()) {
                IconButton(
                    onClick = onClear, modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape
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