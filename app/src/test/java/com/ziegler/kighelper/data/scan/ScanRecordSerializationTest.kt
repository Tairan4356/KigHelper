package com.ziegler.kighelper.data.scan

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanRecordSerializationTest {

    @Test
    fun recordsRoundTripThroughGson() {
        val records = listOf(
            ScanRecord(
                id = "card",
                timestamp = 123L,
                type = ScanRecordType.SOCIAL_CARD,
                rawContent = "https://example.com/card/#payload",
                title = "昵称",
                subtitle = "签名",
                cardUrl = "https://example.com/card/#payload"
            ),
            ScanRecord(
                id = "platform",
                timestamp = 456L,
                type = ScanRecordType.PLATFORM,
                rawContent = "https://qm.qq.com/q/abc",
                title = "QQ",
                subtitle = "https://qm.qq.com/q/abc",
                platformId = 1
            )
        )
        val type = object : TypeToken<List<ScanRecord>>() {}.type
        val json = Gson().toJson(records, type)

        val restored: List<ScanRecord> = Gson().fromJson(json, type)

        assertEquals(records, restored)
    }
}
