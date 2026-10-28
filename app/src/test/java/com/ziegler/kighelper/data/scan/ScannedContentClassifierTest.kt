package com.ziegler.kighelper.data.scan

import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.SocialPlatformIcons
import com.ziegler.kighelper.data.socialshare.SocialCardShareCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannedContentClassifierTest {

    @Test
    fun decodesKigHelperCardAndBuildsProfile() {
        val profile = SocialCardProfile(
            nickname = "测试昵称",
            signature = "测试签名",
            contacts = listOf(
                SocialContact(
                    id = "qq",
                    displayName = "QQ",
                    iconKey = SocialPlatformIcons.KEY_QQ,
                    handle = "123456",
                    qrContent = "https://qm.qq.com/q/example"
                )
            )
        )
        val url = SocialCardShareCodec.encodeQr(profile).url!!

        val result = ScannedContentClassifier.classify(url)

        assertTrue(result is ScannedContent.SocialCard)
        val card = result as ScannedContent.SocialCard
        assertEquals("测试昵称", card.profile.nickname)
        assertEquals("测试签名", card.profile.signature)
        assertEquals(1, card.profile.contacts.size)
        assertEquals("https://qm.qq.com/q/example", card.profile.contacts[0].qrContent)
    }

    @Test
    fun detectsPlatformContent() {
        val result = ScannedContentClassifier.classify("https://qm.qq.com/q/group")

        assertTrue(result is ScannedContent.Platform)
        assertEquals(
            SocialPlatformIcons.KEY_QQ,
            (result as ScannedContent.Platform).match.iconKey
        )
    }

    @Test
    fun keepsUnknownContentAsText() {
        val result = ScannedContentClassifier.classify("hello world")

        assertEquals(ScannedContent.Text("hello world"), result)
    }

    @Test
    fun malformedCardUrlFallsBackToText() {
        val result = ScannedContentClassifier.classify("https://example.com/card/#not-a-payload")

        assertTrue(result is ScannedContent.Text)
    }

    @Test
    fun cardRecordCanBeRestoredFromHistory() {
        val profile = SocialCardProfile(
            nickname = "历史卡片",
            signature = "",
            contacts = listOf(
                SocialContact(
                    id = "bili",
                    displayName = "B站",
                    iconKey = SocialPlatformIcons.KEY_BILIBILI,
                    handle = "2233"
                )
            )
        )
        val url = SocialCardShareCodec.encodeQr(profile).url!!
        val content = ScannedContentClassifier.classify(url)
        val record = content.toScanRecord()

        assertEquals(ScanRecordType.SOCIAL_CARD, record.type)
        assertEquals("历史卡片", record.title)
        assertFalse(record.cardUrl.isNullOrBlank())

        val restored = ScannedContentClassifier.fromRecord(record)
        assertTrue(restored is ScannedContent.SocialCard)
        assertEquals("历史卡片", (restored as ScannedContent.SocialCard).profile.nickname)
    }
}
