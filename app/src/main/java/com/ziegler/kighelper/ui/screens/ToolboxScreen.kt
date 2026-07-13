package com.ziegler.kighelper.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.ui.components.SocialCard
import com.ziegler.kighelper.utils.SocialPlatformLauncher

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

    // 选中平台状态上提（state hoisting）：SocialCard 与下方动作按钮共用，
    // 避免选中态被锁在 SocialCard 内部、外部无法获知当前操作的目标平台。
    var selectedIndex by remember { mutableIntStateOf(0) }
    val visibleContacts = socialCardProfile.visibleContacts
    val selectedContact = visibleContacts.getOrNull(
        selectedIndex.coerceIn(0, visibleContacts.lastIndex.coerceAtLeast(0))
    )

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
            SocialCard(
                profile = socialCardProfile,
                showEditHint = true,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { selectedIndex = it })
        }

        item {
            SocialCardActions(
                selectedContact = selectedContact, onEdit = onNavigateToSocialCardEdit
            )
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

/**
 * 社交卡片下方的动作工具条。
 *
 * 左侧两个按钮作用于卡片中当前选中的平台：
 * - 扫码：拉起该平台的扫码 Activity（仅对已知扫码入口的平台启用）
 * - 打开应用：拉起该平台 App 的主界面
 *
 * 右侧为「编辑卡片信息」单图标按钮。平台包名与扫码组件等知识由
 * [SocialPlatformLauncher] 集中维护，本组合函数仅负责调用与失败反馈。
 *
 * @param selectedContact 卡片当前选中的联系方式，null 表示无可操作平台
 * @param onEdit 点击编辑按钮的回调
 */
@Composable
private fun SocialCardActions(
    selectedContact: SocialContact?, onEdit: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 若选中平台，显示撑满空间的胶囊组合按钮
        if (selectedContact != null) {
            val canScan = SocialPlatformLauncher.supportsScan(selectedContact)
            val canLaunchMain = SocialPlatformLauncher.supportsMain(selectedContact)

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 左侧：扫码区域
                    if (canScan) {
                        Row(modifier = Modifier
                            .fillMaxHeight()
                            .clickable(enabled = canLaunchMain) {
                                if (!SocialPlatformLauncher.launchScan(
                                        context, selectedContact
                                    )
                                ) {
                                    Toast.makeText(
                                        context, "无法打开扫码，可能未安装该应用", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "扫码",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "扫码", style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // 分割线
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                )
                        )
                    }

                    // 右侧：打开平台区域（占满胶囊剩余右侧全部空间）
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(enabled = canLaunchMain) {
                                if (!SocialPlatformLauncher.launchMain(context, selectedContact)) {
                                    Toast.makeText(
                                        context, "无法打开应用，可能未安装", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "打开平台应用",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedContact.displayName.ifBlank { "打开应用" },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        } else {
            // 未选中平台时，用一个弹性的 Spacer 将右侧的“编辑”按钮推至最右侧
            Spacer(modifier = Modifier.weight(1f))
        }

        // 编辑按钮
        IconButton(
            onClick = onEdit, modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "编辑卡片信息",
                modifier = Modifier.size(22.dp)
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
