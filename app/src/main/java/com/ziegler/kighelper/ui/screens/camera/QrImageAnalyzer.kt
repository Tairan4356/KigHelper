package com.ziegler.kighelper.ui.screens.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * 从 CameraX 的 YUV 帧中只提取 Y 平面做二维码识别。
 *
 * 限制识别频率可以降低 ZXing 对预览帧的占用；[enabled] 由页面在弹窗显示或
 * 外部应用返回后关闭，避免同一码连续触发。
 */
class QrImageAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile
    var enabled: Boolean = true

    private var lastScanAt = 0L

    override fun analyze(image: ImageProxy) {
        image.use { image ->
            if (!enabled) return
            val now = System.currentTimeMillis()
            if (now - lastScanAt < SCAN_INTERVAL_MS) return
            lastScanAt = now
            val text = decodeYPlane(image) ?: return
            onQrDetected(text)
        }
    }

    /** 按 rowStride/pixelStride 紧凑拷贝 Y 平面，兼容不同厂商的 YUV 布局。 */
    private fun decodeYPlane(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null
        val buffer = plane.buffer
        val base = buffer.position()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val data = ByteArray(width * height)
        for (row in 0 until height) {
            val rowStart = base + row * rowStride
            for (column in 0 until width) {
                val index = rowStart + column * pixelStride
                data[row * width + column] = if (index < buffer.limit()) buffer.get(index) else 0
            }
        }
        val source = PlanarYUVLuminanceSource(
            data, width, height, 0, 0, width, height, false
        )
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        return runCatching {
            val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), hints)
            result.text?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private companion object {
        const val SCAN_INTERVAL_MS = 300L
    }
}
