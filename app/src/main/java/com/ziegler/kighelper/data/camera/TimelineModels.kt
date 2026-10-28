package com.ziegler.kighelper.data.camera

import com.ziegler.kighelper.data.scan.ScanRecord

/** 时光轴筛选条件。 */
enum class TimelineFilter {
    ALL, PHOTOS, SCANS
}

/**
 * 时光轴中的一条内容，照片和扫码记录共享时间排序与选中逻辑。
 *
 * [stableKey] 前缀区分两类数据，防止照片 ID 与扫码记录 ID 恰好相同时 LazyColumn
 * 复用错误的 item。
 */
sealed interface TimelineItem {
    val timestamp: Long
    val stableKey: String

    data class Photo(val photo: GalleryPhoto) : TimelineItem {
        override val timestamp: Long get() = photo.timestamp
        override val stableKey: String get() = "photo:${photo.id}"
    }

    data class Scan(val record: ScanRecord) : TimelineItem {
        override val timestamp: Long get() = record.timestamp
        override val stableKey: String get() = "scan:${record.id}"
    }
}

/** 多选状态；照片和扫码记录使用不同 ID 类型，分别保存。 */
data class TimelineSelection(
    val photoIds: Set<Long> = emptySet(), val scanIds: Set<String> = emptySet()
) {
    val photoCount: Int get() = photoIds.size
    val scanCount: Int get() = scanIds.size
    val totalCount: Int get() = photoCount + scanCount
    val isEmpty: Boolean get() = totalCount == 0

    /** 切换单条内容；已选中则移除，否则加入。 */
    fun toggle(item: TimelineItem): TimelineSelection = when (item) {
        is TimelineItem.Photo -> copy(
            photoIds = if (item.photo.id in photoIds) {
                photoIds - item.photo.id
            } else {
                photoIds + item.photo.id
            }
        )

        is TimelineItem.Scan -> copy(
            scanIds = if (item.record.id in scanIds) {
                scanIds - item.record.id
            } else {
                scanIds + item.record.id
            }
        )
    }

    /** 选中当前可见的全部内容。 */
    fun selectAll(items: List<TimelineItem>): TimelineSelection {
        return TimelineSelection(
            photoIds = items.filterIsInstance<TimelineItem.Photo>().map { it.photo.id }.toSet(),
            scanIds = items.filterIsInstance<TimelineItem.Scan>().map { it.record.id }.toSet()
        )
    }
}

/** 把照片和扫码记录按筛选条件合并成统一时间线。 */
object TimelineAssembler {
    /**
     * 按时间倒序合并；时间戳相同时扫码记录排在照片前，保证时间线稳定。
     */
    fun merge(
        photos: List<GalleryPhoto>,
        scans: List<ScanRecord>,
        filter: TimelineFilter = TimelineFilter.ALL
    ): List<TimelineItem> {
        val items = buildList {
            if (filter != TimelineFilter.SCANS) {
                addAll(photos.map(TimelineItem::Photo))
            }
            if (filter != TimelineFilter.PHOTOS) {
                addAll(scans.map(TimelineItem::Scan))
            }
        }
        return items.sortedWith(compareByDescending<TimelineItem> { it.timestamp }.thenBy {
            if (it is TimelineItem.Scan) 0 else 1
        })
    }
}
