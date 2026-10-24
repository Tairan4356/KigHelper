package com.ziegler.kighelper.data.socialshare

import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater

/**
 * 扩列卡片 URL 传输协议。
 *
 * 数据流：紧凑二进制 -> raw DEFLATE -> Base64URL -> 静态页地址 fragment。
 * 二维码模式默认限制 240 字节，可通过调用方参数或修改 [MAX_QR_URL_BYTES] 调整；
 * 链接分享模式不限制 URL 长度。
 */
object SocialCardShareCodec {

    /** 静态页地址前缀；修改部署地址时只改这里。 */
    const val BASE_URL = "https://kighelper.pages.dev/card/#"

    /** 集合码 URL 默认最大字节数，后续可直接手动调整。 */
    const val MAX_QR_URL_BYTES = 512

    /** 链接分享时签名最大字符数（码点），超出追加省略号。 */
    const val MAX_LINK_SIGNATURE_CODE_POINTS = 200

    /** 昵称最大字符数（码点），防止异常数据无限增长。 */
    const val MAX_NICKNAME_CODE_POINTS = 48

    const val SCHEMA_VERSION = 1

    private const val MAX_DECOMPRESSED_BYTES = 64 * 1024
    private const val MAX_FIELD_BYTES = 16 * 1024
    private const val MAX_CONTACTS = 64
    private const val ELLIPSIS = "…"

    private const val PAYLOAD_FLAG_HAS_NICKNAME = 1 shl 0
    private const val PAYLOAD_FLAG_HAS_SIGNATURE = 1 shl 1
    private const val PAYLOAD_FLAG_SIGNATURE_TRUNCATED = 1 shl 2
    private const val PAYLOAD_FLAG_NICKNAME_TRUNCATED = 1 shl 3

    private const val CONTACT_FLAG_HAS_NAME = 1 shl 0
    private const val CONTACT_FLAG_HAS_HANDLE = 1 shl 1
    private const val CONTACT_FLAG_HAS_TARGET = 1 shl 2
    private const val CONTACT_FLAG_TARGET_FROM_QR = 1 shl 3

    data class ShareContact(
        val platformId: Int,
        val displayName: String,
        val handle: String,
        val target: String?,
        val targetFromQr: Boolean
    )

    data class SharePayload(
        val nickname: String,
        val signature: String,
        val signatureTruncated: Boolean,
        val nicknameTruncated: Boolean,
        val contacts: List<ShareContact>
    )

    data class ShareEncodeResult(
        val url: String?,
        val urlBytes: Int,
        val maxUrlBytes: Int?,
        val overflow: Boolean,
        val signature: String,
        val signatureTruncated: Boolean,
        val signatureCodePoints: Int,
        val nickname: String,
        val nicknameTruncated: Boolean,
        val contactCount: Int
    )

    private data class EncodedPayload(
        val encoded: String, val urlBytes: Int, val signature: String
    )

    fun encodeQr(
        profile: SocialCardProfile, maxUrlBytes: Int = MAX_QR_URL_BYTES
    ): ShareEncodeResult {
        val nickname = trimmedNickname(profile)
        val contacts = shareContacts(profile)
        val originalSignature = profile.signature.trim()
        val nicknameTruncated = nickname != profile.nickname.trim()
        val maxSignature = originalSignature.codePointCount(0, originalSignature.length)
            .coerceAtMost(MAX_LINK_SIGNATURE_CODE_POINTS)

        val fullSignature = signaturePrefix(originalSignature, maxSignature)
        val full = encodePayload(
            nickname = nickname,
            signature = fullSignature,
            contacts = contacts,
            nicknameTruncated = nicknameTruncated,
            signatureTruncated = fullSignature != originalSignature
        )
        if (full.urlBytes <= maxUrlBytes) {
            return result(
                encoded = full,
                maxUrlBytes = maxUrlBytes,
                overflow = false,
                nickname = nickname,
                nicknameTruncated = nicknameTruncated,
                originalSignature = originalSignature,
                contacts = contacts
            )
        }

        var best: EncodedPayload? = null
        var low = 0
        var high = maxSignature
        while (low <= high) {
            val mid = (low + high) ushr 1
            val candidateSignature = signaturePrefix(originalSignature, mid)
            val candidate = encodePayload(
                nickname = nickname,
                signature = candidateSignature,
                contacts = contacts,
                nicknameTruncated = nicknameTruncated,
                signatureTruncated = candidateSignature != originalSignature
            )
            if (candidate.urlBytes <= maxUrlBytes) {
                best = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        if (best != null) {
            return result(
                encoded = best,
                maxUrlBytes = maxUrlBytes,
                overflow = false,
                nickname = nickname,
                nicknameTruncated = nicknameTruncated,
                originalSignature = originalSignature,
                contacts = contacts
            )
        }

        val withoutSignature = encodePayload(
            nickname = nickname,
            signature = null,
            contacts = contacts,
            nicknameTruncated = nicknameTruncated,
            signatureTruncated = originalSignature.isNotEmpty()
        )
        if (withoutSignature.urlBytes <= maxUrlBytes) {
            return result(
                encoded = withoutSignature,
                maxUrlBytes = maxUrlBytes,
                overflow = false,
                nickname = nickname,
                nicknameTruncated = nicknameTruncated,
                originalSignature = originalSignature,
                contacts = contacts
            )
        }

        return ShareEncodeResult(
            url = null,
            urlBytes = withoutSignature.urlBytes,
            maxUrlBytes = maxUrlBytes,
            overflow = true,
            signature = "",
            signatureTruncated = originalSignature.isNotEmpty(),
            signatureCodePoints = 0,
            nickname = nickname,
            nicknameTruncated = nicknameTruncated,
            contactCount = contacts.size
        )
    }

    fun encodeLink(profile: SocialCardProfile): ShareEncodeResult {
        val nickname = trimmedNickname(profile)
        val nicknameTruncated = nickname != profile.nickname.trim()
        val originalSignature = profile.signature.trim()
        val maxSignature = originalSignature.codePointCount(0, originalSignature.length)
            .coerceAtMost(MAX_LINK_SIGNATURE_CODE_POINTS)
        val signature = signaturePrefix(originalSignature, maxSignature)
        val contacts = shareContacts(profile)
        val encoded = encodePayload(
            nickname = nickname,
            signature = signature,
            contacts = contacts,
            nicknameTruncated = nicknameTruncated,
            signatureTruncated = signature != originalSignature
        )
        return result(
            encoded = encoded,
            maxUrlBytes = null,
            overflow = false,
            nickname = nickname,
            nicknameTruncated = nicknameTruncated,
            originalSignature = originalSignature,
            contacts = contacts
        )
    }

    /**
     * 识别/构造单个联系人的传输目标。
     * 优先使用二维码识别文本；识别失败时按平台规则用账号拼接 URL。
     */
    fun shareContact(contact: SocialContact): ShareContact {
        val handle = contact.handle.trim()
        val qrContent = contact.qrContent?.trim()?.takeIf { it.isNotEmpty() }
        val resolvedUrl = if (qrContent == null) {
            SocialPlatformUrlResolver.resolve(contact.iconKey, handle)
        } else {
            null
        }
        return ShareContact(
            platformId = SocialPlatformShareIds.of(contact.iconKey),
            displayName = contact.displayName.trim().ifBlank { "社交平台" },
            handle = handle,
            target = qrContent ?: resolvedUrl,
            targetFromQr = qrContent != null
        )
    }

    fun shareContacts(profile: SocialCardProfile): List<ShareContact> {
        return profile.aggregateContacts.take(MAX_CONTACTS).map(::shareContact)
    }

    /** 解析静态页 URL 或 fragment payload，主要供测试与后续深链使用。 */
    fun decodeUrl(url: String): SharePayload? {
        val payload = url.substringAfterLast('#', url)
        return decodePayload(payload)
    }

    fun decodePayload(encoded: String): SharePayload? {
        return runCatching {
            val compressed = base64UrlDecode(encoded)
            val inflated = inflate(compressed)
            if (inflated.size < 5) return null
            val contentSize = inflated.size - 4
            val expectedCrc = readIntBigEndian(inflated, contentSize)
            val crc = CRC32().apply { update(inflated, 0, contentSize) }.value.toInt()
            if (crc != expectedCrc) return null
            parsePayload(inflated, contentSize)
        }.getOrNull()
    }

    private fun result(
        encoded: EncodedPayload,
        maxUrlBytes: Int?,
        overflow: Boolean,
        nickname: String,
        nicknameTruncated: Boolean,
        originalSignature: String,
        contacts: List<ShareContact>
    ): ShareEncodeResult {
        return ShareEncodeResult(
            url = if (overflow) null else encoded.encoded,
            urlBytes = encoded.urlBytes,
            maxUrlBytes = maxUrlBytes,
            overflow = overflow,
            signature = encoded.signature,
            signatureTruncated = encoded.signature != originalSignature,
            signatureCodePoints = encoded.signature.codePointCount(0, encoded.signature.length),
            nickname = nickname,
            nicknameTruncated = nicknameTruncated,
            contactCount = contacts.size
        )
    }

    private fun trimmedNickname(profile: SocialCardProfile): String {
        return signaturePrefix(
            profile.nickname.trim(),
            profile.nickname.trim().codePointCount(0, profile.nickname.trim().length)
                .coerceAtMost(MAX_NICKNAME_CODE_POINTS)
        )
    }

    private fun signaturePrefix(value: String, count: Int): String {
        if (value.isEmpty() || count <= 0) return ""
        val total = value.codePointCount(0, value.length)
        if (count >= total) return value
        val end = value.offsetByCodePoints(0, count - 1)
        return value.substring(0, end) + ELLIPSIS
    }

    private fun encodePayload(
        nickname: String,
        signature: String?,
        contacts: List<ShareContact>,
        nicknameTruncated: Boolean = false,
        signatureTruncated: Boolean = false
    ): EncodedPayload {
        val raw = buildRawPayload(
            nickname = nickname,
            signature = signature,
            contacts = contacts,
            nicknameTruncated = nicknameTruncated,
            signatureTruncated = signatureTruncated
        )
        val compressed = deflate(raw)
        val encoded = BASE_URL + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        return EncodedPayload(
            encoded = encoded,
            urlBytes = encoded.toByteArray(StandardCharsets.UTF_8).size,
            signature = signature.orEmpty()
        )
    }

    private fun buildRawPayload(
        nickname: String,
        signature: String?,
        contacts: List<ShareContact>,
        nicknameTruncated: Boolean,
        signatureTruncated: Boolean
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(SCHEMA_VERSION)
        var payloadFlags = 0
        if (nickname.isNotEmpty()) payloadFlags = payloadFlags or PAYLOAD_FLAG_HAS_NICKNAME
        if (!signature.isNullOrEmpty()) {
            payloadFlags = payloadFlags or PAYLOAD_FLAG_HAS_SIGNATURE
            if (signatureTruncated) {
                payloadFlags = payloadFlags or PAYLOAD_FLAG_SIGNATURE_TRUNCATED
            }
        }
        if (nicknameTruncated) {
            payloadFlags = payloadFlags or PAYLOAD_FLAG_NICKNAME_TRUNCATED
        }
        out.write(payloadFlags)
        out.write(contacts.size.coerceAtMost(MAX_CONTACTS))
        writeString(out, nickname)
        writeString(out, signature.orEmpty())

        for (contact in contacts.take(MAX_CONTACTS)) {
            out.write(contact.platformId and 0xFF)
            var flags = 0
            if (contact.displayName.isNotEmpty()) flags = flags or CONTACT_FLAG_HAS_NAME
            if (contact.handle.isNotEmpty()) flags = flags or CONTACT_FLAG_HAS_HANDLE
            if (!contact.target.isNullOrEmpty()) {
                flags = flags or CONTACT_FLAG_HAS_TARGET
                if (contact.targetFromQr) flags = flags or CONTACT_FLAG_TARGET_FROM_QR
            }
            out.write(flags)
            writeString(out, contact.displayName)
            writeString(out, contact.handle)
            writeString(out, contact.target.orEmpty())
        }

        val bytes = out.toByteArray()
        val crc = CRC32().apply { update(bytes) }.value.toInt()
        val result = ByteArray(bytes.size + 4)
        System.arraycopy(bytes, 0, result, 0, bytes.size)
        writeIntBigEndian(result, bytes.size, crc)
        return result
    }

    private fun parsePayload(bytes: ByteArray, size: Int): SharePayload? {
        var index = 0

        fun require(count: Int): Boolean = index + count <= size

        fun readByte(): Int {
            if (!require(1)) throw IllegalArgumentException("truncated")
            return bytes[index++].toInt() and 0xFF
        }

        fun readVarInt(): Int {
            var shift = 0
            var value = 0
            while (shift < 28) {
                val b = readByte()
                value = value or ((b and 0x7F) shl shift)
                if (b and 0x80 == 0) return value
                shift += 7
            }
            throw IllegalArgumentException("invalid varint")
        }

        fun readString(): String {
            val length = readVarInt()
            if (length !in 0..MAX_FIELD_BYTES || !require(length)) {
                throw IllegalArgumentException("invalid field length")
            }
            val value = String(bytes, index, length, StandardCharsets.UTF_8)
            index += length
            return value
        }

        val version = readByte()
        if (version != SCHEMA_VERSION) return null
        val flags = readByte()
        val count = readByte().coerceAtMost(MAX_CONTACTS)
        val nickname = readString()
        val signature = readString()
        val contacts = ArrayList<ShareContact>(count)
        repeat(count) {
            val platformId = readByte()
            val contactFlags = readByte()
            val displayName = readString()
            val handle = readString()
            val target = readString().takeIf { it.isNotEmpty() }
            contacts += ShareContact(
                platformId = platformId,
                displayName = displayName,
                handle = handle,
                target = target,
                targetFromQr = contactFlags and CONTACT_FLAG_TARGET_FROM_QR != 0
            )
        }
        if (index != size) return null
        return SharePayload(
            nickname = nickname,
            signature = signature,
            signatureTruncated = flags and PAYLOAD_FLAG_SIGNATURE_TRUNCATED != 0,
            nicknameTruncated = flags and PAYLOAD_FLAG_NICKNAME_TRUNCATED != 0,
            contacts = contacts
        )
    }

    private fun writeString(out: ByteArrayOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun writeVarInt(out: ByteArrayOutputStream, value: Int) {
        var current = value
        while (current and 0x7F.inv() != 0) {
            out.write((current and 0x7F) or 0x80)
            current = current ushr 7
        }
        out.write(current)
    }

    private fun deflate(raw: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            DeflaterOutputStream(output, deflater).use { it.write(raw) }
        } finally {
            deflater.end()
        }
        return output.toByteArray()
    }

    private fun inflate(compressed: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        val inflater = Inflater(true)
        try {
            inflater.setInput(compressed)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    throw IllegalArgumentException("truncated payload")
                }
                output.write(buffer, 0, count)
                if (output.size() > MAX_DECOMPRESSED_BYTES) {
                    throw IllegalArgumentException("payload too large")
                }
            }
        } finally {
            inflater.end()
        }
        return output.toByteArray()
    }

    private fun base64UrlDecode(value: String): ByteArray {
        if (value.isEmpty()) throw IllegalArgumentException("empty payload")
        val padded = buildString {
            append(value.replace('-', '+').replace('_', '/'))
            val remainder = length % 4
            if (remainder > 0) repeat(4 - remainder) { append('=') }
        }
        return Base64.getDecoder().decode(padded)
    }

    private fun readIntBigEndian(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or ((bytes[offset + 1].toInt() and 0xFF) shl 16) or ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun writeIntBigEndian(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 24).toByte()
        bytes[offset + 1] = (value ushr 16).toByte()
        bytes[offset + 2] = (value ushr 8).toByte()
        bytes[offset + 3] = value.toByte()
    }
}
