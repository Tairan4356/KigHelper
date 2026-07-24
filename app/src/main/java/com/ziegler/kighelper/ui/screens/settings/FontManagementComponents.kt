package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.FontCatalogItem
import com.ziegler.kighelper.ui.FontDownloadState
import com.ziegler.kighelper.utils.InstalledFont

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnlineFontSection(
    fonts: List<FontCatalogItem>?,
    isLoading: Boolean,
    downloadState: FontDownloadState,
    installedFontIds: Set<String>,
    onDownload: (FontCatalogItem) -> Unit,
    onDelete: (FontCatalogItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(max = 400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "在线字体",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        }

        if (fonts.isNullOrEmpty() && !isLoading) {
            Text(
                "无法加载字体目录，请检查网络后重试",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            fonts?.forEach { font ->
                val isInstalled = installedFontIds.contains(font.id)
                OnlineFontCard(
                    font = font,
                    isInstalled = isInstalled,
                    isDownloading = downloadState.isDownloading,
                    onDownload = { onDownload(font) },
                    onDelete = { onDelete(font) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(state: FontDownloadState) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "正在下载: ${state.currentFont}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${state.currentWeight} (${state.completedWeights}/${state.totalWeights})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp), strokeWidth = 2.dp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.progress }, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnlineFontCard(
    font: FontCatalogItem,
    isInstalled: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp, 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        font.displayName, style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        font.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatSizeKB(font.sizeKB),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (isInstalled) {
                    TextButton(
                        onClick = { return@TextButton }, enabled = false
                    ) {
                        Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("已安装", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    TextButton(
                        onClick = onDownload, enabled = !isDownloading
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("安装")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除字体") },
            text = { Text("确定要删除「${font.displayName}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }, colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            })
    }
}

@Composable
fun InstalledFontsSection(
    fonts: List<InstalledFont>,
    selectedFont: String?,
    onDeleteFont: (InstalledFont) -> Unit,
    onImportFont: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(max = 400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "已安装字体",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            OutlinedButton(onClick = onImportFont) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("导入字体文件")
            }
        }

        if (fonts.isEmpty()) {
            Text(
                "暂无已安装的自定义字体",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            fonts.forEach { font ->
                val isSelected = selectedFont == font.baseName
                InstalledFontItem(
                    font = font, isSelected = isSelected, onDelete = { onDeleteFont(font) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InstalledFontItem(
    font: InstalledFont, isSelected: Boolean, onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        font.displayName, style = MaterialTheme.typography.bodyLarge
                    )
                    if (font.catalogFontId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "在线字体",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (font.weights.size > 1) {
                        Text(
                            "${font.weights.size} 种字重",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Text(
                            "使用中",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = {
                if (isSelected) {
                    showDeleteDialog = true
                } else {
                    onDelete()
                }
            }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除字体") },
            text = { Text("「${font.displayName}」正在使用中，删除后将变回系统默认字体。确定要删除吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }, colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            })
    }
}

private fun formatSizeKB(sizeKB: Long): String {
    return if (sizeKB >= 1000) {
        "${sizeKB / 1000}.${(sizeKB % 1000) / 100}MB"
    } else {
        "${sizeKB}KB"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontManagementDialog(
    fontCatalog: List<FontCatalogItem>?,
    isLoadingCatalog: Boolean,
    installedFonts: List<InstalledFont>,
    downloadState: FontDownloadState,
    selectedFont: String?,
    onImportFont: () -> Unit,
    onDownload: (FontCatalogItem) -> Unit,
    onDeleteInstalled: (InstalledFont) -> Unit,
    onDeleteOnline: (FontCatalogItem) -> Unit,
    onRefreshCatalog: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarMessage = downloadState.snackbarMessage

    AlertDialog(onDismissRequest = onDismiss, title = { Text("字体管理") }, text = {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("已安装") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("在线字体") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> InstalledFontsSection(
                    fonts = installedFonts,
                    selectedFont = selectedFont,
                    onDeleteFont = onDeleteInstalled,
                    onImportFont = onImportFont
                )

                1 -> OnlineFontSection(
                    fonts = fontCatalog,
                    isLoading = isLoadingCatalog,
                    downloadState = downloadState,
                    installedFontIds = installedFonts.mapNotNull { it.catalogFontId }.toSet(),
                    onDownload = onDownload,
                    onDelete = onDeleteOnline,
                    onRefresh = onRefreshCatalog
                )
            }

            if (downloadState.isDownloading) {
                DownloadProgressCard(downloadState)
            }

            if (snackbarMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(
                        containerColor = if (snackbarMessage.contains("失败") || snackbarMessage.contains(
                                "错误"
                            )
                        ) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = snackbarMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (snackbarMessage.contains("失败") || snackbarMessage.contains(
                                "错误"
                            )
                        ) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("关闭")
        }
    })
}
