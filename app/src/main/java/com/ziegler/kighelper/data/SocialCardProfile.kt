package com.ziegler.kighelper.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ziegler.kighelper.R
import java.util.UUID

/**
 * 社交平台单条联系方式。
 *
 * @param id 联系方式唯一 ID；预设平台使用稳定常量，自定义平台使用 UUID
 * @param displayName 用户可见的平台名称（可被修改）
 * @param iconKey 平台图标在 [SocialPlatformIcons] 中的查找键，便于切换图标
 * @param customIconPath 自定义上传图标的本地路径；非空时优先于 [iconKey]
 * @param handle 用户在该平台的账号/ID
 * @param qrCodePath 二维码图片本地路径，null 表示未上传
 */
data class SocialContact(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val iconKey: String = SocialPlatformIcons.KEY_DEFAULT,
    val customIconPath: String? = null,
    val handle: String = "",
    val qrCodePath: String? = null
)

/**
 * 个人社交卡片整体资料。
 *
 * @param nickname 昵称
 * @param signature 个性签名
 * @param avatarPath 头像本地路径，null 表示使用默认占位
 * @param templateIndex 选用的预设模板索引；为 [CUSTOM_TEMPLATE_INDEX] 时使用自定义背景，
 *                      为 [ADAPTIVE_TEMPLATE_INDEX] 时使用自适应颜色（primary），
 *                      为 [CUSTOM_COLOR_TEMPLATE_INDEX] 时使用自定义颜色
 * @param customBackgroundPath 自定义背景图本地路径
 * @param customColor 自定义背景颜色（仅当 templateIndex 为 [CUSTOM_COLOR_TEMPLATE_INDEX] 时使用）
 * @param contacts 已配置的社交平台列表，按用户排序
 */
data class SocialCardProfile(
    val nickname: String = "",
    val signature: String = "",
    val avatarPath: String? = null,
    val templateIndex: Int = 0,
    val customBackgroundPath: String? = null,
    val customColor: Long = 0xFF6650A4, // 默认紫色
    val contacts: List<SocialContact> = emptyList()
) {
    /**
     * 已配置（有账号或二维码）的联系方式，按用户排序。
     * 供卡片展示与外部动作（如跳转平台 App）共用，避免重复过滤导致口径不一致。
     */
    val visibleContacts: List<SocialContact>
        get() = contacts.filter { it.qrCodePath != null || it.handle.isNotBlank() }

    companion object {
        /** 表示使用自定义背景的模板索引哨兵值 */
        const val CUSTOM_TEMPLATE_INDEX = -1

        /** 表示使用自适应颜色（primary）的模板索引哨兵值 */
        const val ADAPTIVE_TEMPLATE_INDEX = -2

        /** 表示使用自定义颜色的模板索引哨兵值 */
        const val CUSTOM_COLOR_TEMPLATE_INDEX = -3

        val DEFAULT: SocialCardProfile = SocialCardProfile(
            nickname = "",
            signature = "",
            templateIndex = ADAPTIVE_TEMPLATE_INDEX,
            contacts = SocialPlatformIcons.DEFAULT_CONTACTS
        )
    }
}

/**
 * 平台图标资源映射。
 *
 * 维护「图标键 -> drawable 资源」的固定表，让 UI 在切换图标或新增自定义平台时
 * 始终能从一个入口取到 drawable，避免散落的 when 分支。
 */
object SocialPlatformIcons {
    const val KEY_QQ = "qq"
    const val KEY_WECHAT = "wechat"
    const val KEY_BILIBILI = "bilibili"
    const val KEY_DOUYIN = "douyin"
    const val KEY_WEIBO = "weibo"
    const val KEY_X = "X"
    const val KEY_FACEBOOK = "facebook"
    const val KEY_INSTAGRAM = "instagram"
    const val KEY_TELEGRAM = "telegram"
    const val KEY_DEFAULT = "default"

    private val iconResources: Map<String, Int> = mapOf(
        KEY_QQ to R.drawable.qq_colored,
        KEY_WECHAT to R.drawable.wechat,
        KEY_BILIBILI to R.drawable.bilibili,
        KEY_DOUYIN to R.drawable.douyin,
        KEY_WEIBO to R.drawable.weibo,
        KEY_X to R.drawable.x,
        KEY_FACEBOOK to R.drawable.facebook,
        KEY_INSTAGRAM to R.drawable.instagram,
        KEY_TELEGRAM to R.drawable.telegram
    )

    /** 所有可选图标键（不含 default）。 */
    val selectableKeys: List<String> = iconResources.keys.toList()

    @DrawableRes
    fun iconRes(key: String): Int = iconResources[key] ?: R.drawable.ic_bubble

    /** 4 个预设平台的初始联系方式（handle 为空，等待用户填写）。 */
    val DEFAULT_CONTACTS: List<SocialContact> = listOf(
        SocialContact(id = KEY_QQ, displayName = "QQ", iconKey = KEY_QQ),
        SocialContact(id = KEY_WECHAT, displayName = "微信", iconKey = KEY_WECHAT),
        SocialContact(id = KEY_BILIBILI, displayName = "B站", iconKey = KEY_BILIBILI),
        SocialContact(id = KEY_DOUYIN, displayName = "抖音", iconKey = KEY_DOUYIN)
    )
}

/**
 * 卡片模板定义。
 *
 * @param name 模板名称（用于模板选择列表展示）
 * @param background 卡片背景色（纯色，符合 md3 设计规范）
 * @param onBackground 在该背景上文字的颜色（一般为白或黑）
 */
data class CardTemplate(
    val name: String, val background: Color, val onBackground: Color
)

/** 预设卡片模板（纯色，符合 md3 设计规范）。 */
object CardTemplates {
    val presets: List<CardTemplate> = listOf(
        CardTemplate(
            name = "暮色橙", background = Color(0xFFFF9800), onBackground = Color.White
        ), CardTemplate(
            name = "罪业红", background = Color(0xFFFF1744), onBackground = Color.White
        ), CardTemplate(
            name = "极夜蓝", background = Color(0xFF0D1B2A), onBackground = Color.White
        ), CardTemplate(
            name = "极光青", background = Color(0xFF00E5FF), onBackground = Color(0xFF1C1B1F)
        ), CardTemplate(
            name = "业火橙", background = Color(0xFFFF6F00), onBackground = Color.White
        ), CardTemplate(
            name = "紫雷", background = Color(0xFF4A148C), onBackground = Color.White
        ), CardTemplate(
            name = "苍穹绿", background = Color(0xFF004D40), onBackground = Color.White
        ), CardTemplate(
            name = "彼岸红", background = Color(0xFFD50000), onBackground = Color.White
        ), CardTemplate(
            name = "镜花蓝", background = Color(0xFFB3E5FC), onBackground = Color(0xFF1C1B1F)
        ), CardTemplate(
            name = "终焉白", background = Color(0xFFCFD8DC), onBackground = Color(0xFF1C1B1F)
        )
    )
}
