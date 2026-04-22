package com.ziegler.kighelper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InputScreen(onSpeak: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 文字展示区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.ifEmpty { "请输入文字" },
                fontSize = 44.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.Center,
                color = if (text.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }

        // 底部输入区
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("点击这里输入...") },
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Button(
                        onClick = { if (text.isNotBlank()) onSpeak(text) },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("朗读")
                    }
                    TextButton(
                        onClick = { text = "" },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("清空")
                    }
                }
            }
        }
    }
}