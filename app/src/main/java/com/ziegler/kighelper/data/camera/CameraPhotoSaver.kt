package com.ziegler.kighelper.data.camera

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 负责把 CameraX 产出的缓存文件整理成相册中的最终照片。
 *
 * 处理顺序固定为：写入 EXIF/位置信息 -> 追加实况视频 -> 保存到 MediaStore。
 * 无论成功与否都会清理缓存文件，避免相机目录长期堆积半成品。
 */
class CameraPhotoSaver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageSaver: MediaStoreImageSaver,
    private val photoMetadataWriter: PhotoMetadataWriter
) {
    /**
     * 在 IO 线程完成照片后处理并返回保存结果。
     *
     * @param photoFile CameraX 写入的临时 JPEG。
     * @param videoFile 实况照片的临时 MP4，普通拍照时为 null。
     * @param writeLocation 是否把当前定位写入照片 EXIF。
     */
    suspend fun save(
        photoFile: File, videoFile: File?, writeLocation: Boolean
    ): Result<SavedPhoto> = withContext(Dispatchers.IO) {
        runCatching {
            photoMetadataWriter.write(
                photoFile = photoFile, writeLocation = writeLocation
            )
            val motionPhoto =
                videoFile != null && MotionPhotoWriter.attachVideo(photoFile, videoFile)
            imageSaver.save(photoFile, motionPhoto = motionPhoto) ?: error("照片写入相册失败")
        }.onFailure { error ->
            Log.e(CAMERA_LOG_TAG, "photo save failed", error)
        }.also {
            CameraCaptureFiles.cleanup(context, photoFile)
            CameraCaptureFiles.cleanup(context, videoFile)
        }
    }
}
