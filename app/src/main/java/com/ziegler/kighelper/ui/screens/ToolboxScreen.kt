package com.ziegler.kighelper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.ui.components.SocialCard

/**
 * 工具箱一级页面。
 *
 * 作为底部导航入口的一级页面，本页面不再持有独立的 TopAppBar，
 * 顶部留白由父级 Scaffold 提供的 [contentPadding] 中的 top inset 决定，
 * 与 MainScreen / InputScreen 保持一致。
 */
@Composable
fun ToolboxScreen(
    contentPadding: PaddingValues,
    socialCardProfile: SocialCardProfile,
    onNavigateToSocialCardEdit: () -> Unit,
    onNavigateToPhraseManager: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val pagePadding = 16.dp

    val outerStartPadding = contentPadding.calculateStartPadding(layoutDirection)
    val outerTopPadding = contentPadding.calculateTopPadding()
    val outerEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val outerBottomPadding = contentPadding.calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // 把整个列表压到状态栏之下，避免向上滚动时内容覆盖到状态栏区域
            .padding(top = outerTopPadding), contentPadding = PaddingValues(
            start = outerStartPadding + pagePadding,
            top = pagePadding,
            end = outerEndPadding + pagePadding,
            bottom = outerBottomPadding + pagePadding
        ), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SocialCard(profile = socialCardProfile, showEditHint = true)
        }

        item {
            OutlinedButton(
                onClick = onNavigateToSocialCardEdit, modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("编辑卡片信息")
            }
        }

        item {
            Text(
                text = "菜单",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        item {
            ToolboxMenuItem(
                title = "管理短语",
                subtitle = "添加、编辑、删除和排序快捷短语",
                icon = Icons.Filled.Edit,
                onClick = onNavigateToPhraseManager
            )
        }

        item {
            ToolboxMenuItem(
                title = "全局音色设置",
                subtitle = "调整引擎、语速、音高和管理预设",
                icon = Icons.Filled.SettingsVoice,
                onClick = onNavigateToVoiceSettings
            )
        }

        item {
            ToolboxMenuItem(
                title = "偏好设置",
                subtitle = "显示字体、主题配色、反馈信息等",
                icon = Icons.Filled.Tune,
                onClick = onNavigateToSettings
            )
        }

        item {
            ToolboxMenuItem(
                title = "关于",
                subtitle = "版本、作者和开源协议",
                icon = Icons.Filled.Info,
                onClick = onNavigateToAbout
            )
        }
    }
}

@Composable
private fun ToolboxMenuItem(
    title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), onClick = onClick, shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
