package com.ziegler.kighelper.data.scan

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 扫码历史仓库。
 *
 * 历史记录量很小，使用单个 JSON 文件持久化；内存中的 StateFlow 是唯一读取入口，
 * 时光轴与相机页无需主动刷新。写入通过 Mutex 串行化，避免并发覆盖。
 */
@Singleton
class ScanHistoryRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val gson = Gson()
    private val file = File(context.filesDir, FILE_NAME)
    private val listType: Type = object : TypeToken<List<ScanRecord>>() {}.type
    private val writeMutex = Mutex()

    private val _records = MutableStateFlow(load())

    /** 完整历史，按时间倒序。 */
    val records: StateFlow<List<ScanRecord>> = _records.asStateFlow()

    /** 同步读取当前快照，供 ViewModel 初始化时避免额外收集。 */
    val current: List<ScanRecord> get() = _records.value

    /**
     * 新增一条记录并放到列表头部。
     *
     * 与其他应用扫码不同，连续扫描同一内容很常见；因此 1.5 秒内类型和内容
     * 都相同记录会被忽略，防止弹窗切换时重复入库。
     */
    suspend fun add(record: ScanRecord) {
        writeMutex.withLock {
            val previous = _records.value
            val latest = previous.firstOrNull()
            if (latest != null && latest.type == record.type && latest.rawContent == record.rawContent && abs(
                    latest.timestamp - record.timestamp
                ) < DUPLICATE_WINDOW_MS
            ) {
                return
            }
            persist((listOf(record) + previous).take(MAX_RECORDS))
        }
    }

    /** 按 ID 删除扫码记录；照片删除失败时也可只清掉对应历史。 */
    suspend fun delete(ids: Set<String>) {
        if (ids.isEmpty()) return
        writeMutex.withLock {
            persist(_records.value.filterNot { it.id in ids })
        }
    }

    /** 清空全部扫码历史，不影响相册照片。 */
    suspend fun clear() {
        writeMutex.withLock {
            persist(emptyList())
        }
    }

    /**
     * 先写临时文件再替换正式文件，降低写入中断导致 JSON 损坏的概率。
     */
    private suspend fun persist(records: List<ScanRecord>) = withContext(Dispatchers.IO) {
        val target = file
        val temp = File(target.parentFile, "$FILE_NAME.tmp")
        temp.writeText(gson.toJson(records, listType))
        if (!temp.renameTo(target)) {
            target.writeText(temp.readText())
            temp.delete()
        }
        _records.value = records
    }

    /** 读取历史文件；文件不存在或 JSON 损坏时返回空列表，保证应用可启动。 */
    private fun load(): List<ScanRecord> {
        if (!file.exists()) return emptyList()
        return runCatching {
            gson.fromJson<List<ScanRecord>>(file.readText(), listType) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val FILE_NAME = "scan_history.json"

        /** 最多保留 1000 条，避免历史文件无限增长。 */
        const val MAX_RECORDS = 1000

        /** 同一内容在此时间窗口内视为重复扫描。 */
        const val DUPLICATE_WINDOW_MS = 1500L
    }
}
