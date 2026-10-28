package com.ziegler.kighelper.data.socialshare

import com.ziegler.kighelper.data.SocialPlatformIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SocialPlatformUrlResolverTest {

    @Test
    fun qqNumericHandleBuildsMessagingUrl() {
        assertEquals(
            "https://wpa.qq.com/msgrd?v=3&uin=123456&site=qq&menu=yes",
            SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_QQ, "123456")
        )
    }

    @Test
    fun bilibiliSupportsUidAndSearch() {
        assertEquals(
            "https://space.bilibili.com/2233",
            SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_BILIBILI, "2233")
        )
        assertEquals(
            "https://search.bilibili.com/all?keyword=%E6%B5%8B%E8%AF%95",
            SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_BILIBILI, "测试")
        )
    }

    @Test
    fun wechatHasNoStableProfileUrl() {
        assertNull(SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_WECHAT, "wxid_test"))
    }

    @Test
    fun absoluteUrlIsPassedThrough() {
        assertEquals(
            "https://example.com/me?id=1",
            SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_WECHAT, "https://example.com/me?id=1")
        )
    }

    @Test
    fun xAndTelegramStripAtSign() {
        assertEquals(
            "https://x.com/example",
            SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_X, "@example")
        )
        assertEquals(
            "https://t.me/example",
            SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_TELEGRAM, "@example")
        )
    }

    @Test
    fun customPlatformOnlyAcceptsUrl() {
        assertNull(SocialPlatformUrlResolver.resolve(SocialPlatformIcons.KEY_DEFAULT, "someone"))
        assertEquals(
            "https://example.com/someone",
            SocialPlatformUrlResolver.resolve(
                SocialPlatformIcons.KEY_DEFAULT,
                "https://example.com/someone"
            )
        )
    }

    @Test
    fun detectsSupportedPlatformUrls() {
        assertEquals(
            SocialPlatformIcons.KEY_QQ,
            SocialPlatformUrlResolver.detect("https://qm.qq.com/q/abc")?.iconKey
        )
        assertEquals(
            SocialPlatformIcons.KEY_WECHAT,
            SocialPlatformUrlResolver.detect("https://u.wechat.com/abc")?.iconKey
        )
        assertEquals(
            SocialPlatformIcons.KEY_BILIBILI,
            SocialPlatformUrlResolver.detect("https://b23.tv/abc")?.iconKey
        )
        assertEquals(
            SocialPlatformIcons.KEY_DOUYIN,
            SocialPlatformUrlResolver.detect("https://v.douyin.com/abc")?.iconKey
        )
        assertEquals(
            SocialPlatformIcons.KEY_WEIBO,
            SocialPlatformUrlResolver.detect("https://m.weibo.cn/abc")?.iconKey
        )
        assertEquals(
            SocialPlatformIcons.KEY_X,
            SocialPlatformUrlResolver.detect("https://x.com/abc")?.iconKey
        )
        assertEquals(
            SocialPlatformIcons.KEY_TELEGRAM,
            SocialPlatformUrlResolver.detect("https://t.me/abc")?.iconKey
        )
    }

    @Test
    fun detectsPlatformSchemesAndRejectsLookalikeHosts() {
        assertNotNull(SocialPlatformUrlResolver.detect("weixin://qrcode"))
        assertNull(SocialPlatformUrlResolver.detect("https://notqq.com/q/abc"))
        assertNull(SocialPlatformUrlResolver.detect("普通文本"))
    }

    @Test
    fun platformIdsMapBackToIconsAndNames() {
        assertEquals(
            SocialPlatformIcons.KEY_QQ,
            SocialPlatformShareIds.iconKeyOf(SocialPlatformShareIds.QQ)
        )
        assertEquals(
            SocialPlatformIcons.KEY_TELEGRAM,
            SocialPlatformShareIds.iconKeyOf(SocialPlatformShareIds.TELEGRAM)
        )
        assertEquals("微信", SocialPlatformShareIds.displayNameOf(SocialPlatformShareIds.WECHAT))
    }
}
