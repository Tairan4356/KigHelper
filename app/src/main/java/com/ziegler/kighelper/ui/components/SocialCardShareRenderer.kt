package com.ziegler.kighelper.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.core.net.toUri
import com.ziegler.kighelper.data.SocialCardProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 将社交卡片渲染为分享用 PNG。
 *
 * 分享图只保留昵称、签名与集合码，不包含平台图标行和任何操作入口。
 */
object SocialCardShareRenderer {

    private const val WIDTH = 1080
    private const val HEIGHT = 1440
    private const val PADDING = 72f
    private const val AVATAR_SIZE = 240f

    suspend fun render(
        context: Context,
        profile: SocialCardProfile,
        qrBitmap: Bitmap,
        backgroundColor: Int,
        textColor: Int
    ): File = withContext(Dispatchers.Default) {
        val output = createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val background = loadBitmap(
            context = context,
            path = profile.customBackgroundPath,
            maxWidth = WIDTH,
            maxHeight = HEIGHT
        )
        if (background != null) {
            drawCrop(canvas, background, 0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat())
            canvas.drawColor(Color.argb(102, 0, 0, 0))
            background.recycle()
        } else {
            canvas.drawColor(backgroundColor)
        }

        val avatar = loadBitmap(
            context = context,
            path = profile.avatarPath,
            maxWidth = AVATAR_SIZE.toInt(),
            maxHeight = AVATAR_SIZE.toInt()
        )
        if (avatar != null) {
            drawFit(
                canvas = canvas,
                bitmap = avatar,
                left = WIDTH - PADDING - AVATAR_SIZE,
                top = PADDING,
                width = AVATAR_SIZE,
                height = AVATAR_SIZE
            )
            avatar.recycle()
        }

        val textWidth = if (avatar != null) {
            WIDTH - PADDING * 2 - AVATAR_SIZE - 40f
        } else {
            WIDTH - PADDING * 2
        }
        val nickname = profile.nickname.ifBlank { "扩列卡片" }
        drawTextBlock(
            canvas = canvas,
            text = nickname,
            x = PADDING,
            y = PADDING + 36f,
            width = textWidth,
            textSize = 96f,
            color = textColor,
            bold = true,
            maxLines = 1
        )
        if (profile.signature.isNotBlank()) {
            drawTextBlock(
                canvas = canvas,
                text = profile.signature,
                x = PADDING,
                y = PADDING + 190f,
                width = textWidth,
                textSize = 38f,
                color = ColorUtils.setAlphaComponent(textColor, 230),
                bold = false,
                maxLines = 2
            )
        }

        val qrSize = 560f
        val qrLeft = (WIDTH - qrSize) / 2f
        val qrTop = 540f
        val qrBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawRoundRect(
            RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), 36f, 36f, qrBackground
        )
        canvas.drawBitmap(
            qrBitmap,
            null,
            RectF(qrLeft + 40f, qrTop + 40f, qrLeft + qrSize - 40f, qrTop + qrSize - 40f),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )

        val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("集合码", WIDTH / 2f, qrTop + qrSize + 92f, captionPaint)

        val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(textColor, 215)
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("扫码查看全部联系方式", WIDTH / 2f, qrTop + qrSize + 150f, hintPaint)

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(textColor, 150)
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("娃语 | 扩列卡片", WIDTH / 2f, HEIGHT - 56f, footerPaint)

        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "social_card_share").apply { mkdirs() }
            val file = File(directory, "social_card_${System.currentTimeMillis()}.png")
            file.outputStream().use { stream ->
                output.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            output.recycle()
            file
        }
    }

    private fun drawTextBlock(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        textSize: Float,
        color: Int,
        bold: Boolean,
        maxLines: Int
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            typeface = Typeface.create(
                Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
            .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.withTranslation(x, y) {
            layout.draw(this)
        }
    }

    private fun drawCrop(
        canvas: Canvas, bitmap: Bitmap, left: Float, top: Float, width: Float, height: Float
    ) {
        val scale = maxOf(width / bitmap.width, height / bitmap.height)
        val sourceWidth = width / scale
        val sourceHeight = height / scale
        val sourceLeft = ((bitmap.width - sourceWidth) / 2f).toInt().coerceIn(0, bitmap.width - 1)
        val sourceTop = ((bitmap.height - sourceHeight) / 2f).toInt().coerceIn(0, bitmap.height - 1)
        val source = Rect(
            sourceLeft,
            sourceTop,
            ((bitmap.width + sourceWidth) / 2f).toInt().coerceIn(sourceLeft + 1, bitmap.width),
            ((bitmap.height + sourceHeight) / 2f).toInt().coerceIn(sourceTop + 1, bitmap.height)
        )
        canvas.drawBitmap(
            bitmap,
            source,
            RectF(left, top, left + width, top + height),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
    }

    private fun drawFit(
        canvas: Canvas, bitmap: Bitmap, left: Float, top: Float, width: Float, height: Float
    ) {
        val scale = minOf(width / bitmap.width, height / bitmap.height)
        val drawWidth = bitmap.width * scale
        val drawHeight = bitmap.height * scale
        val target = RectF(
            left + (width - drawWidth) / 2f,
            top + (height - drawHeight) / 2f,
            left + (width + drawWidth) / 2f,
            top + (height + drawHeight) / 2f
        )
        canvas.drawBitmap(
            bitmap, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
    }

    private fun loadBitmap(
        context: Context, path: String?, maxWidth: Int, maxHeight: Int
    ): Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val resolver = context.contentResolver
            val uri = if (path.startsWith("content://") || path.startsWith("file://")) {
                path.toUri()
            } else {
                Uri.fromFile(File(path))
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxWidth * 2 || bounds.outHeight / sampleSize > maxHeight * 2) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }.getOrNull()
    }
}
