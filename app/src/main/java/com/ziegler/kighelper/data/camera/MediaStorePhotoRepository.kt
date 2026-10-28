package com.ziegler.kighelper.data.camera

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 时光轴使用的相册图片快照。 */
data class GalleryPhoto(
    val id: Long, val uri: Uri, val displayName: String, val bucketName: String?,
    /** 毫秒时间戳，优先取 DATE_TAKEN，缺失时由 DATE_ADDED 换算。 */
    val timestamp: Long, val mimeType: String?, val width: Int, val height: Int
)

/**
 * 从 MediaStore 读取设备相册图片。
 *
 * 只读取图片表，不请求具体文件内容；缩略图交由 Coil 按需加载。
 */
@Singleton
class MediaStorePhotoRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * 查询全部可用图片，按拍摄/添加时间倒序返回。
     *
     * 过滤掉尚在写入的 pending 项和位于回收站的图片，避免时光轴显示不可访问条目。
     */
    suspend fun loadPhotos(): List<GalleryPhoto> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION") MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // 列顺序固定，下面用 getColumnIndexOrThrow/可选索引读取，避免厂商列顺序差异。
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
        }.toTypedArray()

        val selection = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add("${MediaStore.MediaColumns.IS_PENDING} = 0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add("${MediaStore.MediaColumns.IS_TRASHED} = 0")
            }
        }.takeIf { it.isNotEmpty() }?.joinToString(" AND ")

        val photos = mutableListOf<GalleryPhoto>()
        context.contentResolver.query(
            collection, projection, selection, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val addedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val bucketIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val widthIndex = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIndex = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val taken = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L
                val added = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L
                // DATE_TAKEN 是毫秒，DATE_ADDED 是秒；优先使用更准确的拍摄时间。
                val timestamp = if (taken > 0L) taken else added * 1000L
                photos += GalleryPhoto(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = cursor.getString(nameIndex) ?: "图片",
                    bucketName = bucketIndex.takeIf { it >= 0 }?.let(cursor::getString),
                    timestamp = timestamp,
                    mimeType = mimeIndex.takeIf { it >= 0 }?.let(cursor::getString),
                    width = widthIndex.takeIf { it >= 0 }?.let(cursor::getInt) ?: 0,
                    height = heightIndex.takeIf { it >= 0 }?.let(cursor::getInt) ?: 0
                )
            }
        }
        // 查询已按 DATE_ADDED 排序，这里再按最终展示时间排序，兼容 DATE_TAKEN 与添加时间不一致的情况。
        photos.sortedByDescending { it.timestamp }
    }
}
