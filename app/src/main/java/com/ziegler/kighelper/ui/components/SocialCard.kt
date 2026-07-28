package com.ziegler.kighelper.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ziegler.kighelper.data.CardTemplates
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.SocialPlatformIcons
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 把一个字符串路径解析成 Coil 可识别的模型。
 * 既支持落库的本地文件路径，也支持预览时的 Uri 字符串。
 */
private fun resolveModel(path: String?): Any? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("content://") || path.startsWith("file://")) {
        path.toUri()
    } else {
        File(path)
    }
}

/**
 * 计算单个联系人在卡片上展示用的图标模型：
 * 1. 优先使用自定义上传图标 [SocialContact.customIconPath]
 * 2. 否则使用预设图标 drawable（按 [SocialContact.iconKey]）
 * 3. 都没有则返回 null，调用方应回退到首字渲染
 *
 * @return Pair.First = AsyncImage 模型（File/Uri），null 表示需要首字回退；
 *         Pair.Second = drawable 资源 ID，0 表示无预设图标可用
 */
private fun resolveContactIcon(contact: SocialContact): Pair<Any?, Int> {
    val customPath = contact.customIconPath
    if (!customPath.isNullOrBlank()) {
        return resolveModel(customPath) to 0
    }
    val iconKey = contact.iconKey
    if (iconKey == SocialPlatformIcons.KEY_DEFAULT) return null to 0
    return null to SocialPlatformIcons.iconRes(iconKey)
}

/** 未选中状态下使用的灰色（去色后呈现的中性灰）。 */
private val UnselectedGrayTint = Color(0xFF404040)

/** 自定义上传图标在未选中状态使用的「去色」颜色矩阵（饱和度 = 0，按 BT.709 亮度系数）。 */
private val DesaturateColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.0535f,
            0.1795f,
            0.0181f,
            0f,
            0f,
            0.0535f,
            0.1795f,
            0.0181f,
            0f,
            0f,
            0.0535f,
            0.1795f,
            0.0181f,
            0f,
            0f,
            0f,
            0f,
            0f,
            1f,
            0f
        )
    )
)

@Composable
private fun rememberCardBackground(profile: SocialCardProfile): Brush {
    return if (profile.templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX && !profile.customBackgroundPath.isNullOrBlank()) {
        // 自定义背景在父 Box 中用 AsyncImage 覆盖渲染；此处返回透明占位
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    } else {
        val index = profile.templateIndex.coerceIn(0, CardTemplates.presets.lastIndex)
        CardTemplates.presets[index].brush
    }
}

@Composable
private fun cardTextColor(profile: SocialCardProfile): Color {
    return if (profile.templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX && !profile.customBackgroundPath.isNullOrBlank()) {
        Color.White
    } else {
        val index = profile.templateIndex.coerceIn(0, CardTemplates.presets.lastIndex)
        CardTemplates.presets[index].onBackground
    }
}

/** 平板最小宽度阈值（dp）：smallestScreenWidthDp >= 此值视为平板。 */
private const val TABLET_SMALLEST_WIDTH_DP = 600

/** 横屏平板下卡片高度占屏幕高度的比例。 */
private const val LANDSCAPE_TABLET_HEIGHT_FRACTION = 0.6f

/**
 * 社交卡片布局模式：由屏幕方向与尺寸共同决定，用于解耦布局判定与渲染逻辑。
 */
private enum class SocialCardLayoutMode {
    /** 竖屏：内容自上而下排列。 */
    PORTRAIT,

    /** 横屏手机：二维码固定尺寸在右，主内容占满剩余宽度。 */
    LANDSCAPE_PHONE,

    /** 横屏平板：二维码:主内容 = 2:1，二维码随容器放大，卡片高度按屏幕高度比例。 */
    LANDSCAPE_TABLET
}

/** 依据当前 [Configuration] 推断社交卡片布局模式。 */
@Composable
private fun socialCardLayoutMode(): SocialCardLayoutMode {
    val configuration = LocalConfiguration.current
    return when {
        configuration.orientation != Configuration.ORIENTATION_LANDSCAPE -> SocialCardLayoutMode.PORTRAIT

        configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP -> SocialCardLayoutMode.LANDSCAPE_TABLET

        else -> SocialCardLayoutMode.LANDSCAPE_PHONE
    }
}

/**
 * 异步读取图片文件的宽高比。
 * 仅读取图片头信息（inJustDecodeBounds = true），不会分配像素内存。
 */
@Composable
private fun rememberImageAspectRatio(context: Context, path: String?): Float? {
    var aspectRatio by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(path) {
        if (!path.isNullOrBlank()) {
            val dimensions = withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                when {
                    path.startsWith("content://") || path.startsWith("file://") -> {
                        val uri = path.toUri()
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)
                            if (options.outWidth > 0 && options.outHeight > 0) {
                                options.outWidth to options.outHeight
                            } else null
                        }
                    }

                    else -> {
                        val file = File(path)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath, options)
                            if (options.outWidth > 0 && options.outHeight > 0) {
                                options.outWidth to options.outHeight
                            } else null
                        } else null
                    }
                }
            }
            aspectRatio = dimensions?.let { (w, h) -> w.toFloat() / h.toFloat() }
        } else {
            aspectRatio = null
        }
    }

    return aspectRatio
}

/**
 * 社交卡片预览。在工具箱主页与编辑页之间复用。
 *
 * 布局自上而下：
 * 1. 头像 + 昵称 + 签名
 * 2. 平台图标行（最多 4 个/行，选中态显示品牌色，未选中显示灰色）
 * 3. 当前选中平台的二维码区域（默认显示第一个平台）
 *
 * 点击平台图标切换下方二维码展示，点击二维码进入全屏查看。
 *
 * @param profile 待展示的资料
 * @param modifier 修饰符
 * @param showEditHint 是否在头像为空时显示「点击编辑」占位
 */
@Composable
fun SocialCard(
    profile: SocialCardProfile,
    modifier: Modifier = Modifier,
    showEditHint: Boolean = false,
    selectedIndex: Int? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null
) {
    val textColor = cardTextColor(profile)
    val context = LocalContext.current
    val layoutMode = socialCardLayoutMode()

    // 已配置的平台（有账号或二维码都算）
    val visibleContacts = remember(profile.contacts) { profile.visibleContacts }

    // 当前选中平台的索引：支持外部上提（state hoisting），未传入时回退到内部状态；
    // 越界时由 safeIndex 自动夹紧
    var internalSelectedIndex by remember(visibleContacts.size) {
        mutableIntStateOf(0)
    }
    val currentSelectedIndex = selectedIndex ?: internalSelectedIndex
    val onSelectedChange = onSelectedIndexChange ?: { internalSelectedIndex = it }
    val safeIndex = currentSelectedIndex.coerceIn(0, visibleContacts.lastIndex.coerceAtLeast(0))
    val selectedContact = visibleContacts.getOrNull(safeIndex)

    // 点击二维码进入全屏查看的目标平台
    var fullscreenContact by remember { mutableStateOf<SocialContact?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 背景层：自定义背景图优先；否则渐变背景由 Surface 持有
            val customBgPath = profile.customBackgroundPath
            val isCustomBg =
                profile.templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX && !customBgPath.isNullOrBlank()

            val imageAspectRatio = if (isCustomBg) {
                rememberImageAspectRatio(context, profile.customBackgroundPath)
            } else null

            Box(
                modifier = Modifier.fillMaxWidth().let { mod ->
                        if (imageAspectRatio != null && imageAspectRatio > 0f) {
                            mod.heightIn(min = maxWidth / imageAspectRatio)
                        } else {
                            mod
                        }
                    }) {
                if (isCustomBg) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(resolveModel(customBgPath))
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    val hasOtherContent =
                        profile.signature.isNotBlank() || profile.avatarPath != null || visibleContacts.isNotEmpty()
                    val hideCover = profile.nickname.isBlank() && !hasOtherContent
                    if (!hideCover) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0x66000000))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(rememberCardBackground(profile))
                    )
                }

                // 头像 + 昵称 + 平台图标：竖屏与横屏共用的主内容区
                val mainContent: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .size(128.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            val hasOtherContent =
                                profile.signature.isNotBlank() || profile.avatarPath != null || visibleContacts.isNotEmpty() || isCustomBg
                            val showPlaceholder = profile.nickname.isBlank() && !hasOtherContent
                            val nicknameText = if (showPlaceholder) "扩列卡片" else profile.nickname

                            if (nicknameText.isNotBlank()) {
                                val nicknameFontSize = remember(nicknameText) {
                                    when {
                                        nicknameText.length <= 3 -> 48.sp
                                        profile.signature.isBlank() && nicknameText.length <= 8 -> 36.sp
                                        nicknameText.length <= 6 -> 36.sp
                                        nicknameText.length <= 8 -> 24.sp
                                        profile.signature.isBlank() || nicknameText.length <= 10 -> 24.sp
                                        else -> 12.sp
                                    }
                                }
                                Text(
                                    text = nicknameText,
                                    fontSize = nicknameFontSize,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = if (profile.signature.isBlank()) 2 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (profile.signature.isNotBlank()) {
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = profile.signature,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor.copy(alpha = 0.9f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.size(20.dp))

                        // 头像
                        val avatarPath = profile.avatarPath
                        if (avatarPath != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(resolveModel(avatarPath))
                                    .build(),
                                contentDescription = "头像",
                                modifier = Modifier.size(96.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(20.dp))

                    // 平台图标行：每行 4 个
                    val rows = visibleContacts.chunked(4)
                    rows.forEachIndexed { rowIndex, row ->
                        if (rowIndex > 0) Spacer(modifier = Modifier.size(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { contact ->
                                val indexInList = visibleContacts.indexOf(contact)
                                ContactIcon(
                                    contact = contact,
                                    textColor = textColor,
                                    selected = indexInList == safeIndex,
                                    onClick = { onSelectedChange(indexInList) })
                            }
                            // 不足 4 个时填充空占位，保持左对齐
                            repeat(4 - row.size) {
                                Spacer(modifier = Modifier.size(56.dp))
                            }
                        }
                    }
                }

                when (layoutMode) {
                    SocialCardLayoutMode.LANDSCAPE_TABLET -> {
                        // 横屏平板：二维码:主内容 = 2:1，卡片高度按屏幕高度比例
                        val cardHeight =
                            (LocalConfiguration.current.screenHeightDp * LANDSCAPE_TABLET_HEIGHT_FRACTION).dp
                        Row(
                            modifier = Modifier
                                .height(cardHeight)
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                mainContent()
                            }

                            Spacer(modifier = Modifier.size(32.dp))

                            // 选中平台的二维码展示区
                            if (selectedContact != null) {
                                SelectedContactQrCode(
                                    contact = selectedContact,
                                    textColor = textColor,
                                    layoutMode = layoutMode,
                                    modifier = Modifier.weight(2f),
                                    onQrClick = { fullscreenContact = selectedContact })
                            }
                        }
                    }

                    SocialCardLayoutMode.LANDSCAPE_PHONE -> {
                        // 横屏手机：二维码固定尺寸在右，主内容占满剩余宽度
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                mainContent()
                            }

                            Spacer(modifier = Modifier.size(32.dp))

                            // 选中平台的二维码展示区
                            if (selectedContact != null) {
                                SelectedContactQrCode(
                                    contact = selectedContact,
                                    textColor = textColor,
                                    layoutMode = layoutMode,
                                    onQrClick = { fullscreenContact = selectedContact })
                            }
                        }
                    }

                    SocialCardLayoutMode.PORTRAIT -> {
                        // 竖屏：自上而下排列
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            mainContent()

                            Spacer(modifier = Modifier.size(32.dp))

                            // 选中平台的二维码展示区
                            if (selectedContact != null) {
                                SelectedContactQrCode(
                                    contact = selectedContact,
                                    textColor = textColor,
                                    layoutMode = layoutMode,
                                    modifier = Modifier.fillMaxWidth(),
                                    onQrClick = { fullscreenContact = selectedContact })
                            }
                        }
                    }
                }
            }
        }
    }

    // 全屏二维码查看
    fullscreenContact?.let { contact ->
        QrCodeFullScreen(
            contactName = contact.displayName.ifBlank { "社交平台" },
            handle = contact.handle,
            qrImagePath = contact.qrCodePath,
            onDismiss = { fullscreenContact = null })
    }
}

/**
 * 单个平台图标。
 *
 * - 选中态：预设 drawable 保持原品牌色（tint = Unspecified）；
 *           自定义上传图标使用原始彩色
 * - 未选中态：预设 drawable 着灰；自定义上传图标做去色处理
 * - 选中态额外加上白边框与浅色背景，便于识别
 */
@Composable
private fun ContactIcon(
    contact: SocialContact, textColor: Color, selected: Boolean, onClick: () -> Unit
) {
    val context = LocalContext.current
    val (customModel, drawableRes) = resolveContactIcon(contact)

    val borderColor = if (selected) Color.White else Color.Transparent
    val borderWidth = if (selected) 2.dp else 0.dp

    Surface(
        modifier = Modifier
            .size(52.dp)
            .border(borderWidth, borderColor, CircleShape),
        shape = CircleShape,
        color = if (selected) Color(0x99FFFFFF) else Color(0x55FFFFFF),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            when {
                // 自定义上传图标：选中彩色，未选中做去色
                customModel != null -> AsyncImage(
                    model = ImageRequest.Builder(context).data(customModel).build(),
                    contentDescription = contact.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (selected) null else DesaturateColorFilter
                )
                // 预设 drawable 图标：选中显示原色，未选中着灰
                drawableRes != 0 -> Icon(
                    painter = painterResource(drawableRes),
                    contentDescription = contact.displayName,
                    tint = if (selected) Color.Unspecified else UnselectedGrayTint,
                    modifier = Modifier.size(32.dp)
                )
                // 首字回退
                else -> Text(
                    text = contact.displayName.firstOrNull()?.toString() ?: "?",
                    color = if (selected) textColor else UnselectedGrayTint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 选中平台的二维码展示区。包含二维码图片 + 平台名称 + handle。
 * 若该平台未上传二维码，显示占位提示。
 *
 * 布局随 [layoutMode] 变化：
 * - 平板横屏：二维码随容器放大（正方形，高度受限），整体填满分配高度；
 * - 其余模式：二维码固定 180dp，居中排列。
 *
 * @param modifier 外层容器修饰符；平板横屏下由调用方传入 Modifier.weight 控制占比。
 * @param layoutMode 当前布局模式，决定二维码尺寸策略。
 * @param onQrClick 点击二维码回调，进入全屏查看。
 */
@Composable
private fun SelectedContactQrCode(
    contact: SocialContact,
    textColor: Color,
    layoutMode: SocialCardLayoutMode,
    modifier: Modifier = Modifier,
    onQrClick: () -> Unit
) {
    val qrModel = resolveModel(contact.qrCodePath)
    val isTabletLandscape = layoutMode == SocialCardLayoutMode.LANDSCAPE_TABLET

    Column(
        modifier = if (isTabletLandscape) modifier.fillMaxHeight() else modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isTabletLandscape) {
            // 平板横屏：二维码随容器放大，正方形并居中于剩余空间
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                val qrSize = minOf(maxHeight, maxWidth)
                QrCodeBox(
                    qrModel = qrModel,
                    contactName = contact.displayName,
                    textColor = textColor,
                    modifier = Modifier.size(qrSize),
                    onQrClick = onQrClick
                )
            }
        } else {
            // 竖屏 / 横屏手机：二维码固定 180dp
            QrCodeBox(
                qrModel = qrModel,
                contactName = contact.displayName,
                textColor = textColor,
                modifier = Modifier.size(180.dp),
                onQrClick = onQrClick
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        ContactCaption(contact = contact, textColor = textColor)
    }
}

/**
 * 二维码图片容器：有图显示二维码（Crop 填充），无图显示占位文字。
 * 尺寸与圆角/背景由 [modifier] 决定，便于在不同布局模式下复用。
 */
@Composable
private fun QrCodeBox(
    qrModel: Any?, contactName: String, textColor: Color, modifier: Modifier, onQrClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .clickable(enabled = qrModel != null, onClick = onQrClick),
        contentAlignment = Alignment.Center
    ) {
        if (qrModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(qrModel).build(),
                contentDescription = "$contactName 二维码",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "无二维码",
                color = textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 选中平台的文字说明：平台名 + handle（若有）。
 */
@Composable
private fun ContactCaption(contact: SocialContact, textColor: Color) {
    Column(
        modifier = Modifier.height(intrinsicSize = IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = contact.displayName.ifBlank { "社交平台" },
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (contact.handle.isNotBlank()) {
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = contact.handle,
                color = textColor.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 全屏二维码查看。
 *
 * 使用 [Dialog] 配合 [DialogProperties.usePlatformDefaultWidth] = false
 * 实现真正的全屏覆盖，避免 Dialog 默认的内边距与宽度限制。
 *
 * - 全黑半透明背景，点击空白处关闭
 * - 二维码以 Fit 模式居中显示，保留完整内容
 * - 顶部展示平台名与账号，方便对方扫码时核对
 *
 * @param contactName 平台名称（标题）
 * @param handle 用户填写的账号/ID
 * @param qrImagePath 二维码本地路径或 Uri 字符串
 * @param onDismiss 关闭回调
 */
@Composable
private fun QrCodeFullScreen(
    contactName: String, handle: String, qrImagePath: String?, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrModel = remember(qrImagePath) { resolveModel(qrImagePath) }

    Dialog(
        onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
                .clickable(onClick = onDismiss), contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .statusBarsPadding()
                    .clickable(enabled = false) {}, // 拦截点击事件不冒泡到外层关闭
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = contactName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                if (handle.isNotBlank()) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = handle,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.size(24.dp))

                if (qrModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(qrModel).build(),
                        contentDescription = "$contactName 二维码",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "无二维码",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.size(24.dp))

                Text(
                    text = "点击空白处关闭",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
