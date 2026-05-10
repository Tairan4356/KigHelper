package com.ziegler.kighelper.utils

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class UpdateConfig(
    val versionCode: Int,
    val versionName: String,
    val updateContent: String,
    val downloadUrl: String
)

/**
 * 更新管理器：负责从远程 JSON 获取版本信息并与本地对比
 */
object UpdateManager {
    private const val TAG = "UpdateManager"
    private val UPDATE_URLS = listOf(
        "https://gitee.com/tairan_4356/kig-helper/raw/master/update.json",
        "https://raw.giteeusercontent.com/tairan_4356/kig-helper/raw/master/update.json"
    )
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .dns(UpdateDns)
        .build()

    suspend fun checkUpdate(context: Context): UpdateConfig? {
        return withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val currentVersionCode = getAppVersionCode(appContext)
            for (url in UPDATE_URLS) {
                val config = fetchUpdateConfig(url) ?: continue
                if (config.versionCode.toLong() > currentVersionCode) {
                    return@withContext config
                }
            }
            null
        }
    }

    private fun fetchUpdateConfig(url: String): UpdateConfig? {
        return try {
            val request = Request.Builder().url(url).header("Accept", "application/json")
                .header("Cache-Control", "no-cache").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "检查更新失败：$url HTTP ${response.code}")
                    return null
                }

                parseUpdateConfig(response.body.string())
            }
        } catch (error: Exception) {
            Log.w(TAG, "检查更新失败：$url", error)
            null
        }
    }

    private fun parseUpdateConfig(json: String): UpdateConfig {
        val jsonObject = JSONObject(json)
        return UpdateConfig(
            versionCode = jsonObject.getInt("versionCode"),
            versionName = jsonObject.getString("versionName"),
            updateContent = jsonObject.getString("updateContent"),
            downloadUrl = jsonObject.getString("downloadUrl")
        )
    }

    private fun getAppVersionCode(context: Context): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(
                context.packageName, 0
            ).versionCode.toLong()
        }
    }

    private object UpdateDns : Dns {
        private const val DNS_PORT = 53
        private const val DNS_TIMEOUT_MS = 2_000
        private const val DNS_TYPE_A = 1
        private val updateHosts = setOf("gitee.com", "raw.giteeusercontent.com")
        private val dnsServers = listOf(
            byteArrayOf(223.toByte(), 5, 5, 5),
            byteArrayOf(114, 114, 114, 114),
            byteArrayOf(8, 8, 8, 8)
        )

        override fun lookup(hostname: String): List<InetAddress> {
            if (hostname !in updateHosts) {
                return Dns.SYSTEM.lookup(hostname)
            }

            val resolvedAddresses = resolveByUdp(hostname)
            if (resolvedAddresses.isNotEmpty()) {
                return resolvedAddresses
            }

            return Dns.SYSTEM.lookup(hostname)
        }

        private fun resolveByUdp(hostname: String): List<InetAddress> {
            for (dnsServer in dnsServers) {
                try {
                    val addresses = queryDnsServer(hostname, dnsServer)
                    if (addresses.isNotEmpty()) {
                        return addresses.distinctBy { it.hostAddress }
                    }
                } catch (error: Exception) {
                    Log.w(TAG, "DNS 解析失败：$hostname @ ${formatIp(dnsServer)}", error)
                }
            }

            return emptyList()
        }

        private fun queryDnsServer(hostname: String, dnsServer: ByteArray): List<InetAddress> {
            val transactionId = Random.nextInt(0, 0x10000)
            val query = buildDnsQuery(hostname, transactionId)
            val responseBuffer = ByteArray(512)

            DatagramSocket().use { socket ->
                socket.soTimeout = DNS_TIMEOUT_MS
                socket.send(
                    DatagramPacket(
                        query,
                        query.size,
                        InetAddress.getByAddress(dnsServer),
                        DNS_PORT
                    )
                )

                val response = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(response)
                return parseDnsResponse(
                    responseBuffer.copyOf(response.length),
                    transactionId,
                    hostname
                )
            }
        }

        private fun buildDnsQuery(hostname: String, transactionId: Int): ByteArray {
            return ByteArrayOutputStream().apply {
                writeShort(transactionId)
                writeShort(0x0100)
                writeShort(1)
                writeShort(0)
                writeShort(0)
                writeShort(0)

                hostname.split('.').forEach { label ->
                    write(label.length)
                    write(label.toByteArray(Charsets.UTF_8))
                }
                write(0)
                writeShort(DNS_TYPE_A)
                writeShort(1)
            }.toByteArray()
        }

        private fun parseDnsResponse(
            response: ByteArray,
            transactionId: Int,
            hostname: String
        ): List<InetAddress> {
            if (response.size < 12 || readUnsignedShort(response, 0) != transactionId) {
                throw UnknownHostException("Invalid DNS response for $hostname")
            }

            val responseCode = response[3].toInt() and 0x0f
            if (responseCode != 0) {
                throw UnknownHostException("DNS response code $responseCode for $hostname")
            }

            val questionCount = readUnsignedShort(response, 4)
            val answerCount = readUnsignedShort(response, 6)
            var offset = 12

            repeat(questionCount) {
                offset = skipDnsName(response, offset) + 4
            }

            val addresses = mutableListOf<InetAddress>()
            repeat(answerCount) {
                offset = skipDnsName(response, offset)
                if (offset + 10 > response.size) return@repeat

                val type = readUnsignedShort(response, offset)
                val recordClass = readUnsignedShort(response, offset + 2)
                val dataLength = readUnsignedShort(response, offset + 8)
                offset += 10

                if (offset + dataLength > response.size) return@repeat
                if (type == DNS_TYPE_A && recordClass == 1 && dataLength == 4) {
                    addresses += InetAddress.getByAddress(
                        hostname,
                        response.copyOfRange(offset, offset + dataLength)
                    )
                }
                offset += dataLength
            }

            return addresses
        }

        private fun skipDnsName(data: ByteArray, startOffset: Int): Int {
            var offset = startOffset

            while (offset < data.size) {
                val length = data[offset].toInt() and 0xff
                if ((length and 0xc0) == 0xc0) return offset + 2
                if (length == 0) return offset + 1

                offset += length + 1
            }

            throw UnknownHostException("Invalid DNS name")
        }

        private fun readUnsignedShort(data: ByteArray, offset: Int): Int {
            return ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)
        }

        private fun ByteArrayOutputStream.writeShort(value: Int) {
            write((value ushr 8) and 0xff)
            write(value and 0xff)
        }

        private fun formatIp(address: ByteArray): String {
            return address.joinToString(".") { (it.toInt() and 0xff).toString() }
        }
    }
}
