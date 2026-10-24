package com.ziegler.kighelper.ui.components

import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact

/** 卡片上可被选中的条目：总二维码或某个平台。 */
sealed interface SocialCardEntry {
    data object Aggregate : SocialCardEntry

    data class Platform(val contact: SocialContact) : SocialCardEntry
}

/**
 * 构建卡片条目列表。
 *
 * 总码只在卡片存在可分享内容时出现；否则保持原有空卡片行为。
 */
fun socialCardEntries(
    profile: SocialCardProfile, showAggregate: Boolean = true
): List<SocialCardEntry> {
    val aggregateAvailable =
        showAggregate && (profile.nickname.isNotBlank() || profile.signature.isNotBlank() || profile.aggregateContacts.isNotEmpty())
    return buildList {
        if (aggregateAvailable) add(SocialCardEntry.Aggregate)
        profile.visibleContacts.forEach { add(SocialCardEntry.Platform(it)) }
    }
}
