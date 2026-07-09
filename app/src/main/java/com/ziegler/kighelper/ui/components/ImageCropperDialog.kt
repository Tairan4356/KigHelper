package com.ziegler.kighelper.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.request.ImageRequest
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.CornerRadiusProperties
import com.smarttoolfactory.cropper.model.CropOutline
import com.smarttoolfactory.cropper.model.OvalCropShape
import com.smarttoolfactory.cropper.model.PolygonCropShape
import com.smarttoolfactory.cropper.model.PolygonProperties
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.model.RoundedCornerCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.core.graphics.createBitmap
import kotlin.math.cos
import kotlin.math.sin

/**
 * 裁剪形状。封装了 SmartToolFactory/Compose-Cropper 中常用的几种轮廓。
 *
 * - [RECT] 矩形：适合二维码、卡片背景
 * - [OVAL] 圆形：传统圆形头像
 * - [ROUNDED_RECT] 圆角矩形
 * - [POLYGON_S3] 三角形：createPolygonShape with sides=3
 * - [POLYGON_S5] 五边形
 * - [POLYGON_S6] 六边形
 * - [POLYGON_S8] 八边形
 *
 * 注意：多边形顶点角度从顶部开始（库默认 0 弧度对应右侧），
 * 为了让三角形顶点朝上，给 polygon 旋转 -90 度（即 -π/2 弧度对应的度数）。
 */
enum class CropShape(val displayName: String) {
    RECT("矩形"), OVAL("圆形"), ROUNDED_RECT("圆角"), POLYGON_S3("三角形"), POLYGON_S5(
        "五边形"
    ),
    POLYGON_S6("六边形"), POLYGON_S8("八边形");

}

/**
 * 把当前 [CropShape] 转为库所需的 [CropOutline]。
 * 每个 enum 项的 id 在此处唯一赋值，避免库内部对 id 排序时混淆。
 */
private fun CropShape.toCropOutline(): CropOutline = when (this) {
    CropShape.RECT -> RectCropShape(id = 0, title = "Rect")
    CropShape.OVAL -> OvalCropShape(id = 1, title = "Oval")
    CropShape.ROUNDED_RECT -> RoundedCornerCropShape(
        id = 2, title = "Rounded", cornerRadius = CornerRadiusProperties(30, 30, 30, 30)
    )

    CropShape.POLYGON_S3 -> PolygonCropShape(
        id = 3, title = "Triangle",
        // 三角形顶点朝上：旋转 -90°
        polygonProperties = PolygonProperties(sides = 3, angle = -90f)
    )

    CropShape.POLYGON_S5 -> PolygonCropShape(
        id = 4, title = "Pentagon", polygonProperties = PolygonProperties(sides = 5, angle = -90f)
    )

    CropShape.POLYGON_S6 -> PolygonCropShape(
        id = 5, title = "Hexagon", polygonProperties = PolygonProperties(sides = 6, angle = 0f)
    )

    CropShape.POLYGON_S8 -> PolygonCropShape(
        id = 6, title = "Octagon", polygonProperties = PolygonProperties(sides = 8, angle = 22.5f)
    )
}

/**
 * 裁剪请求。封装了触发裁剪所需的全部参数。
 *
 * @param inputUri 待裁剪的原图 Uri
 * @param aspectRatio 期望的裁剪比例，如 `AspectRatio(1f)` 为 1:1；
 *                    `AspectRatio.Original` 为自由比例
 * @param defaultShape 默认裁剪形状
 * @param selectableShapes 允许用户在裁剪界面切换的形状列表。
 *                         为 null 或 size <= 1 时隐藏底部形状选择条。
 *                         头像场景应传入多形状供选择；二维码/背景可保持单一形状。
 * @param tag 透传给调用方的标识，用于在 onResult 中区分裁剪来源
 */
data class CropRequest(
    val inputUri: Uri,
    val aspectRatio: AspectRatio,
    val defaultShape: CropShape = CropShape.RECT,
    val selectableShapes: List<CropShape>? = null,
    val tag: String? = null
)

/**
 * 把 [ImageBitmap] 压缩为 PNG 写入应用缓存目录，返回该文件的 [Uri]。
 *
 * 选择 PNG 而非 JPEG：头像、二维码、图标等含文字/边缘的图片不应有压缩失真；
 * 且多边形/圆形裁剪会产生透明 alpha 通道，JPEG 不支持 alpha。
 *
 * 文件名加上随机 UUID 避免并发覆盖。
 * 调用方应在合适的时机清理这些缓存文件（Repository 在最终落库后会复制到 `social_card/`）。
 */
suspend fun ImageBitmap.saveToCachePng(context: Context): Uri = withContext(Dispatchers.IO) {
    val file = File(context.cacheDir, "crop_${UUID.randomUUID()}.png")
    FileOutputStream(file).use { out ->
        val androidBitmap = asAndroidBitmap()
        // 若来源是 HW Bitmap 等不可变位图，复制为可变副本以避免 compress 在某些设备上抛异常
        val safeBitmap = if (androidBitmap.isMutable) androidBitmap
        else androidBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: androidBitmap
        safeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    Uri.fromFile(file)
}

/**
 * 对裁剪结果重新应用裁剪形状蒙版，确保不规则形状外的区域为透明。
 *
 * CropAgent 内部通过 BlendMode.SrcIn 在已有像素的位图上裁剪，理论上形状外
 * 像素应被清除为透明，但部分设备/渲染路径下可能残留为黑色等非透明像素。
 *
 * 此函数在一张全新的透明画布上，用 [PorterDuff.Mode.SRC_IN] 重新合成：
 * 先画形状蒙版（不透明），再绘原图——形状外从未被绘制，自然保持透明。
 * 矩形形状无空白区域，直接跳过。
 *
 * @param shape 实际使用的裁剪形状，用于重建蒙版路径
 */
private suspend fun ImageBitmap.applyCropShapeMask(shape: CropShape): ImageBitmap =
    withContext(Dispatchers.Default) {
        if (shape == CropShape.RECT) return@withContext this@applyCropShapeMask

        val srcBitmap = asAndroidBitmap()
        val width = srcBitmap.width
        val height = srcBitmap.height

        // 用和 CropAgent 相同的方式构建裁剪形状路径
        val outline = when (val co = shape.toCropOutline()) {
            is com.smarttoolfactory.cropper.model.CropShape -> co.shape.createOutline(
                size = Size(width.toFloat(), height.toFloat()),
                layoutDirection = LayoutDirection.Ltr,
                density = Density(1f, 1f)
            )

            else -> return@withContext this@applyCropShapeMask
        }
        val androidPath = Path().apply { addOutline(outline) }.asAndroidPath()

        // 新建透明画布：先画形状蒙版（不透明白色），再用 SRC_IN 绘制原图
        val result = createBitmap(width, height)
        val canvas = Canvas(result)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(androidPath, maskPaint)

        val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(srcBitmap, 0f, 0f, srcPaint)

        result.asImageBitmap()
    }

/**
 * 基于 SmartToolFactory/Compose-Cropper 封装的全屏裁剪对话框。
 *
 * 流程：
 * 1. 调用方通过 [request] 传入原图与裁剪参数；
 * 2. 内部用 Coil 异步加载 [Uri] 为 [ImageBitmap]；
 * 3. 用户调整裁剪框后点击右上角「完成」，触发库的 crop 流程；
 * 4. onCropSuccess 返回的 ImageBitmap 通过 [saveToCachePng] 落到 cacheDir；
 * 5. 把生成的文件 [Uri] 通过 [onResult] 回传给调用方；
 * 6. [onDismiss] 用于用户取消（返回键或左上角返回）。
 *
 * 当 [CropRequest.selectableShapes] 非空且多于一项时，底部显示形状选择条，
 * 用户可在裁剪过程中切换形状，切换会重建 [ImageCropper]。
 *
 * @param request 当前裁剪请求；为 null 时不显示
 * @param onResult 裁剪完成回调，返回裁剪结果的缓存文件 Uri 与原 [CropRequest.tag]
 * @param onDismiss 用户取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropperDialog(
    request: CropRequest?, onResult: (Uri, String?) -> Unit, onDismiss: () -> Unit
) {
    if (request == null) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val imageLoader = remember { ImageLoader(context) }

    // 加载状态：null=加载中, ImageBitmap=就绪
    var imageBitmap by remember(request.inputUri) { mutableStateOf<ImageBitmap?>(null) }
    var loadError by remember(request.inputUri) { mutableStateOf<String?>(null) }

    // 裁剪触发开关：true 时库开始裁剪
    var crop by remember(request) { mutableStateOf(false) }
    // 裁剪进行中（防止重复触发）
    var cropping by remember(request) { mutableStateOf(false) }

    // 当前选择的形状。selectableShapes 非空时可在底部切换；否则固定为 defaultShape
    val canSelectShape = request.selectableShapes != null && request.selectableShapes.size > 1
    var currentShape by remember(request) {
        mutableStateOf(
            if (canSelectShape) request.defaultShape
            else request.defaultShape
        )
    }

    val handleSize = with(density) { 20.dp.toPx() }

    // 异步加载图片。注意：局部变量用 imageRequest 避免与外层 request 同名遮蔽
    LaunchedEffect(request.inputUri) {
        imageBitmap = null
        loadError = null
        runCatching {
            val imageRequest = ImageRequest.Builder(context).data(request.inputUri).build()
            val drawable = imageLoader.execute(imageRequest).drawable
                ?: throw IllegalStateException("drawable is null")
            (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.asImageBitmap()
                ?: throw IllegalStateException("not a bitmap drawable")
        }.onSuccess { imageBitmap = it }.onFailure { loadError = it.message ?: "图片加载失败" }
    }

    // 切换形状时 CropProperties 必须重建（库内部基于此初始化 CropState）
    val cropOutlineProperty = remember(currentShape) {
        val shape = currentShape.toCropOutline()
        val outlineType = when (shape) {
            is RectCropShape -> com.smarttoolfactory.cropper.model.OutlineType.Rect
            is OvalCropShape -> com.smarttoolfactory.cropper.model.OutlineType.Oval
            is RoundedCornerCropShape -> com.smarttoolfactory.cropper.model.OutlineType.RoundedRect
            is PolygonCropShape -> com.smarttoolfactory.cropper.model.OutlineType.Polygon
            else -> com.smarttoolfactory.cropper.model.OutlineType.Rect
        }
        CropOutlineProperty(outlineType = outlineType, cropOutline = shape)
    }
    val cropProperties = remember(cropOutlineProperty, request.aspectRatio, handleSize) {
        CropDefaults.properties(
            cropOutlineProperty = cropOutlineProperty,
            handleSize = handleSize,
            aspectRatio = request.aspectRatio,
            // 固定比例：1:1 头像/二维码/图标必须严格方形；自由比例下也允许用户拉扯
            fixedAspectRatio = request.aspectRatio != AspectRatio.Original
        )
    }
    val cropStyle = remember { CropDefaults.style() }

    Dialog(
        onDismissRequest = onDismiss, properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(title = { Text("裁剪图片") }, navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "取消")
                    }
                }, actions = {
                    // 裁剪中或图片加载中时禁用
                    val ready = imageBitmap != null && !cropping
                    IconButton(
                        onClick = { if (ready) crop = true }, enabled = ready
                    ) { Icon(Icons.Filled.Check, "完成") }
                })

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = imageBitmap
                    when {
                        loadError != null -> Text(
                            text = loadError ?: "", color = MaterialTheme.colorScheme.error
                        )

                        bitmap == null -> CircularProgressIndicator()

                        else -> ImageCropper(
                            modifier = Modifier.fillMaxSize(),
                            imageBitmap = bitmap,
                            contentDescription = "待裁剪图片",
                            cropStyle = cropStyle,
                            cropProperties = cropProperties,
                            crop = crop,
                            backgroundColor = Color.Black,
                            onCropStart = { cropping = true },
                            onCropSuccess = { cropped ->
                                cropping = false
                                crop = false
                                scope.launch {
                                    val masked = cropped.applyCropShapeMask(currentShape)
                                    val uri = masked.saveToCachePng(context)
                                    onResult(uri, request.tag)
                                }
                            })
                    }
                }

                // 底部形状选择条：仅在 selectableShapes > 1 时显示
                if (canSelectShape) {
                    ShapeSelectorBar(
                        shapes = request.selectableShapes,
                        selected = currentShape,
                        onSelect = { currentShape = it })
                }
            }

            // 裁剪进行中的遮罩
            if (cropping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.size(12.dp))
                        Text("正在裁剪…", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 底部形状选择条。横向滚动，每项为带预览的圆形按钮。
 *
 * - 选中态：主色边框 + 主色背景
 * - 未选中：浅边框
 * - 预览用对应形状的裁剪框样式（矩形/圆/三角/六边形等）
 */
@Composable
private fun ShapeSelectorBar(
    shapes: List<CropShape>, selected: CropShape, onSelect: (CropShape) -> Unit
) {
    Surface(color = Color(0xCC000000)) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(shapes.size) { index ->
                val shape = shapes[index]
                ShapeChip(
                    shape = shape, selected = shape == selected, onClick = { onSelect(shape) })
            }
        }
    }
}

/**
 * 单个形状选择按钮。用简单的预览图直观展示形状。
 */
@Composable
private fun ShapeChip(shape: CropShape, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color(0x66FFFFFF)
    val borderWidth = if (selected) 2.dp else 1.dp
    val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    else Color(0x33FFFFFF)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 形状预览：48x48 的方块，内部按形状裁剪白底
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(
                    when (shape) {
                    CropShape.RECT -> RoundedCornerShape(2.dp)
                    CropShape.OVAL -> CircleShape
                    CropShape.ROUNDED_RECT -> RoundedCornerShape(8.dp)
                    CropShape.POLYGON_S3 -> androidx.compose.foundation.shape.GenericShape { size, _ ->
                        addPolygonPreviewPath(size, sides = 3, rotationDeg = -90f)
                    }

                    CropShape.POLYGON_S5 -> androidx.compose.foundation.shape.GenericShape { size, _ ->
                        addPolygonPreviewPath(size, sides = 5, rotationDeg = -90f)
                    }

                    CropShape.POLYGON_S6 -> androidx.compose.foundation.shape.GenericShape { size, _ ->
                        addPolygonPreviewPath(size, sides = 6, rotationDeg = 0f)
                    }

                    CropShape.POLYGON_S8 -> androidx.compose.foundation.shape.GenericShape { size, _ ->
                        addPolygonPreviewPath(size, sides = 8, rotationDeg = 22.5f)
                    }
                })
                .background(Color.White))
        Spacer(Modifier.size(4.dp))
        Text(
            text = shape.displayName,
            color = if (selected) Color.White else Color(0xCCFFFFFF),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * 在 GenericShape builder 中添加多边形路径预览。
 * 与库的 [com.smarttoolfactory.cropper.util.createPolygonShape] 保持一致的几何算法，
 * 以保证预览与实际裁剪形状一致。
 */
private fun Path.addPolygonPreviewPath(
    size: Size, sides: Int, rotationDeg: Float
) {
    val radius = size.width.coerceAtMost(size.height) / 2
    val cx = size.width / 2
    val cy = size.height / 2
    val angleStep = 2.0 * Math.PI / sides
    val rotationRad = Math.toRadians(rotationDeg.toDouble())

    moveTo(
        (cx + (radius * cos(rotationRad))).toFloat(), (cy + (radius * sin(rotationRad))).toFloat()
    )
    for (i in 1 until sides) {
        lineTo(
            (cx + (radius * cos(angleStep * i + rotationRad))).toFloat(),
            (cy + (radius * sin(angleStep * i + rotationRad))).toFloat()
        )
    }
    close()
}
