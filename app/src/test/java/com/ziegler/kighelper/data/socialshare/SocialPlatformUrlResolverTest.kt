package com.ziegler.kighelper.data.socialshare

import com.ziegler.kighelper.data.SocialPlatformIcons
import org.junit.Assert.assertEquals
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
}
