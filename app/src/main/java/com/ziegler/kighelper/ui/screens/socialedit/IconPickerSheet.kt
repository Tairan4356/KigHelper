package com.ziegler.kighelper.ui.screens.socialedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontWeight
import com.ziegler.kighelper.data.SocialPlatformIcons

/**
 * 图标选择底部弹窗。
 *
 * 在联系人编辑器中点击图标时弹出，提供：
 * - 上传自定义图标（调用方负责拉起图片选择器 + 裁剪）
 * - 移除已上传的自定义图标（仅在 [hasCustomIcon] = true 时显示）
 * - 在预设平台图标中切换（[SocialPlatformIcons.selectableKeys]）
 *
 * @param currentKey 当前选中的图标 key，用于高亮
 * @param hasCustomIcon 是否已上传自定义图标，控制「移除自定义」按钮的显隐
 * @param onDismiss 关闭弹窗
 * @param onPick 选择预设图标回调，返回图标 key
 * @param onUploadIcon 上传自定义图标回调
 * @param onRemoveCustomIcon 移除自定义图标回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun IconPickerSheet(
    currentKey: String,
    hasCustomIcon: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onUploadIcon: () -> Unit,
    onRemoveCustomIcon: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "选择图标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 上传自定义图标
                AssistChip(
                    onClick = onUploadIcon, label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("上传图标")
                        }
                    }, colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 若已有自定义图标，提供移除按钮
                if (hasCustomIcon) {
                    AssistChip(
                        onClick = onRemoveCustomIcon, label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("移除自定义")
                            }
                        })
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "预设图标",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SocialPlatformIcons.selectableKeys.forEach { key ->
                    FilterChip(selected = key == currentKey, onClick = { onPick(key) }, label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(SocialPlatformIcons.iconRes(key)),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                when (key) {
                                    SocialPlatformIcons.KEY_QQ -> "QQ"
                                    SocialPlatformIcons.KEY_WECHAT -> "微信"
                                    SocialPlatformIcons.KEY_BILIBILI -> "B站"
                                    SocialPlatformIcons.KEY_DOUYIN -> "抖音"
                                    SocialPlatformIcons.KEY_WEIBO -> "微博"
                                    SocialPlatformIcons.KEY_X -> "X"
                                    SocialPlatformIcons.KEY_FACEBOOK -> "Facebook"
                                    SocialPlatformIcons.KEY_INSTAGRAM -> "Instagram"
                                    SocialPlatformIcons.KEY_TELEGRAM -> "Telegram"
                                    else -> "默认"
                                }
                            )
                        }
                    })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
