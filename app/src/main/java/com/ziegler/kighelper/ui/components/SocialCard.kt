package com.ziegler.kighelper.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ziegler.kighelper.data.CardTemplates
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.SocialPlatformIcons
import java.io.File

/**
 * 把一个字符串路径解析成 Coil 可识别的模型。
 * 既支持落库的本地文件路径，也支持预览时的 Uri 字符串。
 */
private fun resolveModel(path: String?): Any? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("content://") || path.startsWith("file://")) {
        Uri.parse(path)
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
    profile: SocialCardProfile, modifier: Modifier = Modifier, showEditHint: Boolean = false
) {
    val textColor = cardTextColor(profile)
    val context = LocalContext.current

    // 已配置的平台（有账号或二维码都算）
    val visibleContacts = remember(profile.contacts) {
        profile.contacts.filter { it.qrCodePath != null || it.handle.isNotBlank() }
    }

    // 当前选中平台的索引：默认 0，越界时自动夹紧
    var selectedIndex by remember(visibleContacts.size) {
        mutableIntStateOf(0)
    }
    val safeIndex = selectedIndex.coerceIn(0, visibleContacts.lastIndex.coerceAtLeast(0))
    val selectedContact = visibleContacts.getOrNull(safeIndex)

    // 点击二维码进入全屏查看的目标平台
    var fullscreenContact by remember { mutableStateOf<SocialContact?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 背景层：自定义背景图优先；否则渐变背景由 Surface 持有
            val customBgPath = profile.customBackgroundPath
            val isCustomBg =
                profile.templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX && !customBgPath.isNullOrBlank()
            if (isCustomBg) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(resolveModel(customBgPath)).build(),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0x66000000))
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(rememberCardBackground(profile))
                )
            }

            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .size(128.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = profile.nickname.ifBlank { "未设置昵称" },
                            style = MaterialTheme.typography.displayMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center
                        ) {
                            val avatarModel = resolveModel(avatarPath)
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(avatarModel).build(),
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
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
                                onClick = { selectedIndex = indexInList })
                        }
                        // 不足 4 个时填充空占位，保持左对齐
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.size(56.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.size(32.dp))

                // 选中平台的二维码展示区
                if (selectedContact != null) {
                    SelectedContactQrCode(
                        contact = selectedContact,
                        textColor = textColor,
                        onQrClick = { fullscreenContact = selectedContact })
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
                    contentScale = ContentScale.Crop,
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
 * 选中平台的二维码展示区。包含平台名称 + handle + 二维码图片。
 * 若该平台未上传二维码，显示占位提示。
 *
 * 点击二维码可触发 [onQrClick] 进入全屏查看。
 */
@Composable
private fun SelectedContactQrCode(
    contact: SocialContact, textColor: Color, onQrClick: () -> Unit
) {
    val context = LocalContext.current
    val qrModel = resolveModel(contact.qrCodePath)

    Column(
        modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 二维码图片或占位
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x33FFFFFF))
                .clickable(enabled = qrModel != null, onClick = onQrClick),
            contentAlignment = Alignment.Center
        ) {
            if (qrModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(qrModel).build(),
                    contentDescription = "${contact.displayName} 二维码",
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

        Spacer(modifier = Modifier.size(16.dp))

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
