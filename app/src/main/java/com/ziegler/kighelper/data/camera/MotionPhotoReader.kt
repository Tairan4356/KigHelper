package com.ziegler.kighelper.data.camera

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 实况照片中内嵌视频的长度，视频固定在 JPEG 文件末尾。 */
data class MotionPhotoInfo(
    val videoLength: Long
)

/**
 * 解析 Motion Photo 的 XMP 元数据。
 *
 * 同时支持 Google 新版 `Container:Item` 结构（Item:Length）和旧版
 * `GCamera:MicroVideoOffset`，以兼容不同厂商相机生成的文件。
 */
object MotionPhotoMetadata {
    private val itemTag = Regex("""<(?:\w+:)?Item\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val semanticAttribute =
        Regex("""(?:Item:)?Semantic\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
    private val lengthAttribute =
        Regex("""(?:Item:)?Length\s*=\s*"(\d+)"""", RegexOption.IGNORE_CASE)
    private val mimeAttribute = Regex("""(?:Item:)?Mime\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
    private val microVideoOffset =
        Regex("""GCamera:MicroVideoOffset\s*=\s*"(\d+)"""", RegexOption.IGNORE_CASE)

    /**
     * 从 XMP 中提取视频长度。
     *
     * 长度必须大于 0 且小于整张照片大小，否则视为损坏元数据。
     */
    fun videoLength(xmp: String?, fileSize: Long): Long? {
        if (xmp.isNullOrBlank() || fileSize <= 1L) return null

        itemTag.findAll(xmp).forEach { match ->
            val tag = match.value
            val semantic = semanticAttribute.find(tag)?.groupValues?.getOrNull(1)
            val mime = mimeAttribute.find(tag)?.groupValues?.getOrNull(1)
            // 优先按 Semantic 判断；部分设备只写 video/* MIME，也一并接受。
            val isMotion = semantic.equals("MotionPhoto", ignoreCase = true) || mime?.startsWith(
                "video/",
                ignoreCase = true
            ) == true
            if (!isMotion) return@forEach
            val length = lengthAttribute.find(tag)?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (length.isValidVideoLength(fileSize)) return length
        }

        // 旧版 MicroVideo 元数据没有 Item 列表，只能依赖固定偏移字段。
        val legacyLength = microVideoOffset.find(xmp)?.groupValues?.getOrNull(1)?.toLongOrNull()
        return legacyLength.takeIf { it.isValidVideoLength(fileSize) }
    }

    private fun Long?.isValidVideoLength(fileSize: Long): Boolean {
        return this != null && this > 0L && this < fileSize
    }
}

/**
 * 读取并提取相册中的实况视频。
 *
 * 读取阶段只取文件大小和 XMP；真正播放时才把末尾视频复制到缓存。
 */
class MotionPhotoReader(
    private val context: Context
) {
    /** 读取 XMP 并返回视频长度；不是实况照片或元数据损坏时返回 null。 */
    suspend fun read(uri: Uri): MotionPhotoInfo? = withContext(Dispatchers.IO) {
        val fileSize = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        }.getOrNull() ?: return@withContext null
        if (fileSize <= 1L) return@withContext null
        val xmp = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttribute(ExifInterface.TAG_XMP)
            }
        }.getOrNull()
        MotionPhotoMetadata.videoLength(xmp, fileSize)?.let(::MotionPhotoInfo)
    }

    /**
     * 把 JPEG 末尾的 MP4 复制到缓存。
     *
     * 缓存名包含图片 ID 和视频长度，同一张图重复播放时直接命中；先写临时文件
     * 再重命名，避免播放到不完整的视频。
     */
    suspend fun extract(
        uri: Uri, info: MotionPhotoInfo, key: Long
    ): File? = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "motion_photo").apply { mkdirs() }
        val target = File(directory, "motion_${key}_${info.videoLength}.mp4")
        if (target.exists() && target.length() == info.videoLength) return@withContext target

        val temporary = File(directory, "${target.name}.tmp")
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val fileSize = descriptor.statSize
                // 视频紧跟在 JPEG 数据之后，从文件末尾按长度反推起点。
                val start = fileSize - info.videoLength
                if (start < 0L) error("invalid motion photo")
                FileInputStream(descriptor.fileDescriptor).use { input ->
                    input.channel.position(start)
                    FileOutputStream(temporary).use { output ->
                        input.copyTo(output, DEFAULT_BUFFER_SIZE)
                    }
                }
            } ?: error("cannot open motion photo")
            if (temporary.length() != info.videoLength) error("invalid video length")
            if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            target
        }.getOrElse {
            temporary.delete()
            null
        }
    }
}
