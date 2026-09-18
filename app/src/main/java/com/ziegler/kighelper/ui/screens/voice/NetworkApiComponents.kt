package com.ziegler.kighelper.ui.screens.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.ziegler.kighelper.data.NetworkTtsConfig

/**
 * 网络 API 合成引擎配置卡片：填写 OpenAI 兼容接口的地址、模型、音色和自己的 key。
 */
@Composable
fun NetworkApiConfigCard(
    config: NetworkTtsConfig,
    onSave: (NetworkTtsConfig) -> Unit,
    message: String?
) {
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var modelId by remember(config) { mutableStateOf(config.modelId) }
    var voiceId by remember(config) { mutableStateOf(config.voiceId) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var showKey by remember { mutableStateOf(false) }

    val draft = NetworkTtsConfig(
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        modelId = modelId.trim(),
        voiceId = voiceId.trim()
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("接口地址（Base URL）") },
            placeholder = { Text("https://api.openai.com/v1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = modelId,
            onValueChange = { modelId = it },
            label = { Text("模型 ID") },
            placeholder = { Text("tts-1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = voiceId,
            onValueChange = { voiceId = it },
            label = { Text("音色") },
            placeholder = { Text("alloy") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            singleLine = true,
            visualTransformation = if (showKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        imageVector = if (showKey) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (showKey) "隐藏 Key" else "显示 Key"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "使用 OpenAI 兼容的 /audio/speech 接口（Authorization: Bearer）。" +
                "支持 OpenAI、阿里百炼、硅基流动等兼容服务，key 仅保存在本机。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = { onSave(draft) },
            enabled = draft.isUsable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存配置")
        }
        message?.let {
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}