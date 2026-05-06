package com.ziegler.kighelper.ui.components

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.ziegler.kighelper.utils.UpdateConfig
import com.ziegler.kighelper.utils.UpdateManager

@Composable
fun UpdateHandler() {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateConfig?>(null) }

    // 仅在 App 启动时执行一次
    LaunchedEffect(Unit) {
        updateInfo = UpdateManager.checkUpdate(context)
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("发现新版本 v${info.versionName}") },
            text = { Text(info.updateContent) },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, info.downloadUrl.toUri())
                    context.startActivity(intent)
                    updateInfo = null
                }) {
                    Text("更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) {
                    Text("暂不")
                }
            })
    }
}