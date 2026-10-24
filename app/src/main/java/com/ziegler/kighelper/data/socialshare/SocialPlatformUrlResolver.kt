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
}

/**
 * 按第三方平台规则把账号拼成完整 URL。
 *
 * 只在账号本身不是 URL 时参与拼接；无法可靠拼接的平台（如微信）返回 null，
 * 前端将只展示账号文本而不生成二维码。
 */
object SocialPlatformUrlResolver {

    private val absoluteUrlRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")
    private val numericRegex = Regex("^\\d+$")

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
