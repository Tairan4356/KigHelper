package com.ziegler.kighelper.data.scan

import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.socialshare.SocialCardShareCodec
import com.ziegler.kighelper.data.socialshare.SocialPlatformShareIds
import com.ziegler.kighelper.data.socialshare.SocialPlatformUrlResolver
import java.util.UUID

/** 扫码内容分类，决定时光轴图标和从历史恢复详情的方式。 */
enum class ScanRecordType {
    SOCIAL_CARD, PLATFORM, TEXT
}

/**
 * 持久化的扫码历史条目。
 *
 * 只保存展示所需字段和原始内容；社交卡片详情由 [cardUrl] 按需解码，避免历史文件
 * 随联系人数量膨胀。
 */
data class ScanRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: ScanRecordType = ScanRecordType.TEXT,
    val rawContent: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val platformId: Int? = null,
    val cardUrl: String? = null
)

/**
 * 已识别的扫码内容。
 *
 * [SocialCard] 携带解码后的资料，[Platform] 只携带平台匹配信息，
 * 其他内容统一为可复制的 [Text]。
 */
sealed interface ScannedContent {
    data class SocialCard(
        val rawUrl: String,
        val payload: SocialCardShareCodec.SharePayload,
        val profile: SocialCardProfile
    ) : ScannedContent

    data class Platform(
        val rawContent: String, val match: SocialPlatformUrlResolver.PlatformMatch
    ) : ScannedContent

    data class Text(val rawContent: String) : ScannedContent
}

/**
 * 把原始扫码文本归类为社交卡片、已知平台或普通文本。
 */
object ScannedContentClassifier {

    /**
     * 分类优先级：集合码 -> 已知平台 -> 普通文本。
     *
     * 集合码 URL 一定带 `#`，先做轻量判断可以避免对普通文本执行 Base64 解码。
     */
    fun classify(raw: String): ScannedContent {
        val value = raw.trim()
        val payload = if (value.contains('#')) {
            SocialCardShareCodec.decodeUrl(value)
        } else {
            null
        }
        if (payload != null) {
            return ScannedContent.SocialCard(
                rawUrl = value, payload = payload, profile = payload.toProfile()
            )
        }

        val platform = SocialPlatformUrlResolver.detect(value)
        if (platform != null) {
            return ScannedContent.Platform(value, platform)
        }

        return ScannedContent.Text(value)
    }

    /**
     * 从历史记录恢复详情页内容。
     *
     * 集合码解码失败时退化为纯文本，保证旧版本或截断的记录仍可查看。
     */
    fun fromRecord(record: ScanRecord): ScannedContent {
        return when (record.type) {
            ScanRecordType.SOCIAL_CARD -> {
                val url = record.cardUrl ?: record.rawContent
                val payload = SocialCardShareCodec.decodeUrl(url)
                if (payload != null) {
                    ScannedContent.SocialCard(url, payload, payload.toProfile())
                } else {
                    ScannedContent.Text(record.rawContent)
                }
            }

            ScanRecordType.PLATFORM -> {
                val iconKey = SocialPlatformShareIds.iconKeyOf(record.platformId ?: 0)
                val displayName = SocialPlatformShareIds.displayNameOf(record.platformId ?: 0)
                ScannedContent.Platform(
                    rawContent = record.rawContent,
                    match = SocialPlatformUrlResolver.PlatformMatch(iconKey, displayName)
                )
            }

            ScanRecordType.TEXT -> ScannedContent.Text(record.rawContent)
        }
    }
}

/** 提取用于时光轴展示的轻量字段，避免把完整资料重复写入 JSON。 */
fun ScannedContent.toScanRecord(): ScanRecord {
    return when (this) {
        is ScannedContent.SocialCard -> ScanRecord(
            type = ScanRecordType.SOCIAL_CARD,
            rawContent = rawUrl,
            title = payload.nickname.ifBlank { "扩列卡片" },
            subtitle = payload.signature.takeIf { it.isNotBlank() }
                ?: "${payload.contacts.size} 个平台",
            cardUrl = rawUrl)

        is ScannedContent.Platform -> ScanRecord(
            type = ScanRecordType.PLATFORM,
            rawContent = rawContent,
            title = match.displayName,
            subtitle = rawContent,
            platformId = SocialPlatformShareIds.of(match.iconKey)
        )

        is ScannedContent.Text -> ScanRecord(
            type = ScanRecordType.TEXT,
            rawContent = rawContent,
            title = "文本内容",
            subtitle = rawContent.replace('\n', ' ').take(80)
        )
    }
}

/** 把分享协议中的精简联系人转换成卡片展示模型。 */
private fun SocialCardShareCodec.SharePayload.toProfile(): SocialCardProfile {
    val mappedContacts = contacts.mapIndexed { index, contact ->
        SocialContact(
            id = "scanned-${contact.platformId}-$index",
            displayName = contact.displayName.ifBlank { "社交平台" },
            iconKey = SocialPlatformShareIds.iconKeyOf(contact.platformId),
            handle = contact.handle.ifBlank { contact.target.orEmpty() },
            qrContent = contact.target,
            qrContentChecked = true
        )
    }
    return SocialCardProfile(
        nickname = nickname,
        signature = signature,
        templateIndex = SocialCardProfile.ADAPTIVE_TEMPLATE_INDEX,
        contacts = mappedContacts
    )
}
