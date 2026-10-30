package com.ziegler.kighelper.ui.screens.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timelineDateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
private val timelineTimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
private val timelineMonthFormat = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

/** 时光轴分组标题：今天、昨天或完整日期。 */
internal fun formatTimelineDateLabel(date: LocalDate?): String {
    if (date == null) return "未知日期"
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(timelineDateFormat)
    }
}

/** 快速浏览手柄旁只显示到月份，避免长日期挤压气泡宽度。 */
internal fun formatTimelineMonthLabel(date: LocalDate?): String {
    return date?.format(timelineMonthFormat) ?: "未知日期"
}

/** 查看器底部显示拍摄时间，例如“今天 18:06”。 */
internal fun formatTimelineTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "未知时间"
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val dateLabel = when (dateTime.toLocalDate()) {
        LocalDate.now() -> "今天"
        LocalDate.now().minusDays(1) -> "昨天"
        else -> dateTime.format(timelineDateFormat)
    }
    return "$dateLabel ${dateTime.format(timelineTimeFormat)}"
}
