package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.Phrase

/**
 * 主界面：提供大字显示区域和短语快捷按钮网格。
 * @param phrases 要显示的短语列表
 * @param onPhraseClick 点击短语时的回调，通常用于触发 TTS
 * @param onClearClick 点击清除按钮时的回调
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    phrases: List<Phrase>,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
) {
    val initialHint = "点击下面按钮文字在此显示"
    var displayText by remember { mutableStateOf(initialHint) }
    var isShowingInitialHint by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    fun clearDisplayText() {
        displayText = ""
        isShowingInitialHint = false
        onClearClick()
    }

    fun showPhrase(phrase: Phrase) {
        displayText = phrase.speech
        isShowingInitialHint = false
        onPhraseClick(phrase)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(displayText) {
        scrollState.scrollTo(0)
    }

    val contentLayout = @Composable {
        if (isLandscape) {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp)
                ) {
                    DisplaySurface(
                        text = displayText,
                        isSubtle = isShowingInitialHint,
                        scrollState = scrollState,
                        onClear = { clearDisplayText() }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "点击短语显示:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    PhraseGrid(
                        phrases = phrases,
                        columns = GridCells.Fixed(2),
                        onPhraseClick = { showPhrase(it) }
                    )
                }
            }
        } else {
            Column(
                modifier = modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.weight(0.55f)) {
                    DisplaySurface(
                        text = displayText,
                        isSubtle = isShowingInitialHint,
                        scrollState = scrollState,
                        onClear = { clearDisplayText() }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "点击短语显示:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                PhraseGrid(
                    modifier = Modifier.weight(0.45f),
                    phrases = phrases,
                    columns = GridCells.Fixed(2),
                    onPhraseClick = { showPhrase(it) }
                )
            }
        }
    }

    contentLayout()
}

@Composable
fun DisplaySurface(
    text: String,
    isSubtle: Boolean,
    scrollState: ScrollState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = text,
                transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
                label = "textAnimation"
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
                            fontSize = if (targetText.length > 20) 36.sp else 48.sp,
                            lineHeight = if (targetText.length > 20) 40.sp else 52.sp
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
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.Default.Clear, "清除", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun PhraseGrid(
    phrases: List<Phrase>,
    columns: GridCells,
    onPhraseClick: (Phrase) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(phrases, key = { it.id }) { phrase ->
            Button(
                onClick = { onPhraseClick(phrase) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(phrase.label, fontSize = 18.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}
