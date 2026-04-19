package com.ziegler.kighelper.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.Phrase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    phrases: List<Phrase>, onPhraseClick: (Phrase) -> Unit, onSettingsClick: () -> Unit
) {
    var lastSpoken by remember { mutableStateOf("点击下方进行沟通") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, "设置")
            }
        }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 显示区
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(lastSpoken, fontSize = 32.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(16.dp))
            // 按钮区
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(0.7f)) {
                items(phrases) { phrase ->
                    Button(
                        onClick = {
                            lastSpoken = phrase.speech
                            onPhraseClick(phrase)
                        }, modifier = Modifier
                            .padding(4.dp)
                            .height(80.dp)
                    ) {
                        Text(phrase.label, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}