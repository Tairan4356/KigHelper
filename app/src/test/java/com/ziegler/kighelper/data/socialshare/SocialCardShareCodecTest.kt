package com.ziegler.kighelper.data.socialshare

import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.SocialPlatformIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Random

class SocialCardShareCodecTest {

    private fun incompressibleAscii(length: Int): String {
        val bytes = ByteArray(length)
        Random(42).nextBytes(bytes)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun incompressibleCjk(length: Int): String {
        val random = Random(7)
        return buildString(length) {
            repeat(length) {
                append((0x4E00 + random.nextInt(0x4000)).toChar())
            }
        }
    }

    private fun profile(
        nickname: String = "测试昵称",
        signature: String = "这是一段签名",
        contacts: List<SocialContact> = emptyList()
    ) = SocialCardProfile(
        nickname = nickname,
        signature = signature,
        contacts = contacts
    )

    @Test
    fun roundTripKeepsCardAndContacts() {
        val source = profile(
            contacts = listOf(
                SocialContact(
                    id = "qq",
                    displayName = "QQ",
                    iconKey = SocialPlatformIcons.KEY_QQ,
                    handle = "123456",
                    qrContent = "https://qm.qq.com/q/example"
                ),
                SocialContact(
                    id = "bili",
                    displayName = "B站",
                    iconKey = SocialPlatformIcons.KEY_BILIBILI,
                    handle = "2233"
                )
            )
        )
        val result = SocialCardShareCodec.encodeQr(source)
        assertNotNull(result.url)
        assertTrue(result.urlBytes <= SocialCardShareCodec.MAX_QR_URL_BYTES)

        val decoded = SocialCardShareCodec.decodeUrl(result.url!!)
        assertNotNull(decoded)
        decoded!!
        assertEquals(source.nickname, decoded.nickname)
        assertEquals(source.signature, decoded.signature)
        assertEquals(2, decoded.contacts.size)
        assertEquals("https://qm.qq.com/q/example", decoded.contacts[0].target)
        assertTrue(decoded.contacts[0].targetFromQr)
        assertEquals("https://space.bilibili.com/2233", decoded.contacts[1].target)
        assertFalse(decoded.contacts[1].targetFromQr)
    }

    @Test
    fun signatureIsTruncatedToFitLimit() {
        val source = profile(
            nickname = "很长很长的昵称用于测试",
            signature = incompressibleCjk(180),
            contacts = listOf(
                SocialContact(
                    id = "qq",
                    displayName = "QQ",
                    iconKey = SocialPlatformIcons.KEY_QQ,
                    handle = "123456789",
                    qrContent = "https://qm.qq.com/q/" + incompressibleAscii(20)
                )
            )
        )
        val result = SocialCardShareCodec.encodeQr(source)
        assertFalse(result.overflow)
        assertNotNull(result.url)
        assertTrue(result.urlBytes <= SocialCardShareCodec.MAX_QR_URL_BYTES)
        assertTrue(result.signatureTruncated)
        assertTrue(result.signature.endsWith("…"))
        assertTrue(result.signatureCodePoints < source.signature.length)
    }

    @Test
    fun oversizedContactCausesOverflow() {
        val source = profile(
            signature = "",
            contacts = listOf(
                SocialContact(
                    id = "big",
                    displayName = "大内容",
                    handle = "x",
                    qrContent = incompressibleAscii(1500)
                )
            )
        )
        val result = SocialCardShareCodec.encodeQr(source)
        assertTrue(result.overflow)
        assertNull(result.url)
        assertTrue(result.urlBytes > SocialCardShareCodec.MAX_QR_URL_BYTES)
    }

    @Test
    fun linkModeIsNotLimitedByQrCapacity() {
        val source = profile(
            signature = incompressibleCjk(300),
            contacts = listOf(
                SocialContact(
                    id = "big",
                    displayName = "大内容",
                    handle = "x",
                    qrContent = incompressibleAscii(1500)
                )
            )
        )
        val result = SocialCardShareCodec.encodeLink(source)
        assertFalse(result.overflow)
        assertNotNull(result.url)
        assertTrue(result.urlBytes > SocialCardShareCodec.MAX_QR_URL_BYTES)
        assertEquals(SocialCardShareCodec.MAX_LINK_SIGNATURE_CODE_POINTS, result.signatureCodePoints)
    }

    @Test
    fun truncationFlagsReflectActualChanges() {
        val natural = profile(nickname = "昵称…", signature = "签名…")
        val naturalPayload = SocialCardShareCodec.decodeUrl(
            SocialCardShareCodec.encodeLink(natural).url!!
        )!!
        assertFalse(naturalPayload.nicknameTruncated)
        assertFalse(naturalPayload.signatureTruncated)

        val long = profile(nickname = "长".repeat(60), signature = "签".repeat(250))
        val longPayload = SocialCardShareCodec.decodeUrl(
            SocialCardShareCodec.encodeLink(long).url!!
        )!!
        assertTrue(longPayload.nicknameTruncated)
        assertTrue(longPayload.signatureTruncated)
        assertEquals(
            SocialCardShareCodec.MAX_NICKNAME_CODE_POINTS,
            longPayload.nickname.codePointCount(0, longPayload.nickname.length)
        )
        assertEquals(
            SocialCardShareCodec.MAX_LINK_SIGNATURE_CODE_POINTS,
            longPayload.signature.codePointCount(0, longPayload.signature.length)
        )
    }

    @Test
    fun corruptedPayloadIsRejected() {
        val result = SocialCardShareCodec.encodeQr(profile())
        val url = result.url!!
        val corrupted = url.dropLast(4) + "AAAA"
        assertNull(SocialCardShareCodec.decodeUrl(corrupted))
    }

    @Test
    fun decodeAllowsCustomMutation() {
        val result = SocialCardShareCodec.encodeQr(profile(), maxUrlBytes = 400)
        assertNotNull(result.url)
        assertEquals(400, result.maxUrlBytes)
        assertTrue(result.url!!.toByteArray(StandardCharsets.UTF_8).size <= 400)
    }
}
