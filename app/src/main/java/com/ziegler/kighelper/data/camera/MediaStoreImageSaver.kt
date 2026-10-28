package com.ziegler.kighelper.data.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** 相册写入成功后的结果，URI 可直接交给图片加载器或分享。 */
data class SavedPhoto(
    val uri: Uri, val displayName: String
)

/**
 * 把相机缓存中的 JPEG 复制到 DCIM/KigHelper。
 *
 * Android 10+ 使用 MediaStore 的 IS_PENDING 流程，写入完成前不会出现在相册；
 * Android 9 及以下直接写公共目录并通知媒体扫描器。
 */
@Singleton
class MediaStoreImageSaver @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * 保存照片并生成显示名称。
     *
     * @param motionPhoto 实况照片会在文件名追加 `_MP`，便于用户识别。
     * @return 写入失败或源文件为空时返回 null。
     */
    suspend fun save(
        source: File, mimeType: String = "image/jpeg", motionPhoto: Boolean = false
    ): SavedPhoto? = withContext(Dispatchers.IO) {
        if (!source.exists() || source.length() <= 0L) return@withContext null
        val displayName = buildDisplayName(mimeType, motionPhoto)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(source, displayName, mimeType)
        } else {
            saveLegacy(source, displayName)
        }
    }

    /** 分区存储写入：先插入 pending 记录，复制成功后再公开。 */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveScoped(source: File, displayName: String, mimeType: String): SavedPhoto? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(
                MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/KigHelper"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("无法打开输出流")
            val clear = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, clear, null, null)
            SavedPhoto(uri, displayName)
        }.getOrElse {
            // 复制或公开失败时删除 pending 记录，避免相册留下不可用的空条目。
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    /** Android 9 及以下写公共 DCIM，并通知媒体扫描器刷新相册。 */
    @Suppress("DEPRECATION")
    private fun saveLegacy(source: File, displayName: String): SavedPhoto? {
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val directory = File(dcim, "KigHelper").apply { mkdirs() }
        val destination = uniqueFile(directory, displayName)
        return runCatching {
            source.inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            MediaScannerConnection.scanFile(
                context, arrayOf(destination.absolutePath), arrayOf("image/jpeg"), null
            )
            SavedPhoto(Uri.fromFile(destination), destination.name)
        }.getOrNull()
    }

    /** 旧版文件系统没有 MediaStore 去重，手动追加序号避免覆盖同名照片。 */
    private fun uniqueFile(directory: File, displayName: String): File {
        val base = displayName.substringBeforeLast('.')
        val extension = displayName.substringAfterLast('.', "jpg")
        var candidate = File(directory, displayName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "${base}_$index.$extension")
            index += 1
        }
        return candidate
    }

    /** 生成 `KigHelper_时间戳_MP.扩展名`，同一秒内由 MediaStore 自动处理重名。 */
    private fun buildDisplayName(mimeType: String, motionPhoto: Boolean): String {
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val suffix = if (motionPhoto) "_MP" else ""
        return "KigHelper_${stamp}$suffix.$extension"
    }
}

/**
 * 相机临时文件管理。
 *
 * 拍照先写应用缓存，保存到相册或失败后再删除，避免权限不足或进程中断时污染相册。
 */
object CameraCaptureFiles {
    /** 创建普通拍照的临时 JPEG。 */
    fun create(context: Context): File {
        val directory = File(context.cacheDir, "camera_capture").apply { mkdirs() }
        return File(directory, "capture_${System.currentTimeMillis()}.jpg")
    }

    /** 创建实况照片后录的临时 MP4。 */
    fun createVideo(context: Context): File {
        val directory = File(context.cacheDir, "camera_capture").apply { mkdirs() }
        return File(directory, "live_${System.currentTimeMillis()}.mp4")
    }

    /** 创建头像/缩略图裁剪的临时文件。 */
    fun createThumbnail(context: Context): File {
        val directory = File(context.cacheDir, "camera_capture").apply { mkdirs() }
        return File(directory, "thumbnail_${System.currentTimeMillis()}.jpg")
    }

    /** 删除单个临时文件；文件已不存在时静默忽略。 */
    fun cleanup(context: Context, file: File?) {
        file?.takeIf { it.exists() }?.delete()
    }

    /** 清理超过 24 小时的残留缓存，通常在相机页初始化时调用。 */
    fun cleanupOld(context: Context) {
        val directory = File(context.cacheDir, "camera_capture")
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        directory.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }
}
