package com.ziegler.kighelper.data.socialshare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * 二维码图片识别与生成。
 *
 * 识别只用于从用户上传的二维码图片中提取文本；生成的二维码用于集合码展示。
 */
object SocialCardQrCodec {

    private const val MAX_DECODE_EDGE = 2048

    /**
     * 识别二维码。支持内部文件路径、file:// 与 content:// URI。
     * 失败返回 null，调用方应回退到平台名 + 账号。
     */
    suspend fun decode(context: Context, pathOrUri: String): String? = withContext(Dispatchers.IO) {
        if (pathOrUri.isBlank()) return@withContext null
        val bitmap = runCatching { loadSampledBitmap(context, pathOrUri) }.getOrNull()
            ?: return@withContext null
        try {
            decodeBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun generate(content: String, sizePx: Int = 1024): Bitmap? {
        if (content.isEmpty() || sizePx <= 0) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )
            val matrix: BitMatrix = QRCodeWriter().encode(
                content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints
            )
            matrixToBitmap(matrix)
        }.getOrNull()
    }

    private fun decodeBitmap(source: Bitmap): String? {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return null

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val original = RGBLuminanceSource(width, height, pixels)
        decodeWithSource(original)?.let { return it }

        val invertedPixels = IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            val r = 0xFF - Color.red(pixel)
            val g = 0xFF - Color.green(pixel)
            val b = 0xFF - Color.blue(pixel)
            Color.rgb(r, g, b)
        }
        val inverted = RGBLuminanceSource(width, height, invertedPixels)
        decodeWithSource(inverted)?.let { return it }

        val rotated = rotate90(pixels, width, height)
        decodeWithSource(RGBLuminanceSource(height, width, rotated))?.let { return it }

        val rotated180 = rotate90(rotated, height, width)
        decodeWithSource(RGBLuminanceSource(width, height, rotated180))?.let { return it }

        val rotated270 = rotate90(rotated180, width, height)
        return decodeWithSource(RGBLuminanceSource(height, width, rotated270))
    }

    private fun decodeWithSource(source: RGBLuminanceSource): String? {
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val binarizers = listOf({ HybridBinarizer(source) }, { GlobalHistogramBinarizer(source) })
        for (binarizerFactory in binarizers) {
            val result = runCatching {
                MultiFormatReader().decode(BinaryBitmap(binarizerFactory()), hints)
            }.getOrNull()
            if (!result?.text.isNullOrBlank()) return result.text
        }
        return null
    }

    private fun rotate90(pixels: IntArray, width: Int, height: Int): IntArray {
        val rotated = IntArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val newX = height - 1 - y
                rotated[x * height + newX] = pixels[y * width + x]
            }
        }
        return rotated
    }

    private fun matrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun loadSampledBitmap(context: Context, pathOrUri: String): Bitmap? {
        val source = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            pathOrUri.toUri()
        } else {
            Uri.fromFile(File(pathOrUri))
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_DECODE_EDGE || bounds.outHeight / sampleSize > MAX_DECODE_EDGE) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return openStream(context, source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun openStream(context: Context, uri: Uri): InputStream? {
        return runCatching {
            if (uri.scheme == "file") {
                File(uri.path ?: return null).inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
        }.getOrNull()
    }
}
