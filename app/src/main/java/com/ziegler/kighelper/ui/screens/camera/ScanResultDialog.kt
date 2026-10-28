package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ziegler.kighelper.data.scan.ScannedContent

/**
 * 全屏扫码结果页。
 *
 * 相机页与时光轴扫码记录共用该组件；[dismissLabel] 由调用方决定是“继续扫码”
 * 还是普通“关闭”，具体内容布局由 [ScanResultContent] 负责。
 */
@Composable
fun ScanResultDialog(
    content: ScannedContent,
    onDismiss: () -> Unit,
    dismissLabel: String = "关闭"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                ScanResultHeader(
                    content = content,
                    dismissLabel = dismissLabel,
                    onDismiss = onDismiss
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ScanResultContent(content = content, onDismiss = onDismiss)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissLabel)
                    }
                }
            }
        }
    }
}

/** 标题根据内容类型显示集合码、平台名称或普通扫码文本。 */
@Composable
private fun ScanResultHeader(
    content: ScannedContent,
    dismissLabel: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (content) {
                is ScannedContent.SocialCard -> "扩列卡片"
                is ScannedContent.Platform -> content.match.displayName
                is ScannedContent.Text -> "扫码内容"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = dismissLabel)
        }
    }
}
