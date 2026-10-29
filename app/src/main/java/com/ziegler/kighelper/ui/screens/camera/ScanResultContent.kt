package com.ziegler.kighelper.ui.screens.camera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ziegler.kighelper.data.scan.ScannedContent
import com.ziegler.kighelper.ui.components.SocialCard
import com.ziegler.kighelper.utils.SocialPlatformLauncher

/**
 * 扫码结果正文，根据内容类型选择卡片、平台链接或纯文本布局。
 *
 * [ScanResultDialog] 只负责全屏容器和标题栏，内容类型增加时无需改动外层。
 */
@Composable
internal fun ScanResultContent(
    content: ScannedContent, onDismiss: () -> Unit
) {
    when (content) {
        is ScannedContent.SocialCard -> SocialCardScanContent(content)
        is ScannedContent.Platform -> PlatformScanContent(
            content = content, onDismiss = onDismiss
        )

        is ScannedContent.Text -> PlainTextScanContent(content)
    }
}

@Composable
private fun SocialCardScanContent(content: ScannedContent.SocialCard) {
    val context = LocalContext.current
    Text(
        text = "来源：「娃语」集合码",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SocialCard(
        profile = content.profile, modifier = Modifier.fillMaxWidth()
    )
    TextButton(
        onClick = {
            copyText(context, content.rawUrl)
            Toast.makeText(context, "已复制集合码链接", Toast.LENGTH_SHORT).show()
        }) {
        Text("复制集合码链接")
    }
}

@Composable
private fun PlatformScanContent(
    content: ScannedContent.Platform, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Text(
        text = "识别到 ${content.match.displayName} 内容",
        style = MaterialTheme.typography.titleMedium
    )
    SelectionContainer {
        Text(
            text = content.rawContent, style = MaterialTheme.typography.bodyMedium
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {
            val opened = SocialPlatformLauncher.openScannedContent(
                context, content.rawContent, content.match.iconKey
            )
            if (opened) {
                onDismiss()
            } else {
                Toast.makeText(
                    context, "未找到可打开该内容的平台或浏览器", Toast.LENGTH_SHORT
                ).show()
            }
        }) {
            Text("打开${content.match.displayName}")
        }
        FilledTonalButton(onClick = {
            copyText(context, content.rawContent)
            Toast.makeText(context, "已复制内容", Toast.LENGTH_SHORT).show()
        }) {
            Text("复制")
        }
    }
}

@Composable
private fun PlainTextScanContent(content: ScannedContent.Text) {
    val context = LocalContext.current
    // 已识别平台走 Platform 分支；这里只处理普通网页链接或纯文本。
    val webUrl = remember(content.rawContent) { findWebUrl(content.rawContent) }
    SelectionContainer {
        Text(
            text = content.rawContent, style = MaterialTheme.typography.bodyLarge
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (webUrl != null) {
            Button(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUrl.toUri()))
                } catch (_: Exception) {
                    Toast.makeText(context, "未找到可打开网页的应用", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("访问网页")
            }
        }
        FilledTonalButton(onClick = {
            copyText(context, content.rawContent)
            Toast.makeText(context, "已复制内容", Toast.LENGTH_SHORT).show()
        }) {
            Text("复制内容")
        }
    }
}

/** 从普通文本中提取网页地址，并补全省略的 https 前缀。 */
private fun findWebUrl(text: String): String? {
    val matcher = Patterns.WEB_URL.matcher(text.trim())
    if (!matcher.find()) return null
    val value = matcher.group() ?: return null
    return when {
        value.startsWith("http://", ignoreCase = true) || value.startsWith(
            "https://",
            ignoreCase = true
        ) -> value

        else -> "https://$value"
    }
}

/** 复制原文到剪贴板，供无法直接打开平台的二维码使用。 */
private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("扫码内容", text))
}
