package com.ziegler.kighelper.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ziegler.kighelper.data.SocialPlatformIcons
import com.ziegler.kighelper.data.camera.GalleryPhoto
import com.ziegler.kighelper.data.camera.TimelineItem
import com.ziegler.kighelper.data.scan.ScanRecord
import com.ziegler.kighelper.data.scan.ScanRecordType
import com.ziegler.kighelper.data.socialshare.SocialPlatformShareIds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rowTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
private const val GRID_THUMBNAIL_SIZE = 360

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

/** 缩略图网格中的单条内容，照片铺满卡片，扫码记录显示类型图标和底部预览文字。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimelineThumbnail(
    item: TimelineItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = shape,
        color = if (item is TimelineItem.Scan) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            when (item) {
                is TimelineItem.Photo -> AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(item.photo.uri)
                        .size(GRID_THUMBNAIL_SIZE).build(),
                    contentDescription = item.photo.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                is TimelineItem.Scan -> ScanThumbnailContent(item.record)
            }
            if (selectionMode && selected) {
                SelectedMark(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ScanThumbnailContent(record: ScanRecord) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        when (record.type) {
            ScanRecordType.SOCIAL_CARD -> Icon(
                imageVector = Icons.Filled.QrCode,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            ScanRecordType.PLATFORM -> Image(
                painter = painterResource(
                    SocialPlatformIcons.iconRes(
                        SocialPlatformShareIds.iconKeyOf(record.platformId ?: 0)
                    )
                ),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                contentScale = ContentScale.Fit
            )

            ScanRecordType.TEXT -> Icon(
                imageVector = Icons.AutoMirrored.Filled.TextSnippet,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            text = scanThumbnailLabel(record),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun scanThumbnailLabel(record: ScanRecord): String = when (record.type) {
    ScanRecordType.SOCIAL_CARD -> record.title.ifBlank { "扩列卡片" }
    else -> record.rawContent.replace('\n', ' ').ifBlank { record.title }
}

@Composable
private fun SelectedMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(22.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "已选择",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
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
