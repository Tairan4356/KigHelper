package com.ziegler.kighelper.data.socialshare

import com.ziegler.kighelper.data.SocialPlatformIcons
import java.net.URLEncoder

/**
 * 平台图标键到集合码协议中的平台 ID。
 *
 * ID 一经发布必须保持稳定，前端静态页按同一张表还原平台语义。
 */
object SocialPlatformShareIds {
    const val DEFAULT = 0
    const val QQ = 1
    const val WECHAT = 2
    const val BILIBILI = 3
    const val DOUYIN = 4
    const val WEIBO = 5
    const val X = 6
    const val FACEBOOK = 7
    const val INSTAGRAM = 8
    const val TELEGRAM = 9

    fun of(iconKey: String): Int = when (iconKey) {
        SocialPlatformIcons.KEY_QQ -> QQ
        SocialPlatformIcons.KEY_WECHAT -> WECHAT
        SocialPlatformIcons.KEY_BILIBILI -> BILIBILI
        SocialPlatformIcons.KEY_DOUYIN -> DOUYIN
        SocialPlatformIcons.KEY_WEIBO -> WEIBO
        SocialPlatformIcons.KEY_X -> X
        SocialPlatformIcons.KEY_FACEBOOK -> FACEBOOK
        SocialPlatformIcons.KEY_INSTAGRAM -> INSTAGRAM
        SocialPlatformIcons.KEY_TELEGRAM -> TELEGRAM
        else -> DEFAULT
    }

    fun iconKeyOf(platformId: Int): String = when (platformId) {
        QQ -> SocialPlatformIcons.KEY_QQ
        WECHAT -> SocialPlatformIcons.KEY_WECHAT
        BILIBILI -> SocialPlatformIcons.KEY_BILIBILI
        DOUYIN -> SocialPlatformIcons.KEY_DOUYIN
        WEIBO -> SocialPlatformIcons.KEY_WEIBO
        X -> SocialPlatformIcons.KEY_X
        FACEBOOK -> SocialPlatformIcons.KEY_FACEBOOK
        INSTAGRAM -> SocialPlatformIcons.KEY_INSTAGRAM
        TELEGRAM -> SocialPlatformIcons.KEY_TELEGRAM
        else -> SocialPlatformIcons.KEY_DEFAULT
    }

    fun displayNameOf(platformId: Int): String = when (platformId) {
        QQ -> "QQ"
        WECHAT -> "微信"
        BILIBILI -> "B站"
        DOUYIN -> "抖音"
        WEIBO -> "微博"
        X -> "X"
        FACEBOOK -> "Facebook"
        INSTAGRAM -> "Instagram"
        TELEGRAM -> "Telegram"
        else -> "社交平台"
    }
}

/**
 * 按第三方平台规则把账号拼成完整 URL。
 *
 * 只在账号本身不是 URL 时参与拼接；无法可靠拼接的平台（如微信）返回 null，
 * 前端将只展示账号文本而不生成二维码。
 */
object SocialPlatformUrlResolver {

    data class PlatformMatch(
        val iconKey: String, val displayName: String
    )

    private data class PlatformRule(
        val iconKey: String,
        val displayName: String,
        val hosts: Set<String>,
        val schemes: Set<String>
    )

    private val rules = listOf(
        PlatformRule(
            SocialPlatformIcons.KEY_QQ,
            "QQ",
            setOf("qq.com", "qm.qq.com"),
            setOf("mqqapi", "mqq", "qq")
        ), PlatformRule(
            SocialPlatformIcons.KEY_WECHAT,
            "微信",
            setOf("weixin.qq.com", "wechat.com", "weixin.com"),
            setOf("weixin", "wechat")
        ), PlatformRule(
            SocialPlatformIcons.KEY_BILIBILI,
            "B站",
            setOf("bilibili.com", "b23.tv", "bili2233.cn"),
            setOf("bilibili", "bstar")
        ), PlatformRule(
            SocialPlatformIcons.KEY_DOUYIN,
            "抖音",
            setOf("douyin.com", "iesdouyin.com"),
            setOf("snssdk1128", "aweme")
        ), PlatformRule(
            SocialPlatformIcons.KEY_WEIBO,
            "微博",
            setOf("weibo.com", "weibo.cn"),
            setOf("sinaweibo", "weibo")
        ), PlatformRule(
            SocialPlatformIcons.KEY_X, "X", setOf("x.com", "twitter.com"), setOf("twitter")
        ), PlatformRule(
            SocialPlatformIcons.KEY_FACEBOOK,
            "Facebook",
            setOf("facebook.com", "fb.com", "fb.me"),
            setOf("fb", "facebook")
        ), PlatformRule(
            SocialPlatformIcons.KEY_INSTAGRAM,
            "Instagram",
            setOf("instagram.com", "instagr.am"),
            setOf("instagram")
        ), PlatformRule(
            SocialPlatformIcons.KEY_TELEGRAM,
            "Telegram",
            setOf("t.me", "telegram.me", "telegram.org"),
            setOf("tg", "telegram")
        )
    )

    private val absoluteUrlRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")
    private val numericRegex = Regex("^\\d+$")

    fun detect(raw: String): PlatformMatch? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        val scheme = runCatching { java.net.URI(value).scheme }.getOrNull()?.lowercase()
        val normalized = if (value.startsWith("www.", ignoreCase = true)) {
            "https://$value"
        } else {
            value
        }
        val host = runCatching { java.net.URI(normalized).host }.getOrNull()?.lowercase()
            ?.removeSuffix(".")
        for (rule in rules) {
            val hostMatched = host != null && rule.hosts.any { suffix ->
                host == suffix || host.endsWith(".$suffix")
            }
            val schemeMatched = scheme != null && scheme in rule.schemes
            if (hostMatched || schemeMatched) {
                return PlatformMatch(rule.iconKey, rule.displayName)
            }
        }
        return null
    }

    fun resolve(iconKey: String, rawHandle: String): String? {
        val handle = rawHandle.trim()
        if (handle.isEmpty()) return null
        if (isAbsoluteUrl(handle)) return handle
        if (handle.startsWith("mailto:", ignoreCase = true)) return handle
        if (handle.startsWith("www.", ignoreCase = true)) return "https://$handle"

        return when (iconKey) {
            SocialPlatformIcons.KEY_QQ -> {
                if (!numericRegex.matches(handle)) null
                else "https://wpa.qq.com/msgrd?v=3&uin=${encodeQuery(handle)}&site=qq&menu=yes"
            }

            SocialPlatformIcons.KEY_WECHAT -> null

            SocialPlatformIcons.KEY_BILIBILI -> {
                if (numericRegex.matches(handle)) {
                    "https://space.bilibili.com/${encodePath(handle)}"
                } else {
                    "https://search.bilibili.com/all?keyword=${encodeQuery(handle)}"
                }
            }

            SocialPlatformIcons.KEY_DOUYIN -> {
                if (numericRegex.matches(handle)) {
                    "https://www.douyin.com/user/${encodePath(handle)}"
                } else {
                    "https://www.douyin.com/search/${encodePath(handle)}"
                }
            }

            SocialPlatformIcons.KEY_WEIBO -> {
                if (numericRegex.matches(handle)) {
                    "https://weibo.com/u/${encodePath(handle)}"
                } else {
                    "https://weibo.com/n/${encodePath(handle)}"
                }
            }

            SocialPlatformIcons.KEY_X -> {
                "https://x.com/${encodePath(handle.removePrefix("@"))}"
            }

            SocialPlatformIcons.KEY_FACEBOOK -> {
                "https://facebook.com/${encodePath(handle.removePrefix("@"))}"
            }

            SocialPlatformIcons.KEY_INSTAGRAM -> {
                "https://instagram.com/${encodePath(handle.removePrefix("@"))}"
            }

            SocialPlatformIcons.KEY_TELEGRAM -> {
                "https://t.me/${encodePath(handle.removePrefix("@"))}"
            }

            else -> null
        }
    }

    private fun isAbsoluteUrl(value: String): Boolean = absoluteUrlRegex.matches(value)

    private fun encodePath(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    }

    private fun encodeQuery(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
