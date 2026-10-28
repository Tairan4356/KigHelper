package com.ziegler.kighelper.data.camera

import android.location.Location
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.ziegler.kighelper.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 为 CameraX 生成的 JPEG 补充 EXIF。
 *
 * CameraX 只保证基础图像数据，这里统一写入拍摄时间、应用/设备信息和可选的 GPS。
 * 读取定位可能耗时，因此整个方法运行在 IO 线程。
 */
@Singleton
class PhotoMetadataWriter @Inject constructor(
    private val locationProvider: PhotoLocationProvider
) {
    /**
     * 写入元数据并返回实际取得的定位，便于调用方记录或调试。
     *
     * @param writeLocation true 且已授权时才请求定位；拒绝授权不会导致保存失败。
     */
    suspend fun write(photoFile: File, writeLocation: Boolean): Location? =
        withContext(Dispatchers.IO) {
            val location = if (writeLocation) locationProvider.currentLocation() else null
            runCatching {
                val exif = ExifInterface(photoFile.absolutePath)
                val now = SimpleDateFormat(
                    "yyyy:MM:dd HH:mm:ss", Locale.US
                ).format(Date())
                exif.setAttribute(ExifInterface.TAG_DATETIME, now)
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, now)
                exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, now)
                exif.setAttribute(
                    ExifInterface.TAG_SOFTWARE, "KigHelper ${BuildConfig.VERSION_NAME}"
                )
                exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
                exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "KigHelper")
                if (location != null) {
                    writeLocation(exif, location)
                }
                exif.saveAttributes()
            }
            location
        }

    /** 按 EXIF 规范写入经纬度、海拔和 UTC 时间。 */
    private fun writeLocation(exif: ExifInterface, location: Location) {
        exif.setAttribute(
            ExifInterface.TAG_GPS_LATITUDE, toRational(location.latitude)
        )
        exif.setAttribute(
            ExifInterface.TAG_GPS_LATITUDE_REF, if (location.latitude >= 0) "N" else "S"
        )
        exif.setAttribute(
            ExifInterface.TAG_GPS_LONGITUDE, toRational(location.longitude)
        )
        exif.setAttribute(
            ExifInterface.TAG_GPS_LONGITUDE_REF, if (location.longitude >= 0) "E" else "W"
        )
        if (location.hasAltitude()) {
            exif.setAttribute(
                ExifInterface.TAG_GPS_ALTITUDE, "${location.altitude.roundToInt()}/1"
            )
            exif.setAttribute(
                ExifInterface.TAG_GPS_ALTITUDE_REF, if (location.altitude >= 0) "0" else "1"
            )
        }
        // GPS 日期/时间按 EXIF 规范使用 UTC，而不是设备本地时区。
        val utc = TimeZone.getTimeZone("UTC")
        val timestamp = Date(location.time)
        exif.setAttribute(
            ExifInterface.TAG_GPS_DATESTAMP, SimpleDateFormat("yyyy:MM:dd", Locale.US).apply {
                timeZone = utc
            }.format(timestamp)
        )
        exif.setAttribute(
            ExifInterface.TAG_GPS_TIMESTAMP, SimpleDateFormat("HH:mm:ss", Locale.US).apply {
                timeZone = utc
            }.format(timestamp)
        )
    }

    /** 把十进制度转换成 EXIF 的度/分/秒有理数格式。 */
    private fun toRational(value: Double): String {
        val absolute = abs(value)
        val degrees = absolute.toInt()
        val minutesValue = (absolute - degrees) * 60
        val minutes = minutesValue.toInt()
        val seconds = ((minutesValue - minutes) * 60 * 10_000).roundToInt()
        return "$degrees/1,$minutes/1,$seconds/10000"
    }
}

/**
 * 把 MP4 追加到 JPEG 末尾并写入 Motion Photo XMP。
 *
 * 文件结构约定：先写完整 JPEG，再写 MP4；播放端通过 XMP 中的视频长度
 * 从文件末尾反推视频起点。写入顺序不能交换。
 */
object MotionPhotoWriter {
    /** 追加视频并写入 GCamera 元数据；失败时返回 false，原 JPEG 仍可正常显示。 */
    fun attachVideo(photoFile: File, videoFile: File): Boolean {
        if (!photoFile.exists() || !videoFile.exists() || videoFile.length() <= 0L) {
            return false
        }
        return runCatching {
            val videoSize = videoFile.length()
            val exif = ExifInterface(photoFile.absolutePath)
            val existingXmp = exif.getAttribute(ExifInterface.TAG_XMP)
            val xmp = mergeXmp(existingXmp, videoSize)
            exif.setAttribute(ExifInterface.TAG_XMP, xmp)
            exif.saveAttributes()
            FileOutputStream(photoFile, true).use { output ->
                videoFile.inputStream().use { input -> input.copyTo(output) }
            }
            true
        }.getOrDefault(false)
    }

    /**
     * 合并已有 XMP。
     *
     * 已有 Motion Photo 标记时保持不变，避免重复播放；否则把新描述插入 `</rdf:RDF>` 前。
     */
    private fun mergeXmp(existing: String?, videoSize: Long): String {
        val description = buildMotionDescription(videoSize)
        if (existing.isNullOrBlank()) {
            return """
                <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="KigHelper">
                 <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  $description
                 </rdf:RDF>
                </x:xmpmeta>
            """.trimIndent()
        }
        if (!existing.contains("</rdf:RDF>")) {
            return """
                <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="KigHelper">
                 <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  $description
                 </rdf:RDF>
                </x:xmpmeta>
            """.trimIndent()
        }
        if (existing.contains("GCamera:MotionPhoto") || existing.contains("GCamera:MicroVideo")) {
            return existing
        }
        return existing.replaceFirst("</rdf:RDF>", "$description</rdf:RDF>")
    }

    /** 生成同时兼容 MotionPhoto 和旧 MicroVideo 字段的 XMP 描述。 */
    private fun buildMotionDescription(videoSize: Long): String {
        return """
            <rdf:Description rdf:about=""
              xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
              xmlns:Container="http://ns.google.com/photos/1.0/container/"
              xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
              GCamera:MotionPhoto="1"
              GCamera:MotionPhotoVersion="1"
              GCamera:MotionPhotoPresentationTimestampUs="0"
              GCamera:MicroVideo="1"
              GCamera:MicroVideoVersion="1"
              GCamera:MicroVideoOffset="$videoSize"
              GCamera:MicroVideoPresentationTimestampUs="0">
             <Container:Directory>
              <rdf:Seq>
               <rdf:li rdf:parseType="Resource">
                <Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" Item:Length="0" Item:Padding="0"/>
               </rdf:li>
               <rdf:li rdf:parseType="Resource">
                <Container:Item Item:Mime="video/mp4" Item:Semantic="MotionPhoto" Item:Length="$videoSize" Item:Padding="0"/>
               </rdf:li>
              </rdf:Seq>
             </Container:Directory>
            </rdf:Description>
        """.trimIndent()
    }
}
