package com.ziegler.kighelper.ui.screens.socialedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ziegler.kighelper.data.SocialPlatformIcons

/**
 * 添加平台对话框。
 *
 * 用户输入平台名称并从预设图标中选择一个，确认后通过 [onConfirm] 回传。
 *
 * - 平台名称非空才允许添加
 * - 图标默认选择 [SocialPlatformIcons.KEY_DEFAULT]，用户可改为其他预设
 * - 用户可后续在联系人编辑器中再次更换图标或上传自定义图标
 *
 * @param onDismiss 关闭对话框
 * @param onConfirm 确认回调，参数为 (平台名称, 图标 key)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun AddContactDialog(
    onDismiss: () -> Unit, onConfirm: (name: String, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf(SocialPlatformIcons.KEY_DEFAULT) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("添加平台") }, text = {
        Column {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("平台名称") },
                placeholder = { Text("如：微博、小红书") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("选择图标", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SocialPlatformIcons.selectableKeys.forEach { key ->
                    AssistChip(
                        onClick = { selectedIconKey = key }, label = {
                        Icon(
                            painter = painterResource(SocialPlatformIcons.iconRes(key)),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }, colors = if (selectedIconKey == key) AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) else AssistChipDefaults.assistChipColors()
                    )
                }
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), selectedIconKey)
            }) { Text("添加") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("取消") }
    })
}
