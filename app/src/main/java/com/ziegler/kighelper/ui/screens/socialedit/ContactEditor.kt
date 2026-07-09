package com.ziegler.kighelper.ui.screens.socialedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.SocialPlatformIcons

/**
 * 单个社交平台的联系人编辑器。
 *
 * 显示为一行 Card，内容包含：
 * 1. 顶部行：平台图标（点击换图标） + 平台名称 + 重命名/删除按钮
 * 2. 账号输入框
 * 3. 二维码上传/预览/更换/移除
 *
 * 内部维护两个临时弹窗状态：
 * - [IconPickerSheet]：点击图标时弹出
 * - 重命名对话框：点击重命名按钮时弹出
 *
 * @param contact 当前联系人数据
 * @param qrPreviewModel 二维码预览模型（File/Uri/null）
 * @param iconPreviewModel 平台图标预览模型（File/Uri/null）；为 null 时按 iconKey 渲染预设图标
 * @param onRename 重命名回调
 * @param onIconKeyChange 切换预设图标回调
 * @param onUploadIcon 上传自定义图标回调
 * @param onRemoveCustomIcon 移除自定义图标回调
 * @param onHandleChange 账号变更回调
 * @param onPickQr 选择二维码回调
 * @param onRemoveQr 移除二维码回调
 * @param onDelete 删除该平台回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun ContactEditor(
    contact: SocialContact,
    qrPreviewModel: Any?,
    iconPreviewModel: Any?,
    onRename: (String) -> Unit,
    onIconKeyChange: (String) -> Unit,
    onUploadIcon: () -> Unit,
    onRemoveCustomIcon: () -> Unit,
    onHandleChange: (String) -> Unit,
    onPickQr: () -> Unit,
    onRemoveQr: () -> Unit,
    onDelete: () -> Unit
) {
    var showIconPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember(contact.id) { mutableStateOf(contact.displayName) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 平台图标（点击切换图标）
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { showIconPicker = true }, contentAlignment = Alignment.Center
                ) {
                    when {
                        iconPreviewModel != null -> AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(iconPreviewModel).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        contact.iconKey != SocialPlatformIcons.KEY_DEFAULT -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(SocialPlatformIcons.iconRes(contact.iconKey)),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        else -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.displayName.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    contact.displayName.ifBlank { "(未命名)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "重命名")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = contact.handle,
                onValueChange = onHandleChange,
                label = { Text("账号/ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (qrPreviewModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(qrPreviewModel)
                            .build(),
                        contentDescription = "二维码",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = onPickQr) { Text("更换二维码") }
                    Spacer(Modifier.size(4.dp))
                    TextButton(onClick = onRemoveQr) { Text("移除") }
                } else {
                    OutlinedButton(onClick = onPickQr) {
                        Icon(
                            Icons.Filled.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("上传二维码")
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerSheet(
            currentKey = contact.iconKey,
            hasCustomIcon = iconPreviewModel != null,
            onDismiss = { showIconPicker = false },
            onPick = { key ->
                onIconKeyChange(key)
                showIconPicker = false
            },
            onUploadIcon = {
                showIconPicker = false
                onUploadIcon()
            },
            onRemoveCustomIcon = {
                showIconPicker = false
                onRemoveCustomIcon()
            })
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("修改平台名称") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { Text("平台名称") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameValue.isNotBlank()) onRename(renameValue.trim())
                    showRenameDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            })
    }
}
