package com.ziegler.kighelper.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.ui.utils.rememberPhysicalButtonHaptics

/**
 * 自由输入界面：允许用户手动输入文字并朗读
 */
@Composable
fun InputScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSpeak: (String) -> Unit,
    onStop: () -> Unit
) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val text = textFieldValue.text
    val performButtonHaptic = rememberPhysicalButtonHaptics()
    val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val navigationStartPadding = with(density) {
        WindowInsets.navigationBars.getLeft(this, layoutDirection).toDp()
    }
    val navigationEndPadding = with(density) {
        WindowInsets.navigationBars.getRight(this, layoutDirection).toDp()
    }
    val safeStartPadding = maxOf(
        contentPadding.calculateStartPadding(layoutDirection),
        navigationStartPadding
    )
    val safeEndPadding = maxOf(
        contentPadding.calculateEndPadding(layoutDirection),
        navigationEndPadding
    )
    var navigationBottomPadding by remember { mutableStateOf(0.dp) }
    val isImeVisible = imeBottomPadding > 0.dp

    val fontSize = when {
        text.length > 60 -> if (isLandscape) 28.sp else 32.sp
        text.length > 20 -> if (isLandscape) 34.sp else 40.sp
        isLandscape -> 40.sp
        else -> 48.sp
    }
    val lineHeight = when {
        text.length > 60 -> if (isLandscape) 32.sp else 36.sp
        text.length > 20 -> if (isLandscape) 38.sp else 44.sp
        isLandscape -> 44.sp
        else -> 52.sp
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(contentPadding, isImeVisible) {
        if (!isImeVisible) {
            navigationBottomPadding = contentPadding.calculateBottomPadding()
        }
    }

    val actionBottomPadding = if (imeBottomPadding > navigationBottomPadding) {
        imeBottomPadding
    } else {
        navigationBottomPadding
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = lineHeight,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
                    if (text.isEmpty()) {
                        Text(
                            text = "请输入文字",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = fontSize,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = lineHeight,
                                letterSpacing = 0.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField()
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
                },
                enabled = text.isNotBlank(),
                shape = MaterialTheme.shapes.medium
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
