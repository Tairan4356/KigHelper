package com.ziegler.kighelper.ui.screens.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.utils.OfflineVoiceModelFormat
import com.ziegler.kighelper.utils.OfflineVoiceModelStatus
import com.ziegler.kighelper.utils.RemoteVoiceModelCatalogEntry

/**
 * 离线模型状态卡片
 */
@Composable
fun OfflineModelStatusCard(
    activeModelStatus: OfflineVoiceModelStatus?,
    installMessage: String?,
    onClick: () -> Unit
) {
    val isReady = activeModelStatus?.isReady == true
    val isPartial = activeModelStatus?.isPartiallyInstalled == true
    val compatibilityIssue = activeModelStatus?.runtimeCompatibilityIssue

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "点击选择或导入模型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    isReady -> "当前模型：${activeModelStatus.pack.name} · ${activeModelStatus.pack.format.label}"
                    isPartial -> "已导入 ONNX 权重，但缺少：${activeModelStatus.missingFiles.joinToString()}"
                    else -> "模型未安装"
                },
                style = MaterialTheme.typography.bodySmall
            )
            if (compatibilityIssue != null) {
                Text(
                    text = compatibilityIssue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (isPartial) {
                Text(
                    text = "缺少文本前端文件时无法把中文转换为模型 token，端侧推理会自动回退系统 TTS。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (installMessage != null) {
                Text(
                    text = installMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 模型选择对话框
 */
@Composable
fun ModelPickerDialog(
    activeModelId: String?,
    remoteModelCatalog: List<RemoteVoiceModelCatalogEntry>,
    modelStatuses: List<OfflineVoiceModelStatus>,
    isInstalling: Boolean,
    installProgress: Float,
    installMessage: String?,
    selectedImportFormat: OfflineVoiceModelFormat,
    downloadUrl: String,
    onImportFormatSelect: (OfflineVoiceModelFormat) -> Unit,
    onDownloadUrlChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (RemoteVoiceModelCatalogEntry) -> Unit,
    onSelectStatus: (OfflineVoiceModelStatus) -> Unit,
    onDeleteModel: (OfflineVoiceModelStatus) -> Unit,
    onInstall: (RemoteVoiceModelCatalogEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择端侧模型") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        text = "端侧引擎使用Sherpa-ONNX，支持VITS、Piper和Kokoro格式的模型包。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                items(remoteModelCatalog, key = { it.pack.id }) { entry ->
                    val status = modelStatuses.firstOrNull { it.pack.id == entry.pack.id }
                    RemoteModelItem(
                        entry = entry,
                        selected = entry.pack.id == activeModelId,
                        installed = status?.isReady == true,
                        partiallyInstalled = status?.isPartiallyInstalled == true,
                        isInstalling = isInstalling,
                        onSelect = { onSelect(entry) },
                        onInstall = { onInstall(entry) }
                    )
                }
                val remoteModelIds = remoteModelCatalog.map { it.pack.id }.toSet()
                val customModelStatuses = modelStatuses.filter { it.pack.id !in remoteModelIds }
                if (customModelStatuses.isNotEmpty()) {
                    item {
                        Text(
                            text = "用户导入模型",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(customModelStatuses, key = { it.pack.id }) { status ->
                        ImportedModelItem(
                            status = status,
                            selected = status.pack.id == activeModelId,
                            isInstalling = isInstalling,
                            onSelect = { onSelectStatus(status) },
                            onDelete = { onDeleteModel(status) }
                        )
                    }
                }
                item {
                    Text(
                        text = "手动安装",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item {
                    Text(
                        text = "模型格式（格式错误的模型可能会导致程序崩溃）",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                item {
                    ImportFormatSelector(
                        selected = selectedImportFormat,
                        onSelect = onImportFormatSelect
                    )
                }
                item {
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = onDownloadUrlChange,
                        label = { Text("模型压缩包下载地址") },
                        enabled = !isInstalling,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onImportClick,
                            enabled = !isInstalling,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("导入压缩包")
                        }
                        OutlinedButton(
                            onClick = onDownloadClick,
                            enabled = !isInstalling,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下载压缩包")
                        }
                    }
                }
                if (isInstalling) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (installProgress > 0) {
                                    "正在安装模型包 (${(installProgress * 100).toInt()}%)..."
                                } else {
                                    "正在准备安装模型包..."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress = { installProgress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (installMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = installMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 导入模型单项
 */
@Composable
fun ImportedModelItem(
    status: OfflineVoiceModelStatus,
    selected: Boolean,
    isInstalling: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val compatibilityIssue = status.runtimeCompatibilityIssue
                Text(status.pack.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = when {
                        compatibilityIssue != null -> compatibilityIssue
                        status.isReady -> "已安装 · ${status.pack.format.label}"
                        status.isPartiallyInstalled -> "缺少：${status.missingFiles.joinToString()}"
                        else -> "未安装完整"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onSelect,
                enabled = !isInstalling && status.isReady && status.runtimeCompatibilityIssue == null
            ) {
                Text(if (selected) "已选择" else "选择")
            }
            IconButton(
                onClick = onDelete,
                enabled = !isInstalling
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除模型",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 远程模型单项
 */
@Composable
fun RemoteModelItem(
    entry: RemoteVoiceModelCatalogEntry,
    selected: Boolean,
    installed: Boolean,
    partiallyInstalled: Boolean,
    isInstalling: Boolean,
    onSelect: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.pack.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        entry.pack.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = if (installed) onSelect else onInstall,
                    enabled = !isInstalling,
                ) {
                    Text(
                        when {
                            installed && selected -> "已选择"
                            installed -> "选择"
                            partiallyInstalled -> "补齐文件"
                            else -> "一键安装"
                        }
                    )
                }
            }
            Text(
                if (partiallyInstalled) {
                    "已导入 ONNX，但仍缺少：${
                        entry.pack.requiredFiles.filterNot { it == "config.json" }.joinToString()
                    }"
                } else {
                    "${entry.pack.format.label} · 说话人：${entry.pack.speakerCount}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 模型使用合规声明对话框
 */
@Composable
fun ModelComplianceDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型使用合规声明") },
        text = {
            Text(
                "继续操作前，请确认你导入或下载的模型来源合法，许可允许在本应用中离线端侧推理，并且不包含未经授权的声音克隆或受限制数据。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("我已确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
