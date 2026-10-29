package com.ziegler.kighelper.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.camera.TimelineFilter

/** 全部/照片/扫码三个筛选 Chip；数量直接来自当前完整数据。 */
@Composable
internal fun TimelineFilterRow(
    selected: TimelineFilter,
    photoCount: Int,
    scanCount: Int,
    enabled: Boolean,
    onSelected: (TimelineFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == TimelineFilter.ALL,
            onClick = { onSelected(TimelineFilter.ALL) },
            label = { Text("全部 ${photoCount + scanCount}") },
            enabled = enabled
        )
        FilterChip(
            selected = selected == TimelineFilter.PHOTOS,
            onClick = { onSelected(TimelineFilter.PHOTOS) },
            label = { Text("照片 $photoCount") },
            enabled = enabled
        )
        FilterChip(
            selected = selected == TimelineFilter.SCANS,
            onClick = { onSelected(TimelineFilter.SCANS) },
            label = { Text("扫码 $scanCount") },
            enabled = enabled
        )
    }
}

/** 完全无相册权限时的说明卡片。 */
@Composable
internal fun MediaPermissionCard(
    onRequest: () -> Unit, onOpenSettings: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("允许访问相册", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequest) { Text("允许访问") }
                TextButton(onClick = onOpenSettings) { Text("系统设置") }
            }
        }
    }
}

/** Android 14 仅授权部分照片时的提示，可再次打开系统照片选择器。 */
@Composable
internal fun PartialAccessBanner(onManage: () -> Unit) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前仅显示已授权的照片",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onManage) { Text("管理照片访问") }
        }
    }
}

/** 无照片且无扫码记录时的空态。 */
@Composable
internal fun EmptyTimeline() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("暂无内容", color = MaterialTheme.colorScheme.outline)
    }
}
