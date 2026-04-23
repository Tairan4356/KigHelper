package com.ziegler.kighelper.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun InputScreen(onSpeak: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 文字展示区 ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = text,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.9f)).togetherWith(
                        fadeOut() + scaleOut(
                            targetScale = 0.9f
                        )
                    )
                },
                label = "inputTextColorAnimation"
            ) { targetText ->
                Text(
                    text = targetText.ifEmpty { "请输入文字" },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 52.sp,
                        letterSpacing = 0.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = if (targetText.isEmpty())
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // --- 清空按钮 ---
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = { text = "" },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                        contentDescription = "清空内容",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // --- 底部输入区 ---
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("在此输入内容...") },
                    maxLines = 3,
                    shape = MaterialTheme.shapes.large
                )

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { if (text.isNotBlank()) onSpeak(text) },
                    modifier = Modifier
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("朗读", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}