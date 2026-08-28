package com.ziegler.kighelper.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

object WidgetBitmapLoader {

    fun loadBitmap(context: Context, path: String?, maxSize: Int = 512): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            decodePath(context, path, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
            options.inJustDecodeBounds = false
            decodePath(context, path, options)
        } catch (_: Exception) {
            null
        }
    }

    fun loadCircularBitmap(context: Context, path: String?, size: Int): Bitmap? {
        val original = loadBitmap(context, path, size * 8) ?: return null
        val scaled = original.scale(size, size)
        if (scaled !== original) original.recycle()
        return circleBitmap(scaled)
    }

    fun getDrawableAsBitmap(context: Context, resId: Int, size: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, resId) ?: return null
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun decodePath(
        context: Context, path: String, options: BitmapFactory.Options
    ): Bitmap? {
        return if (path.startsWith("content://") || path.startsWith("file://")) {
            val uri = path.toUri()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } else {
            BitmapFactory.decodeFile(path, options)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun circleBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val squared = Bitmap.createBitmap(bitmap, x, y, size, size)
        val result = createBitmap(size, size)
        val canvas = Canvas(result)
        val paint = Paint().apply { isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squared, 0f, 0f, paint)
        if (squared !== bitmap) squared.recycle()
        return result
    }

}
