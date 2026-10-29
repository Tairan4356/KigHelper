package com.ziegler.kighelper.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ziegler.kighelper.data.camera.GalleryPhoto
import com.ziegler.kighelper.data.camera.TimelineItem
import com.ziegler.kighelper.data.scan.ScanRecord
import com.ziegler.kighelper.data.scan.ScanRecordType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rowTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

/** 日期分组标题，今天/昨天使用相对文案。 */
@Composable
internal fun DateHeader(date: LocalDate?) {
    Text(
        text = formatTimelineDateLabel(date),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

/**
 * 单条时光轴内容。
 *
 * 短按查看详情，长按进入多选；多选状态下右侧显示复选框。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimelineRow(
    item: TimelineItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }, shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (item) {
                is TimelineItem.Photo -> PhotoLeading(item.photo)
                is TimelineItem.Scan -> ScanLeading(item.record)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (item) {
                        is TimelineItem.Photo -> item.photo.displayName
                        is TimelineItem.Scan -> item.record.title
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (item) {
                        is TimelineItem.Photo -> item.photo.bucketName ?: "相册"
                        is TimelineItem.Scan -> item.record.subtitle ?: item.record.rawContent
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.timestamp > 0L) {
                    Text(
                        text = Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault())
                            .format(rowTimeFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            }
        }
    }
}

/** 固定解码到 180px 的缩略图，避免列表滚动时加载原图。 */
@Composable
private fun PhotoLeading(photo: GalleryPhoto) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(photo.uri).size(180).build(),
        contentDescription = photo.displayName,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

/** 使用内容类型图标占位，避免扫码记录依赖网络缩略图。 */
@Composable
private fun ScanLeading(record: ScanRecord) {
    val icon = when (record.type) {
        ScanRecordType.SOCIAL_CARD -> Icons.Filled.QrCode
        ScanRecordType.PLATFORM -> Icons.Filled.Link
        ScanRecordType.TEXT -> Icons.AutoMirrored.Filled.TextSnippet
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)
            ), contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** 多选底部操作栏；删除按钮由页面统一确认。 */
@Composable
internal fun SelectionBottomBar(
    count: Int, enabled: Boolean, onDelete: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选择 $count 项",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onDelete,
                enabled = enabled && count > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("删除")
            }
        }
    }
}
